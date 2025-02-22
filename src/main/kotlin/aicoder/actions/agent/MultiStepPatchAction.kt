package aicoder.actions.agent

import ai.grazie.utils.mpp.UUID
import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.AddApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.core.util.commonRoot
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.getChildClient
import com.simiacryptus.util.JsonUtil.toJson
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class MultiStepPatchAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  val path = "/autodev"
  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    UITools.getSelectedFile(event) ?: return false
    return true
  }


  override fun handle(e: AnActionEvent) {
    val project = e.project ?: return
    UITools.runAsync(project, "Initializing Auto Dev Assistant", true) { progress ->
      progress.isIndeterminate = true
      try {
        val session = Session.newGlobalID()
        val storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome) as DataStorage?
        val selectedFile = UITools.getSelectedFolder(e)
        if (null != storage && null != selectedFile) {
          DataStorage.sessionPaths[session] = selectedFile.toFile
        }
        SessionProxyServer.metadataStorage.setSessionName(
          null,
          session,
          "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
        )
        SessionProxyServer.chats[session] = AutoDevApp(event = e)
        ApplicationServer.appInfoMap[session] = AppInfoData(
          applicationName = "Code Chat",
          singleInput = true,
          stickyInput = false,
          loadImages = false,
          showMenubar = false
        )
        val server = AppServer.getServer(e.project)


        ApplicationManager.getApplication().invokeLater {
          progress.text = "Opening browser..."
          val uri = server.server.uri.resolve("/#$session")
          BaseAction.log.info("Opening browser to $uri")
          browse(uri)
        }
      } catch (e: Throwable) {
        UITools.error(log, "Failed to initialize Auto Dev Assistant", e)
      }
    }
  }

  open class AutoDevApp(
    applicationName: String = "Auto Dev Assistant v1.2",
    val temperature: Double = 0.1,
    val event: AnActionEvent,
  ) : ApplicationServer(
    applicationName = applicationName,
    path = "/autodev",
    showMenubar = false,
  ) {
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
      val settings = getSettings(session, user) ?: Settings(
        budget = DEFAULT_BUDGET,
        model = AppSettingsState.instance.smartModel.chatModel()
      )
      if (api is ChatClient) api.budget = settings.budget ?: DEFAULT_BUDGET
      AutoDevAgent(
        api = api,
        session = session,
        user = user,
        ui = ui,
        model = settings.model!!,
        parsingModel = AppSettingsState.instance.fastModel.chatModel(),
        event = event,
      ).start(
        userMessage = userMessage,
      )
    }

    data class Settings(
      val budget: Double? = 2.00,
      val tools: List<String> = emptyList(),
      val model: ChatModel? = AppSettingsState.instance.smartModel.chatModel(),
    )

    override val settingsClass: Class<*> get() = Settings::class.java

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T? = Settings() as T
  }

  class AutoDevAgent(
    val api: API,
    val session: Session,
    val user: User?,
    val ui: ApplicationInterface,
    val model: ChatModel,
    val parsingModel: ChatModel,
    val event: AnActionEvent,
  ) {
    val actors = mapOf(
      ActorTypes.DesignActor to ParsedActor(
        resultClass = TaskList::class.java,
        prompt = """
          Translate the user directive into an action plan for the project.
          Break the user's request into a list of simple tasks to be performed.
          For each task, provide a list of files to be modified and a description of the changes to be made.
        """.trimIndent(),
        model = model,
        parsingModel = parsingModel,
      ),
      ActorTypes.TaskCodingActor to SimpleActor(
          prompt = "Implement the changes to the codebase as described in the task list.\n\n" + patchFormatPrompt,
          model = model
      ),
    ).map { it.key.name to it.value }.toMap()

    enum class ActorTypes {
      DesignActor,
      TaskCodingActor,
    }

    private val designActor by lazy { actors.get(ActorTypes.DesignActor.name)!! as ParsedActor<TaskList> }
    private val taskActor by lazy { actors.get(ActorTypes.TaskCodingActor.name)!! as SimpleActor }

    fun start(
      userMessage: String,
    ) {
      val codeFiles = mutableSetOf<Path>()
      val root = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)
        ?.map { it.toFile.toPath() }?.toTypedArray()?.commonRoot()!!
      PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)?.forEach { file ->
        //
        codeFiles.add(root.relativize(file.toNioPath()))
      }
      require(codeFiles.isNotEmpty()) { "No files selected" }
      fun codeSummary() = codeFiles.joinToString("\n\n") { path ->
        "# $path\n```${
          path.toString().split('.').last()
        }\n${root.resolve(path).toFile().readText()}\n```"
      }

      val task = ui.newTask()
      val api = (api as ChatClient).getChildClient(task)

      val toInput = { it: String -> listOf(codeSummary(), it) }
      val architectureResponse = Discussable(
        task = task,
        userMessage = { userMessage },
        heading = renderMarkdown(userMessage),
        initialResponse = { it: String -> designActor.answer(toInput(it), api = api) },
        outputFn = { design: ParsedResponse<TaskList> ->
          //          renderMarkdown("${design.text}\n\n```json\n${JsonUtil.toJson(design.obj)/*.indent("  ")*/}\n```")
          AgentPatterns.displayMapInTabs(
            mapOf(
              "Text" to renderMarkdown(design.text, ui = ui),
              "JSON" to renderMarkdown("```json\n${toJson(design.obj)/*.indent("  ")*/}\n```", ui = ui),
            )
          )
        },
        ui = ui,
        reviseResponse = { userMessages: List<Pair<String, Role>> ->
          designActor.respond(
            messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
              .toTypedArray<ApiModel.ChatMessage>()),
            input = toInput(userMessage),
            api = api
          )
        },
        atomicRef = AtomicReference(),
        semaphore = Semaphore(0),
      ).call()

      try {
        val taskTabs = TabbedDisplay(task)
        architectureResponse.obj.tasks.map { (paths, description) ->
          var description = (description ?: UUID.random().toString()).trim()
          // Strip `#` from the beginning of the description
          while (description.startsWith("#")) {
            description = description.substring(1)
          }
          description = renderMarkdown(description, ui = ui, tabs = false)
          val task = ui.newTask(false).apply { taskTabs[description] = placeholder }
          ApplicationServices.clientManager.getPool(session, user).submit {
            task.header("Task: $description", 2)
            Retryable(ui, task) {
              try {
                val filter = codeFiles.filter { path ->
                  paths?.find { path.toString().contains(it) }?.isNotEmpty() == true
                }
                require(filter.isNotEmpty()) {
                  """
                  No files found for """.trimIndent() + paths + """
                  
                  Root:
                  """.trimIndent() + root + """
                  
                  Files:
                  """.trimIndent() + codeFiles.joinToString("\n") + """
                  
                  Paths:
                  """.trimIndent() + (paths?.joinToString("\n") ?: "")
                }
                renderMarkdown(
                  AddApplyFileDiffLinks.instrumentFileDiffs(
                    ui.socketManager!!,
                    root = root,
                    response = taskActor.answer(
                      listOf(
                        codeSummary(),
                        userMessage,
                        filter.joinToString("\n\n") {
                          "# ${it}\n```${
                            it.toString().split('.').last().let { /*escapeHtml4*/it/*.indent("  ")*/ }
                          }\n${root.resolve(it).toFile().readText()}\n```"
                        },
                        architectureResponse.text,
                        "Provide a change for ${paths?.joinToString(",") { it } ?: ""} ($description)"
                      ), api),
                    handle = { newCodeMap ->
                      newCodeMap.forEach { (path, newCode) ->
                        task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                      }
                    },
                    ui = ui,
                    api = api
                  )
                )
              } catch (e: Exception) {
                task.error(ui, e)
                ""
              }
            }
          }
        }.toTypedArray().forEach { it.get() }
      } catch (e: Exception) {
        log.warn("Error", e)
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(MultiStepPatchAction::class.java)
    val root: File get() = File(AppSettingsState.instance.pluginHome, "code_chat")

    data class TaskList(
      @Description("List of tasks to be performed in this project")
      val tasks: List<Task> = emptyList()
    ) : ValidatedObject {
      override fun validate(): String? = when {
        tasks.isEmpty() -> "Resources are required"
        tasks.any { it.validate() != null } -> "Invalid resource"
        else -> null
      }
    }

    data class Task(
      @Description("List of paths involved in the task. This should include all files to be modified, and can include other files whose content will be informative in writing the changes.")
      val paths: List<String>? = null,
      @Description("Detailed description of the changes to be made. Markdown format is supported.")
      val description: String? = null
    ) : ValidatedObject {
      override fun validate(): String? = when {
        paths.isNullOrEmpty() -> "Paths are required"
        paths.any { it.isBlank() } -> "Invalid path"
        else -> null
      }
    }

  }
}