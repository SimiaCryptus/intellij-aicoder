package aicoder.actions.agent

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.AddApplyFileDiffLinks

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.ImageModels
import com.simiacryptus.jopenai.models.OpenAIModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.*
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import kotlin.io.path.name

val VirtualFile.toFile: File get() = File(this.path)

class WebDevelopmentAssistantAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    val file = UITools.getSelectedFolder(event) ?: return false
    return file.isDirectory
  }


  override fun handle(e: AnActionEvent) {
    try {
      val project = e.project ?: return
      val session = Session.newGlobalID()
      val selectedFile = UITools.getSelectedFolder(e) ?: return
      DataStorage.sessionPaths[session] = selectedFile.toFile
      SessionProxyServer.metadataStorage.setSessionName(
        null,
        session,
        "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
      )
      SessionProxyServer.chats[session] = WebDevApp(root = selectedFile)
      ApplicationServer.appInfoMap[session] = AppInfoData(
        applicationName = "Code Chat",
        singleInput = true,
        stickyInput = false,
        loadImages = false,
        showMenubar = false
      )
      val server = AppServer.getServer(project)

      UITools.runAsync(e.project, "Opening Web Development Assistant", true) { progress ->
        progress.text = "Launching browser..."
        Thread.sleep(500)

        val uri = server.server.uri.resolve("/#$session")
        BaseAction.log.info("Opening browser to $uri")
        browse(uri)
      }
    } catch (e: Throwable) {
      UITools.error(log, "Error launching Web Development Assistant", e)
    }
  }


  open class WebDevApp(
    applicationName: String = "Web Development Agent",
    val temperature: Double = 0.1,
    root: VirtualFile?,
    override val singleInput: Boolean = false,
  ) : ApplicationServer(
    applicationName = applicationName,
    path = "/webdev",
    showMenubar = false,
    root = root?.toFile!!,
  ) {
    private val log = LoggerFactory.getLogger(WebDevApp::class.java)

    companion object {
      private const val DEFAULT_BUDGET = 2.00
    }

    override fun userMessage(
      session: Session,
      user: User?,
      userMessage: String,
      ui: ApplicationInterface,
      api: API
    ) {
      try {
        val settings = getSettings(session, user) ?: Settings()
        if (api is ChatClient) {
          api.budget = settings.budget ?: DEFAULT_BUDGET
        }
        WebDevAgent(
          api = api,
          api2 = IdeaOpenAIClient.instance,
          session = session,
          user = user,
          ui = ui,
          model = settings.model,
          parsingModel = settings.parsingModel,
          tools = settings.tools,
          root = root,
        ).start(userMessage = userMessage)
      } catch (e: Throwable) {
        log.error("Error processing user message", e)
        throw e
      }
    }

    data class Settings(
      val budget: Double? = 2.00,
      val tools: List<String> = emptyList(),
      val model: ChatModel = OpenAIModels.GPT4o,
      val parsingModel: ChatModel = OpenAIModels.GPT4oMini,
    )

    override val settingsClass: Class<*> get() = Settings::class.java

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T? = Settings() as T
  }

  class WebDevAgent(
    val api: API,
    val api2: OpenAIClient,
    val session: Session,
    val user: User?,
    val ui: ApplicationInterface,
    val model: ChatModel,
    val parsingModel: ChatModel,
    val tools: List<String> = emptyList(),
    val root: File,
  ) {
    val actors = mapOf(
      ActorTypes.ArchitectureDiscussionActor to ParsedActor(
        resultClass = ProjectSpec::class.java,
        prompt = """
                  Translate the user's idea into a detailed architecture for a simple web application. 
                  
                  List all html, css, javascript, and image files to be created, and for each file:
                  1. Mark with <file>filename</file> tags.
                  2. Describe the public interface / interaction with other components.
                  3. Core functional requirements.
                  
                  Specify user interactions and how the application will respond to them.
                  Identify key HTML classes and element IDs that will be used to bind the application to the HTML.
                  """.trimIndent(),
        model = model,
        parsingModel = parsingModel,
      ),
      ActorTypes.CodeReviewer to SimpleActor(
          prompt = """
                  Analyze the code summarized in the user's header-labeled code blocks.
                  Review, look for bugs, and provide fixes. 
                  Provide implementations for missing functions.
                  
                """.trimIndent() + patchFormatPrompt,
          model = model,
      ),
      ActorTypes.HtmlCodingActor to SimpleActor(
        prompt = """
          You will translate the user request into a skeleton HTML file for a rich javascript application.
          The html file can reference needed CSS and JS files, which are will be located in the same directory as the html file.
          Do not output the content of the resource files, only the html file.
        """.trimIndent(), model = model
      ),
      ActorTypes.JavascriptCodingActor to SimpleActor(
        prompt = """
          You will translate the user request into a javascript file for use in a rich javascript application.
        """.trimIndent(), model = model
      ),
      ActorTypes.CssCodingActor to SimpleActor(
        prompt = """
          You will translate the user request into a CSS file for use in a rich javascript application.
        """.trimIndent(), model = model
      ),
      ActorTypes.EtcCodingActor to SimpleActor(
        prompt = """
              You will translate the user request into a file for use in a web application.
            """.trimIndent(),
        model = model,
      ),
      ActorTypes.ImageActor to ImageActor(
        prompt = """
              You will translate the user request into an image file for use in a web application.
            """.trimIndent(),
        textModel = model,
        imageModel = ImageModels.DallE3,
      ).apply {
        setImageAPI(api2)
      },
    ).map { it.key.name to it.value }.toMap()

    enum class ActorTypes {
      HtmlCodingActor,
      JavascriptCodingActor,
      CssCodingActor,
      ArchitectureDiscussionActor,
      CodeReviewer,
      EtcCodingActor,
      ImageActor,
    }
    
    private val architectureDiscussionActor by lazy { actors.get(ActorTypes.ArchitectureDiscussionActor.name)!! as ParsedActor<ProjectSpec> }
    private val htmlActor by lazy { actors.get(ActorTypes.HtmlCodingActor.name)!! as SimpleActor }
    private val imageActor by lazy { actors.get(ActorTypes.ImageActor.name)!! as ImageActor }
    private val javascriptActor by lazy { actors.get(ActorTypes.JavascriptCodingActor.name)!! as SimpleActor }
    private val cssActor by lazy { actors.get(ActorTypes.CssCodingActor.name)!! as SimpleActor }
    private val codeReviewer by lazy { actors.get(ActorTypes.CodeReviewer.name)!! as SimpleActor }
    private val etcActor by lazy { actors.get(ActorTypes.EtcCodingActor.name)!! as SimpleActor }

    private val codeFiles = mutableSetOf<Path>()

    fun start(
      userMessage: String,
    ) {
      val task = ui.newTask()
      val toInput = { it: String -> listOf(it) }
      val architectureResponse = Discussable(
        task = task,
        userMessage = { userMessage },
        initialResponse = { it: String -> architectureDiscussionActor.answer(toInput(it), api = api) },
        outputFn = { design: ParsedResponse<ProjectSpec> ->
          //          renderMarkdown("${design.text}\n\n```json\n${JsonUtil.toJson(design.obj)/*.indent("  ")*/}\n```")
          AgentPatterns.displayMapInTabs(
            mapOf(
              "Text" to renderMarkdown(design.text, ui = ui),
              "JSON" to renderMarkdown(
                "```json\n${JsonUtil.toJson(design.obj)/*.indent("  ")*/}\n```",
                ui = ui
              ),
            )
          )
        },
        ui = ui,
        reviseResponse = { userMessages: List<Pair<String, Role>> ->
          architectureDiscussionActor.respond(
            messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
              .toTypedArray<ApiModel.ChatMessage>()),
            input = toInput(userMessage),
            api = api
          )
        },
        atomicRef = AtomicReference(),
        semaphore = Semaphore(0),
        heading = renderMarkdown(userMessage)
      ).call()


      try {
//                val toolSpecs = tools.map { ToolServlet.tools.find { t -> t.path == it } }
//                    .joinToString("\n\n") { it?.let { JsonUtil.toJson(it.openApiDescription) } ?: "" }
        var messageWithTools = userMessage
//                if (toolSpecs.isNotBlank()) messageWithTools += "\n\nThese services are available:\n$toolSpecs"
        task.echo(
          renderMarkdown(
            "```json\n${JsonUtil.toJson(architectureResponse.obj)/*.indent("  ")*/}\n```",
            ui = ui
          )
        )
        val fileTabs = TabbedDisplay(task)
        architectureResponse.obj.files.filter {
          !it.name!!.startsWith("http")
        }.map { (path, description) ->
          val task = ui.newTask(false).apply { fileTabs[path.toString()] = placeholder }
          task.header("Drafting $path", 1)
          codeFiles.add(File(path).toPath())
          ApplicationServices.clientManager.getPool(session, user).submit {
            when (path!!.split(".").last().lowercase()) {

              "js" -> draftResourceCode(
                task = task,
                request = javascriptActor.chatMessages(
                  listOf(
                    messageWithTools,
                    architectureResponse.text,
                    "Render $path - $description"
                  )
                ),
                actor = javascriptActor,
                path = File(path).toPath(), "js", "javascript"
              )


              "css" -> draftResourceCode(
                task = task,
                request = cssActor.chatMessages(
                  listOf(
                    messageWithTools,
                    architectureResponse.text,
                    "Render $path - $description"
                  )
                ),
                actor = cssActor,
                path = File(path).toPath()
              )

              "html" -> draftResourceCode(
                task = task,
                request = htmlActor.chatMessages(
                  listOf(
                    messageWithTools,
                    architectureResponse.text,
                    "Render $path - $description"
                  )
                ),
                actor = htmlActor,
                path = File(path).toPath()
              )

              "png" -> draftImage(
                task = task,
                request = etcActor.chatMessages(
                  listOf(
                    messageWithTools,
                    architectureResponse.text,
                    "Render $path - $description"
                  )
                ),
                actor = imageActor,
                path = File(path).toPath()
              )

              "jpg" -> draftImage(
                task = task,
                request = etcActor.chatMessages(
                  listOf(
                    messageWithTools,
                    architectureResponse.text,
                    "Render $path - $description"
                  )
                ),
                actor = imageActor,
                path = File(path).toPath()
              )

              else -> draftResourceCode(
                task = task,
                request = etcActor.chatMessages(
                  listOf(
                    messageWithTools,
                    architectureResponse.text,
                    "Render $path - $description"
                  )
                ),
                actor = etcActor,
                path = File(path).toPath()
              )

            }
          }
        }.toTypedArray().forEach { it.get() }
        // Apply codeReviewer

        iterateCode(task)
      } catch (e: Throwable) {
        log.warn("Error", e)
        task.error(ui, e)
      }
    }

    fun codeSummary() = codeFiles.filter {
      if (it.name.lowercase().endsWith(".png")) return@filter false
      if (it.name.lowercase().endsWith(".jpg")) return@filter false
      true
    }.joinToString("\n\n") { path ->
      "# $path\n```${path.toString().split('.').last()}\n${root.resolve(path.toFile()).readText()}\n```"
    }

    private fun iterateCode(
      task: SessionTask
    ) {
      Discussable(
        task = task,
        heading = "Code Refinement",
        userMessage = { codeSummary() },
        initialResponse = {
          codeReviewer.answer(listOf(it), api = api)
        },
        outputFn = { code ->
          renderMarkdown(
            AddApplyFileDiffLinks.instrumentFileDiffs(
              ui.socketManager!!,
              root = root.toPath(),
              response = code,
              handle = { newCodeMap ->
                newCodeMap.forEach { (path, newCode) ->
                  task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                }
              },
              ui = ui,
              api = api
            )
          )
        },
        ui = ui,
        reviseResponse = { userMessages ->
          val userMessages = userMessages.toMutableList()
          userMessages.set(0, userMessages.get(0).copy(first = codeSummary()))
          val combinedMessages =
            userMessages.map { ApiModel.ChatMessage(Role.user, it.first.toContentList()) }
          codeReviewer.respond(
            input = listOf(element = combinedMessages.joinToString("\n")),
            api = api,
            messages = combinedMessages.toTypedArray(),
          )
        },
      ).call()
    }

    private fun draftImage(
      task: SessionTask,
      request: Array<ApiModel.ChatMessage>,
      actor: ImageActor,
      path: Path,
    ) {
      try {
        var code = Discussable(
          task = task,
          userMessage = { "" },
          heading = "Drafting $path",
          initialResponse = {
            val messages = (request + ApiModel.ChatMessage(Role.user, "Draft $path".toContentList()))
              .toList().toTypedArray()
            actor.respond(
              listOf(request.joinToString("\n") { it.content?.joinToString() ?: "" }),
              api,
              *messages
            )

          },
          outputFn = { img ->
            renderMarkdown(
              "<img src='${
                task.saveFile(
                  path.toString(),
                  write(img, path)
                )
              }' style='max-width: 100%;'/>", ui = ui
            )
          },
          ui = ui,
          reviseResponse = { userMessages: List<Pair<String, Role>> ->
            actor.respond(
              messages = (request.toList() + userMessages.map {
                ApiModel.ChatMessage(
                  it.second,
                  it.first.toContentList()
                )
              })
                .toTypedArray<ApiModel.ChatMessage>(),
              input = listOf(
                element = (request.toList() + userMessages.map {
                  ApiModel.ChatMessage(
                    it.second,
                    it.first.toContentList()
                  )
                })
                  .joinToString("\n") { it.content?.joinToString() ?: "" }),
              api = api,
            )
          },
        ).call()
        task.complete(
          renderMarkdown(
            "<img src='${
              task.saveFile(
                path.toString(),
                write(code, path)
              )
            }' style='max-width: 100%;'/>", ui = ui
          )
        )
      } catch (e: Throwable) {
        val error = task.error(ui, e)
        task.complete(ui.hrefLink("♻", "href-link regen-button") {
          error?.clear()
          draftImage(task, request, actor, path)
        })
      }
    }

    private fun write(
      code: ImageResponse,
      path: Path
    ): ByteArray {
      val byteArrayOutputStream = ByteArrayOutputStream()
      ImageIO.write(
        code.image,
        path.toString().split(".").last(),
        byteArrayOutputStream
      )
      val bytes = byteArrayOutputStream.toByteArray()
      return bytes
    }

    private fun draftResourceCode(
      task: SessionTask,
      request: Array<ApiModel.ChatMessage>,
      actor: SimpleActor,
      path: Path,
      vararg languages: String = arrayOf(path.toString().split(".").last().lowercase()),
    ) {
      try {
        var code = Discussable(
          task = task,
          userMessage = { "Drafting $path" },
          heading = "",
          initialResponse = {
            actor.respond(
              listOf(request.joinToString("\n") { it.content?.joinToString() ?: "" }),
              api,
              *(request + ApiModel.ChatMessage(Role.user, "Draft $path".toContentList()))
                .toList().toTypedArray()
            )
          },
          outputFn = { design: String ->
            var design = design
            languages.forEach { language ->
              if (design.contains("```$language")) {
                design = design.substringAfter("```$language").substringBefore("```")
              }
            }
            renderMarkdown("```${languages.first()}\n${design.let { it }}\n```", ui = ui)
          },
          ui = ui,
          reviseResponse = { userMessages: List<Pair<String, Role>> ->
            actor.respond(
              messages = (request.toList() + userMessages.map {
                ApiModel.ChatMessage(
                  it.second,
                  it.first.toContentList()
                )
              })
                .toTypedArray<ApiModel.ChatMessage>(),
              input = listOf(
                element = (request.toList() + userMessages.map {
                  ApiModel.ChatMessage(
                    it.second,
                    it.first.toContentList()
                  )
                })
                  .joinToString("\n") { it.content?.joinToString() ?: "" }),
              api = api,
            )
          },
        ).call()
        code = extractCode(code)
        task.complete(
          "<a href='${
            task.saveFile(
              path.toString(),
              code.toByteArray(Charsets.UTF_8)
            )
          }'>$path</a> Updated"
        )
      } catch (e: Throwable) {
        val error = task.error(ui, e)
        task.complete(ui.hrefLink("♻", "href-link regen-button") {
          error?.clear()
          draftResourceCode(task, request, actor, path, *languages)
        })
      }
    }

    private fun extractCode(code: String): String {
      var code = code
      code = code.trim()
      "(?s)```[^\\n]*\n(.*)\n```".toRegex().find(code)?.let {
        code = it.groupValues[1]
      }
      return code
    }

  }

  companion object {
    private val log = LoggerFactory.getLogger(WebDevelopmentAssistantAction::class.java)
    val root: File get() = File(AppSettingsState.instance.pluginHome, "code_chat")

    data class ProjectSpec(
      @Description("Files in the project design, including all local html, css, and js files.")
      val files: List<ProjectFile> = emptyList()
    ) : ValidatedObject {
      override fun validate(): String? = when {
        files.isEmpty() -> "Resources are required"
        files.any { it.validate() != null } -> "Invalid resource"
        else -> null
      }
    }

    data class ProjectFile(
      @Description("The path to the file, relative to the project root.")
      val name: String? = "",
      @Description("A brief description of the file's purpose and contents.")
      val description: String? = ""
    ) : ValidatedObject {
      override fun validate(): String? = when {
        name.isNullOrBlank() -> "Path is required"
        name.contains(" ") -> "Path cannot contain spaces"
        !name.contains(".") -> "Path must contain a file extension"
        else -> null
      }
    }

  }

}