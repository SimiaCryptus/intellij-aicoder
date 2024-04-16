package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.dev.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.diff.addApplyFileDiffLinks
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil.toJson
import com.simiacryptus.skyenet.Acceptable
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.core.actors.*
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

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
    showMenubar = false,
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
        model = settings.model!!,
        event = event,
        parsingModel = AppSettingsState.instance.defaultFastModel(),
      ).start(
        userMessage = userMessage,
      )
    }

    data class Settings(
      val budget: Double? = 2.00,
      val tools: List<String> = emptyList(),
      val model: ChatModels? = AppSettingsState.instance.defaultSmartModel(),
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
    val parsingModel: ChatModels,
    val tools: List<String> = emptyList(),
    actorMap: Map<ActorTypes, BaseActor<*, *>> = mapOf(
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
 import com.simiacryptus.skyenet.webui.components.CheckboxTab
          - const b = 2;
          + const a = 1;
          ```
          
          Continued text
          """.trimIndent(),
        model = model
      ),
    ),
    val event: AnActionEvent,
  ) : ActorSystem<AutoDevAgent.ActorTypes>(
    actorMap.map { it.key.name to it.value }.toMap(), dataStorage, user, session
  ) {
    enum class ActorTypes {
      DesignActor,
      TaskCodingActor,
    }

    private val designActor by lazy { getActor(ActorTypes.DesignActor) as ParsedActor<TaskList> }
    private val taskActor by lazy { getActor(ActorTypes.TaskCodingActor) as SimpleActor }

    fun start(
      userMessage: String,
    ) {
      val codeFiles = mutableMapOf<String, String>()
      val root = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)
        ?.map { it.toFile.toPath() }?.toTypedArray()?.commonRoot()!!
      PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)?.forEach { file ->
        val code = file.inputStream.bufferedReader().use { it.readText() }
        codeFiles[root.relativize(file.toNioPath()).toString()] = code
      }
      require(codeFiles.isNotEmpty()) { "No files selected" }
      fun codeSummary() = codeFiles.entries.joinToString("\n\n") { (path, code) ->
        "# $path\n```${
          path.split('.').last()
        }\n${code/*.indent("  ")*/}\n```"
      }

      val task = ui.newTask()
      val toInput = { it: String -> listOf(codeSummary(), it) }
      val architectureResponse = Acceptable(
        task = task,
        userMessage = userMessage,
        initialResponse = { it: String -> designActor.answer(toInput(it), api = api) },
        outputFn = { design: ParsedResponse<TaskList> ->
          //          renderMarkdown("${design.text}\n\n```json\n${JsonUtil.toJson(design.obj)/*.indent("  ")*/}\n```")
          AgentPatterns.displayMapInTabs(
            mapOf(
              "Text" to renderMarkdown(design.text, ui=ui),
              "JSON" to renderMarkdown("```json\n${toJson(design.obj)/*.indent("  ")*/}\n```", ui=ui),
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
        heading = userMessage
      ).call()

      try {
        architectureResponse.obj.tasks.forEach { (paths, description) ->
          task.complete(ui.hrefLink(renderMarkdown("Task: $description", ui=ui)) {
            val task = ui.newTask()
            task.header("Task: $description")
            val process = { it: StringBuilder ->
              val filter = codeFiles.filter { (path, _) -> paths?.find { path.contains(it) }?.isNotEmpty() == true }
              require(filter.isNotEmpty()) {
                """
                              |No files found for $paths
                              |
                              |Root:
                              |$root
                              |
                              |Files:
                              |${codeFiles.keys.joinToString("\n")}
                              |
                              |Paths:
                              |${paths?.joinToString("\n") ?: ""}
                              |
                            """.trimMargin()
              }
              ui.socketManager.addApplyFileDiffLinks(
                  root = root,
                  code = { codeFiles },
                  response = taskActor.answer(listOf(
                    codeSummary(),
                    userMessage,
                    filter.entries.joinToString("\n\n") {
                      "# ${it.key}\n```${
                        it.key.split('.').last()?.let { /*escapeHtml4*/it/*.indent("  ")*/ }
                      }\n${it.value/*.indent("  ")*/}\n```"
                    },
                    architectureResponse.text,
                    "Provide a change for ${paths?.joinToString(",") { it } ?: ""} ($description)"
                  ), api),
                  handle = { newCodeMap ->
                    newCodeMap.forEach { (path, newCode) ->
                      task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                    }
                  },
                  ui = ui
              )
            }
            Retryable(ui, task, process).apply { set(label(size), process(container!!)) }
          })
        }
      } catch (e: Throwable) {
        log.warn("Error", e)
        task.error(ui, e)
      }
    }
  }

  val taskStates = mutableMapOf<String, TaskState>()

  companion object {
    private val log = LoggerFactory.getLogger(AutoDevAction::class.java)
    private val agents = mutableMapOf<Session, AutoDevApp>()
    val root: File get() = File(AppSettingsState.instance.pluginHome, "code_chat")
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
  enum class TaskState {
    Pending,
    InProgress,
    Completed
  }
}