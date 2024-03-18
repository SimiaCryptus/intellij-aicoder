package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.dev.AppServer
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.addApplyDiffLinks
import com.github.simiacryptus.aicoder.util.addSaveLinks
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.JsonUtil.toJson
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.core.actors.*
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.ApplicationServices.clientManager
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

class TaskRunner : BaseAction() {

  val path = "/taskDev"
  override fun handle(e: AnActionEvent) {
    val session = StorageInterface.newGlobalID()
    val storage = ApplicationServices.dataStorageFactory(DiffChatAction.root) as DataStorage?
    val selectedFile = UITools.getSelectedFolder(e)
    if (null != storage && null != selectedFile) {
      DataStorage.sessionPaths[session] = selectedFile.toFile
    }
    TaskRunnerApp.agents[session] = TaskRunnerApp(event = e)
    val server = AppServer.getServer(e.project)
    val app = TaskRunnerApp.initApp(server, path)
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
}

class TaskRunnerApp(
  applicationName: String = "Task Planning v1.0",
  path: String = "/taskDev",
  val event: AnActionEvent,
) : ApplicationServer(
  applicationName = applicationName,
  path = path,
) {
  data class Settings(
    val model: ChatModels = ChatModels.GPT4Turbo,
    val parsingModel: ChatModels = ChatModels.GPT35Turbo,
    val temperature: Double = 0.2,
    val budget: Double = 2.0,
  )

  override val settingsClass: Class<*> get() = Settings::class.java

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> initSettings(session: Session): T? = Settings() as T

  override fun userMessage(
    session: Session,
    user: User?,
    userMessage: String,
    ui: ApplicationInterface,
    api: API
  ) {
    try {
      val settings = getSettings<Settings>(session, user)
      if (api is ClientManager.MonitoredClient) api.budget = settings?.budget ?: 2.0
      TaskRunnerAgent(
        user = user,
        session = session,
        dataStorage = dataStorage,
        api = api,
        ui = ui,
        model = settings?.model ?: ChatModels.GPT4Turbo,
        parsingModel = settings?.parsingModel ?: ChatModels.GPT35Turbo,
        temperature = settings?.temperature ?: 0.3,
        event = event,
      ).startProcess(userMessage = userMessage)
    } catch (e: Throwable) {
      log.warn("Error", e)
    }
  }

  companion object {
    fun initApp(server: AppServer, path: String): ChatServer {
      server.appRegistry[path]?.let { return it }
      val socketServer = object : ApplicationServer(applicationName = "Task Agent", path = path) {
        override val singleInput = true
        override val stickyInput = false
        override fun newSession(user: User?, session: Session) = agents[session]!!.newSession(user, session)
      }
      server.addApp(path, socketServer)
      return socketServer
    }

    private val log = LoggerFactory.getLogger(TaskRunnerApp::class.java)
    val agents = mutableMapOf<Session, TaskRunnerApp>()
  }
}

class TaskRunnerAgent(
  user: User?,
  session: Session,
  dataStorage: StorageInterface,
  val ui: ApplicationInterface,
  val api: API,
  model: ChatModels = ChatModels.GPT4Turbo,
  parsingModel: ChatModels = ChatModels.GPT35Turbo,
  temperature: Double = 0.3,
  val actorMap: Map<ActorTypes, BaseActor<*, *>> = mapOf(
    ActorTypes.TaskBreakdown to ParsedActor(
      resultClass = TaskBreakdownResult::class.java,
      prompt = """
        Given a user request, identify and list smaller, actionable tasks that can be directly implemented in code.
        For each task, clearly define its scope, specific requirements, constraints, and the expected outcome.
        Arrange these tasks in a logical sequence for implementation, considering any dependencies.
        Briefly explain your rationale for the task breakdown and ordering.
      """.trimIndent(),
      model = model,
      parsingModel = parsingModel,
      temperature = temperature,
    ),
    ActorTypes.DocumentationGenerator to SimpleActor(
      prompt = """
        Create detailed and clear documentation for the provided code, covering its purpose, functionality, inputs, outputs, and any assumptions or limitations.
        Use a structured and consistent format that facilitates easy understanding and navigation. 
        Include code examples where applicable, and explain the rationale behind key design decisions and algorithm choices.
        Document any known issues or areas for improvement, providing guidance for future developers on how to extend or maintain the code.
      """.trimIndent(),
      model = model,
      temperature = temperature,
    ),
    ActorTypes.NewFileCreator to SimpleActor(
      prompt = """
        Generate the necessary code for a new file based on the given requirements and context. 
        Ensure the code is well-structured, follows best practices, and meets the specified functionality. 
        Provide a clear file name suggestion based on the content and purpose of the file.
          
        Response should use one or more ``` code blocks to output file contents.
        Each file should be preceded by a header that identifies the file being modified.
        
        Example:
        
        Explanation text
        
        ### scripts/filename.js
        ```js
        const b = 2;
        function exampleFunction() {
          return b + 1;
        }
        ```
        
        Continued text
      """.trimIndent(),
      model = model,
      temperature = temperature,
    ),
    ActorTypes.FilePatcher to SimpleActor(
      prompt = """
        Generate a patch for an existing file to modify its functionality or fix issues based on the given requirements and context. 
        Ensure the modifications are efficient, maintain readability, and adhere to coding standards. 
        Provide a summary of the changes made.
          
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
      model = model,
      temperature = temperature,
    ),
    ActorTypes.Inquiry to SimpleActor(
      prompt = """
       Gather detailed information and insights on the given topic. 
       Provide a comprehensive overview, including key concepts, relevant technologies, best practices, and any potential challenges or considerations. 
       Ensure the information is accurate, up-to-date, and well-organized to facilitate easy understanding.
     """.trimIndent(),
      model = model,
      temperature = temperature,
    ),
  ),

  val event: AnActionEvent
) : ActorSystem<TaskRunnerAgent.ActorTypes>(
  actorMap.map { it.key.name to it.value.javaClass }.toMap(),
  dataStorage,
  user,
  session
) {
  val documentationGeneratorActor by lazy { actorMap[ActorTypes.DocumentationGenerator] as SimpleActor }
  val taskBreakdownActor by lazy { actorMap[ActorTypes.TaskBreakdown] as ParsedActor<TaskBreakdownResult> }
  val newFileCreatorActor by lazy { actorMap[ActorTypes.NewFileCreator] as SimpleActor }
  val filePatcherActor by lazy { actorMap[ActorTypes.FilePatcher] as SimpleActor }
  val inquiryActor by lazy { actorMap[ActorTypes.Inquiry] as SimpleActor }


  data class TaskBreakdownResult(
    val tasksByID: Map<String, Task>? = null,
    val finalTaskID: String? = null,
  )

  data class Task(
    val description: String? = null,
    val taskType: TaskType? = null,
    var task_dependencies: List<String>? = null,
    val input_files: List<String>? = null,
    val output_files: List<String>? = null,
  )

  enum class TaskType {
    TaskPlanning,
    Requirements,
    NewFile,
    EditFile,
    Documentation,
  }

  val root by lazy {
    PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)?.map { it.toFile.toPath() }?.toTypedArray()
      ?.commonRoot()!!
  }
  val virtualFiles by lazy { PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext) }
  val codeFiles by lazy {
    mutableMapOf<String, String>().apply {
      virtualFiles?.forEach { file ->
        val code = file.inputStream.bufferedReader().use { it.readText() }
        this[root.relativize(file.toNioPath()).toString()] = code
      }
    }
  }

  fun startProcess(userMessage: String) {
    val eventStatus = """
          |Root: ${root.toFile().absolutePath}
          |
          |Files:
          |${expandPaths(virtualFiles).joinToString("\n") { "* ${root.relativize(it)}" }}  
        """.trimMargin()
    val highLevelPlan = AgentPatterns.iterate(
      input = userMessage,
      heading = userMessage,
      actor = taskBreakdownActor,
      toInput = {
        listOf(
          eventStatus,
          it
        )
      },
      api = api,
      ui = ui,
      outputFn = { task, design ->
        task.add(renderMarkdown("${design.text}\n\n```json\n${toJson(design.obj)}\n```"))
      }
    )

    val pool: ThreadPoolExecutor = clientManager.getPool(session, user, dataStorage)
    val genState = GenState(highLevelPlan.obj.tasksByID?.toMutableMap() ?: mutableMapOf())

    try {
      ui.newTask()
        .complete(renderMarkdown("## Task Graph\n```mermaid\n${buildMermaidGraph(genState.subTasks)}\n```"))
      while (genState.taskIds.isNotEmpty()) {
        val taskId = genState.taskIds.removeAt(0)
        val subTask = genState.subTasks[taskId] ?: throw RuntimeException("Task not found: $taskId")
        subTask.task_dependencies
          ?.associate { it to genState.taskFutures[it] }
          ?.forEach { (id, future) ->
            try {
              future?.get() ?: log.warn("Dependency not found: $id")
            } catch (e: Throwable) {
              log.warn("Error", e)
            }
          }
        genState.taskFutures[taskId] = pool.submit {
          runTask(
            taskId = taskId,
            subTask = subTask,
            userMessage = userMessage,
            highLevelPlan = highLevelPlan,
            genState = genState
          )
        }
      }
      genState.taskFutures.forEach { (id, future) ->
        try {
          future.get() ?: log.warn("Dependency not found: $id")
        } catch (e: Throwable) {
          log.warn("Error", e)
        }
      }
    } catch (e: Throwable) {
      ui.newTask().error(ui, e)
      log.warn("Error during incremental code generation process", e)
    }
  }

  private fun expandPaths(files: Array<out VirtualFile>?) = files?.toList()?.flatMap {
    if (it.isDirectory) it.children.toList() else listOf(it)
  }?.map { it.toFile.toPath() }?.toTypedArray() ?: arrayOf()

  data class GenState(
    val subTasks: MutableMap<String, Task>,
    val taskIds: MutableList<String> = executionOrder(subTasks).toMutableList(),
    val replyText: MutableMap<String, String> = mutableMapOf(),
    val completedTasks: MutableList<String> = mutableListOf(),
    val taskFutures: MutableMap<String, Future<*>> = mutableMapOf(),
  )

  private fun runTask(
    taskId: String,
    subTask: Task,
    userMessage: String,
    highLevelPlan: ParsedResponse<TaskBreakdownResult>,
    genState: GenState,
  ) {
    val task = ui.newTask()
    try {
      val dependencies = subTask.task_dependencies?.toMutableSet() ?: mutableSetOf()
      dependencies += getAllDependencies(subTask, genState.subTasks)
      task.add(
        renderMarkdown(
          "## Task `${taskId}`\n${subTask.description ?: ""}\n\n```json\n${toJson(subTask)}\n```\n\n### Dependencies:\n${
            dependencies.joinToString(
              "\n"
            ) { "- $it" }
          }"
        )
      )
      val priorCode = dependencies
        .joinToString("\n\n\n") {
          """
            # $it
            ${genState.replyText[it] ?: ""}
          """.trimIndent()
        }
      val inputFileCode = subTask.input_files?.joinToString("\n\n\n") {
        """
          # $it
          ```
          ${codeFiles[it]}
          ```
        """.trimIndent()
      } ?: ""
      when (subTask.taskType) {

        TaskType.NewFile -> {
          val codeResult = newFileCreatorActor.answer(
            listOf(
              userMessage,
              highLevelPlan.text,
              priorCode,
              inputFileCode,
              subTask.description ?: "",
            ), api
          )
          genState.replyText[taskId] = codeResult
          task.complete(renderMarkdown(ui.socketManager.addSaveLinks(codeResult) { path, newCode ->
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
          }))
        }

        TaskType.EditFile -> {
          val codeResult = filePatcherActor.answer(
            listOf(
              userMessage,
              highLevelPlan.text,
              priorCode,
              inputFileCode,
              subTask.description ?: "",
            ), api
          )
          genState.replyText[taskId] = codeResult
          task.complete(renderMarkdown(ui.socketManager.addApplyDiffLinks(codeFiles, codeResult) { newCodeMap ->
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
          }))
        }

        TaskType.Documentation -> {
          val docResult = documentationGeneratorActor.answer(
            listOf(
              userMessage,
              highLevelPlan.text,
              priorCode,
              inputFileCode,
              priorCode
            ), api
          )
          genState.replyText[taskId] = docResult
          task.complete(renderMarkdown("## Generated Documentation\n$docResult"))
        }

        TaskType.Requirements -> {
          val inquiryResult = AgentPatterns.iterate(
            input = "Expand ${subTask.description ?: ""}",
            heading = "Expand ${subTask.description ?: ""}",
            actor = inquiryActor,
            toInput = {
              listOf(
                userMessage,
                highLevelPlan.text,
                priorCode,
                inputFileCode,
                it,
              )
            },
            api = api,
            ui = ui,
            outputFn = { task, design ->
              task.add(renderMarkdown(design))
            }
          )
          genState.replyText[taskId] = inquiryResult
          task.complete(renderMarkdown("## Generated Inquiry Response\n$inquiryResult"))
        }

        TaskType.TaskPlanning -> {
          val subPlan = AgentPatterns.iterate(
            input = "Expand ${subTask.description ?: ""}",
            heading = "Expand ${subTask.description ?: ""}",
            actor = taskBreakdownActor,
            toInput = {
              listOf(
                userMessage,
                highLevelPlan.text,
                priorCode,
                inputFileCode,
                it
              )
            },
            api = api,
            ui = ui,
            outputFn = { task, design ->
              task.add(renderMarkdown("${design.text}\n\n```json\n${toJson(design.obj)}\n```"))
            }
          )
          genState.replyText[taskId] = subPlan.text
          var newTasks = subPlan.obj.tasksByID
          val conflictingKeys = newTasks?.keys?.intersect(genState.subTasks.keys)
          newTasks = newTasks?.entries?.associate { (key, value) ->
            (when {
              conflictingKeys?.contains(key) == true -> "${taskId}_${key}"
              else -> key
            }) to value.copy(task_dependencies = value.task_dependencies?.map { key ->
              when {
                conflictingKeys?.contains(key) == true -> "${taskId}_${key}"
                else -> key
              }
            })
          }
          genState.subTasks.putAll(newTasks ?: emptyMap())
          executionOrder(newTasks ?: emptyMap()).reversed().forEach { genState.taskIds.add(0, it) }
          genState.subTasks.values.forEach {
            it.task_dependencies = it.task_dependencies?.map { dep ->
              when {
                dep == taskId -> subPlan.obj.finalTaskID ?: dep
                else -> dep
              }
            }
          }
          task.complete(renderMarkdown("## Task Dependency Graph\n```mermaid\n${buildMermaidGraph(genState.subTasks)}\n```"))
        }

        else -> null
      }
    } catch (e: Exception) {
      task.error(ui, e)
      log.warn("Error during task execution", e)
    } finally {
      genState.completedTasks.add(taskId)
    }
  }

  private fun getAllDependencies(subTask: Task, subTasks: MutableMap<String, Task>): List<String> {
    return getAllDependenciesHelper(subTask, subTasks, mutableSetOf())
  }

  private fun getAllDependenciesHelper(
    subTask: Task,
    subTasks: MutableMap<String, Task>,
    visited: MutableSet<String>
  ): List<String> {
    val dependencies = subTask.task_dependencies?.toMutableList() ?: mutableListOf()
    subTask.task_dependencies?.forEach { dep ->
      if (dep in visited) return@forEach
      val subTask = subTasks[dep]
      if (subTask != null) {
        visited.add(dep)
        dependencies.addAll(getAllDependenciesHelper(subTask, subTasks, visited))
      }
    }
    return dependencies
  }

  private fun buildMermaidGraph(subTasks: Map<String, Task>): String {
    val graphBuilder = StringBuilder("graph TD;\n")
    val escapeMermaidCharacters: (String) -> String = { input ->
      input.replace("\"", "\\\"")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("(", "\\(")
        .replace(")", "\\)")
    }
    subTasks.forEach { (taskId, task) ->
      val taskId = taskId.replace(" ", "_")
      val escapedDescription = escapeMermaidCharacters(task.description ?: "")
      graphBuilder.append("    ${taskId}[\"${escapedDescription}\"];\n")
      task.task_dependencies?.forEach { dependency ->
        graphBuilder.append("    ${dependency.replace(" ", "_")} --> ${taskId};\n")
      }
    }
    return graphBuilder.toString()
  }

  enum class ActorTypes {
    TaskBreakdown,
    DocumentationGenerator,
    NewFileCreator,
    FilePatcher,
    Inquiry,
  }

  companion object {
    private val log = LoggerFactory.getLogger(TaskRunnerAgent::class.java)
    fun executionOrder(tasks: Map<String, Task>): List<String> {
      val taskIds: MutableList<String> = mutableListOf()
      val taskMap = tasks.toMutableMap()
      while (taskMap.isNotEmpty()) {
        val nextTasks = taskMap.filter { (_, task) -> task.task_dependencies?.all { taskIds.contains(it) } ?: true }
        if (nextTasks.isEmpty()) {
          throw RuntimeException("Circular dependency detected in task breakdown")
        }
        taskIds.addAll(nextTasks.keys)
        nextTasks.keys.forEach { taskMap.remove(it) }
      }
      return taskIds
    }
  }
}
