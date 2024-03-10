package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.ApplicationEvents
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.dev.AppServer
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.addApplyDiffLinks
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.core.actors.ActorSystem
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File

class AutoDevAction : BaseAction() {

  val path = "/autodev"

  override fun handle(e: AnActionEvent) {
    val session = StorageInterface.newGlobalID()
    val storage = ApplicationServices.dataStorageFactory(DiffChatAction.root) as DataStorage?
    val selectedFile = UITools.getSelectedFolder(e)
    if (null != storage && null != selectedFile) {
      DataStorage.sessionPaths[session] = selectedFile.toFile
    }
    agents[session] = AutoDevApp(event = e)
    val server = AppServer.getServer(e.project)
    val app = initApp(server, path)
    app.sessions[session] = app.newSession(null, session)
    Thread {
      Thread.sleep(500)
      try {
        Desktop.getDesktop().browse(server.server.uri.resolve("$path/#$session"))
      } catch (e: Throwable) {
        log.warn("Error opening browser", e)
      }
    }.start()
  }

  open class AutoDevApp(
    applicationName: String = "Auto Dev Assistant v1.1",
    open val symbols: Map<String, Any> = mapOf(),
    val temperature: Double = 0.1,
    val event: AnActionEvent,
  ) : ApplicationServer(
    applicationName = applicationName,
    path = "/autodev",
  ) {
    override fun userMessage(
      session: Session,
      user: User?,
      userMessage: String,
      ui: ApplicationInterface,
      api: API
    ) {
      val settings = getSettings(session, user) ?: Settings()
      if (api is ClientManager.MonitoredClient) api.budget = settings.budget ?: 2.00
      AutoDevAgent(
        api = api,
        dataStorage = dataStorage,
        session = session,
        user = user,
        ui = ui,
        tools = settings.tools,
        model = settings.model,
        event = event,
      ).start(
        userMessage = userMessage,
      )
    }

    data class Settings(
      val budget: Double? = 2.00,
      val tools: List<String> = emptyList(),
      val model: ChatModels = ChatModels.GPT4Turbo,
    )

    override val settingsClass: Class<*> get() = Settings::class.java

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T? = Settings() as T
  }

  class AutoDevAgent(
    val api: API,
    dataStorage: StorageInterface,
    session: Session,
    user: User?,
    val ui: ApplicationInterface,
    val model: ChatModels,
    val tools: List<String> = emptyList(),
    val actorMap: Map<ActorTypes, BaseActor<*, *>> = mapOf(
      ActorTypes.DesignActor to ParsedActor(
        parserClass = TaskListParser::class.java,
        prompt = """
            Translate the user directive into an action plan for the project.
            Break the user's request into a list of simple tasks to be performed.
            For each task, provide a list of files to be modified and a description of the changes to be made.
          """.trimIndent(),
        model = model,
        parsingModel = model,
      ),
      ActorTypes.TaskCodingActor to SimpleActor(
        prompt = """
          Implement the changes to the codebase as described in the task list.
            
          Response should use one or more code patches in diff format within ```diff code blocks.
          Each diff should be preceded by a header that identifies the file being modified.
          The diff format should use + for line additions, - for line deletions.
          The diff should include 2 lines of context before and after every change.
          
          Example:
          
          Explanation text
          
          ### scripts/filename.js
          ```diff
          - const b = 2;
          + const a = 1;
          ```
          
          Continued text
          """.trimIndent(),
        model = model
      ),
    ),
    val event: AnActionEvent,
  ) : ActorSystem<AutoDevAgent.ActorTypes>(actorMap, dataStorage, user, session) {
    enum class ActorTypes {
      DesignActor,
      TaskCodingActor,
    }

    val designActor by lazy { getActor(ActorTypes.DesignActor) as ParsedActor<TaskList> }
    val taskActor by lazy { getActor(ActorTypes.TaskCodingActor) as SimpleActor }

    fun start(
      userMessage: String,
    ) {
      val dataContext = event.dataContext
      val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
      val languages =
        virtualFiles?.associate { it to ComputerLanguage.findByExtension(it.extension ?: "")?.name } ?: mapOf()
      val virtualFileMap = virtualFiles?.associate { it.toNioPath() to it } ?: mapOf()
      val codeFiles = mutableMapOf<String, String>()
      val root = virtualFiles?.map { file ->
        file.toNioPath()
      }?.toTypedArray()?.commonRoot()!!
      val paths = virtualFiles.associate { file ->
        val relative = root.relativize(file.toNioPath())
        val path = relative.toString()
        val language = languages[file] ?: "plaintext"
        val code = file.contentsToByteArray().toString(Charsets.UTF_8)
        codeFiles[path] = code
        path to language
      }

      fun codeSummary() = codeFiles.entries.joinToString("\n\n") { (path, code) ->
        "# $path\n```${
          path.split('.').last()
        }\n$code\n```"
      }


      val architectureResponse = AgentPatterns.iterate(
        input = userMessage,
        heading = userMessage,
        actor = designActor,
        toInput = { listOf(codeSummary(), it) },
        api = api,
        ui = ui,
        outputFn = { task, design ->
          task.add(renderMarkdown("${design.text}\n\n```json\n${JsonUtil.toJson(design.obj)}\n```"))
        }
      )


      val task = ui.newTask()
      try {
        architectureResponse.obj.tasks.forEach { (paths, description) ->
          task.complete(ui.hrefLink(renderMarkdown("Task: $description")) {
            val task = ui.newTask()
            task.header("Task: $description")
            task.complete(
              renderMarkdown(
                ui.socketManager.addApplyDiffLinks(
                  codeFiles.filter { (path, _) -> paths?.contains(path) == true },
                  taskActor.answer(listOf(
                    userMessage,
                    architectureResponse.text,
                    codeFiles.filter { (path, _) -> paths?.contains(path) == true }.entries.joinToString("\n\n") {
                      "# ${it.key}\n```${it.key.split('.').last()}\n${it.value}\n```"
                    },
                    "Provide a change for ${paths?.joinToString(",") { it } ?: ""} ($description)"
                  ), api)
                ) { newCodeMap ->
                  newCodeMap.forEach { (path, newCode) ->
                    val prev = codeFiles[path]
                    if (prev != newCode) {
                      codeFiles[path] = newCode
                      task.complete(
                        "<a href='${
                          task.saveFile(
                            path,
                            newCode.toByteArray(Charsets.UTF_8)
                          )
                        }'>$path</a> Updated"
                      )
                    }
                  }
                })
            )
          })

        }
      } catch (e: Throwable) {
        log.warn("Error", e)
        task.error(ui, e)
      }
    }


  }

  companion object {
    private val log = LoggerFactory.getLogger(AutoDevAction::class.java)
    private val agents = mutableMapOf<Session, AutoDevApp>()
    val root: File get() = File(ApplicationEvents.pluginHome, "code_chat")
    private fun initApp(server: AppServer, path: String): ChatServer {
      server.appRegistry[path]?.let { return it }
      val socketServer = object : ApplicationServer(applicationName = "Code Chat", path = path) {
        override val singleInput = true
        override val stickyInput = false
        override fun newSession(user: User?, session: Session) = agents[session]!!.newSession(user, session)
      }
      server.addApp(path, socketServer)
      return socketServer
    }

    interface TaskListParser : java.util.function.Function<String, TaskList> {
      @Description("Parse out a list of tasks to be performed in this project")
      override fun apply(text: String): TaskList
    }

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