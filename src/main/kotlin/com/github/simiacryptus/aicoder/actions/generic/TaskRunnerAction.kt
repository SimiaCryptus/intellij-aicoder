package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.dev.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.diff.addApplyFileDiffLinks
import com.github.simiacryptus.diff.addSaveLinks
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys.VIRTUAL_FILE_ARRAY
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil.toJson
import com.simiacryptus.skyenet.Acceptable
import com.simiacryptus.skyenet.AgentPatterns.displayMapInTabs
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.coding.CodingAgent
import com.simiacryptus.skyenet.core.actors.*
import com.simiacryptus.skyenet.core.platform.ApplicationServices.clientManager
import com.simiacryptus.skyenet.core.platform.ClientManager
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.interpreter.ProcessInterpreter
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

class TaskRunnerAction : BaseAction() {

    val path = "/taskDev"
    override fun handle(e: AnActionEvent) {
        val session = StorageInterface.newGlobalID()
        val folder = UITools.getSelectedFolder(e)
        val root = if (null != folder) {
            folder.toFile
        } else {
            getModuleRootForFile(UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException(""))
        }
        DataStorage.sessionPaths[session] = root
        TaskRunnerApp.agents[session] = TaskRunnerApp(event = e, root = root)
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
    override val root: File,
) : ApplicationServer(
    applicationName = applicationName,
    path = path,
    showMenubar = false,
) {
    data class Settings(
        val model: ChatModels = AppSettingsState.instance.smartModel.chatModel(),
        val parsingModel: ChatModels = AppSettingsState.instance.fastModel.chatModel(),
        val temperature: Double = 0.2,
        val budget: Double = 2.0,
        val taskPlanningEnabled: Boolean = false,
        val shellCommandTaskEnabled: Boolean = true,
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
                model = settings?.model ?: AppSettingsState.instance.smartModel.chatModel(),
                parsingModel = settings?.parsingModel ?: AppSettingsState.instance.fastModel.chatModel(),
                temperature = settings?.temperature ?: 0.3,
                event = event,
                root = root.toPath(),
                taskPlanningEnabled = false,
                shellCommandTaskEnabled = false,
            ).startProcess(userMessage = userMessage)
        } catch (e: Throwable) {
            ui.newTask().error(ui, e)
            log.warn("Error", e)
        }
    }

    companion object {
        fun initApp(server: AppServer, path: String): ChatServer {
            server.appRegistry[path]?.let { return it }
            val socketServer = object : ApplicationServer(
                applicationName = "Task Agent",
                path = path,
                showMenubar = false,
            ) {
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
    val taskPlanningEnabled: Boolean,
    val shellCommandTaskEnabled: Boolean,
    val env: Map<String, String> = mapOf(),
    val workingDir: String = ".",
    val language: String = if (isWindows) "powershell" else "bash",
    val command: List<String> = listOf(language),
    val actorMap: Map<ActorTypes, BaseActor<*, *>> = mapOf(
        ActorTypes.TaskBreakdown to ParsedActor(
            resultClass = TaskBreakdownResult::class.java,
            prompt = """
        |Given a user request, identify and list smaller, actionable tasks that can be directly implemented in code.
        |Detail files input and output as well as task execution dependencies.
        |Keep in mind that implementation details need to be shared between the file generation tasks.
        |Creating directories and initializing source control are out of scope.
        |
        |Tasks can be of the following types: 
        |${
                if (!taskPlanningEnabled) "" else """
          |* TaskPlanning - High-level planning and organization of tasks - identify smaller, actionable tasks based on the information available at task execution time.
          |  ** Specify the prior tasks and the goal of the task
        """.trimMargin().trim()
            }
        |${
                if (!shellCommandTaskEnabled) "" else """
          |* RunShellCommand - Execute shell commands and provide the output
          |  ** Specify the command to be executed, or describe the task to be performed
          |  ** List input files/tasks to be examined when writing the command
          """.trimMargin().trim()
            }
        |* Inquiry - Answer questions by reading in files and providing a summary that can be discussed with and approved by the user
        |  ** Specify the questions and the goal of the inquiry
        |  ** List input files to be examined when answering the questions
        |* NewFile - Create one or more new files, carefully considering how they fit into the existing project structure
        |  ** For each file, specify the relative file path and the purpose of the file
        |  ** List input files/tasks to be examined when authoring the new files
        |* EditFile - Modify existing files
        |  ** For each file, specify the relative file path and the goal of the modification
        |  ** List input files/tasks to be examined when designing the modifications
        |* Documentation - Generate documentation
        |  ** List input files/tasks to be examined
      """.trimMargin(),
            model = model,
            parsingModel = parsingModel,
            temperature = temperature,
        ),
        ActorTypes.DocumentationGenerator to SimpleActor(
            name = "DocumentationGenerator",
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
            name = "NewFileCreator",
            prompt = """
        Generate the necessary code for a new file based on the given requirements and context. 
        Ensure the code is well-structured, follows best practices, and meets the specified functionality. 
        Carefully consider how the new file fits into the existing project structure and architecture.
        Avoid creating files that duplicate functionality or introduce inconsistencies.
        Provide a clear file name suggestion based on the content and purpose of the file.
          
        Response should use one or more ``` code blocks to output file contents.
        Triple backticks should be bracketed by newlines and an optional the language identifier.
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
            name = "FilePatcher",
            prompt = """
        Generate a patch for an existing file to modify its functionality or fix issues based on the given requirements and context. 
        Ensure the modifications are efficient, maintain readability, and adhere to coding standards. 
        Carefully review the existing code and project structure to ensure the changes are consistent and do not introduce bugs.
        Consider the impact of the modifications on other parts of the codebase.

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
            name = "Inquiry",
            prompt = """
        Create code for a new file that fulfills the specified requirements and context.
        Given a detailed user request, break it down into smaller, actionable tasks suitable for software development.
        Compile comprehensive information and insights on the specified topic.
        Provide a comprehensive overview, including key concepts, relevant technologies, best practices, and any potential challenges or considerations. 
        Ensure the information is accurate, up-to-date, and well-organized to facilitate easy understanding.

        When generating insights, consider the existing project context and focus on information that is directly relevant and applicable.
        Focus on generating insights and information that support the task types available in the system (${
                if (!taskPlanningEnabled) "" else "TaskPlanning, "
            }${
                if (!shellCommandTaskEnabled) "" else "RunShellCommand, "
            }Requirements, NewFile, EditFile, Documentation).
        This will ensure that the inquiries are tailored to assist in the planning and execution of tasks within the system's framework.
     """.trimIndent(),
            model = model,
            temperature = temperature,
        ),
    ) + (if (!shellCommandTaskEnabled) mapOf() else mapOf(
        ActorTypes.RunShellCommand to CodingActor(
            name = "RunShellCommand",
            interpreterClass = ProcessInterpreter::class,
            details = """
        Execute the following shell command(s) and provide the output. Ensure to handle any errors or exceptions gracefully.
    
        Note: This task is for running simple and safe commands. Avoid executing commands that can cause harm to the system or compromise security.
      """.trimIndent(),
            symbols = mapOf(
                "env" to (env ?: mapOf()),
                "workingDir" to File(workingDir ?: ".").absolutePath,
                "language" to (language ?: "bash"),
                "command" to (command ?: listOf("bash")),
            ),
            model = model,
            temperature = temperature,
        ),
    )),

    val event: AnActionEvent,
    val root: Path
) : ActorSystem<TaskRunnerAgent.Companion.ActorTypes>(
    actorMap.map { it.key.name to it.value }.toMap(),
    dataStorage,
    user,
    session
) {
    val documentationGeneratorActor by lazy { actorMap[ActorTypes.DocumentationGenerator] as SimpleActor }
    val taskBreakdownActor by lazy { actorMap[ActorTypes.TaskBreakdown] as ParsedActor<TaskBreakdownResult> }
    val newFileCreatorActor by lazy { actorMap[ActorTypes.NewFileCreator] as SimpleActor }
    val filePatcherActor by lazy { actorMap[ActorTypes.FilePatcher] as SimpleActor }
    val inquiryActor by lazy { actorMap[ActorTypes.Inquiry] as SimpleActor }
    val shellCommandActor by lazy { actorMap[ActorTypes.RunShellCommand] as CodingActor }

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
        var state: TaskState? = null,
    )

    enum class TaskState {
        Pending,
        InProgress,
        Completed,
    }

    enum class TaskType {
        TaskPlanning,
        Inquiry,
        NewFile,
        EditFile,
        Documentation,
        RunShellCommand,
    }

    val virtualFiles by lazy {
        expandFileList(
            VIRTUAL_FILE_ARRAY.getData(event.dataContext) ?: arrayOf()
        )
    }

    private fun expandFileList(data: Array<VirtualFile>): Array<VirtualFile> {
        return data.flatMap {
            (when {
                it.name.startsWith(".") -> arrayOf()
                it.length > 1e6 -> arrayOf()
                it.extension?.lowercase(Locale.getDefault()) in
                        setOf("jar", "zip", "class", "png", "jpg", "jpeg", "gif", "ico") -> arrayOf()

                it.isDirectory -> expandFileList(it.children)
                else -> arrayOf(it)
            }).toList()
        }.toTypedArray()
    }

    val codeFiles = mutableMapOf<String, String>().apply {
        virtualFiles.filter { it.isFile }.forEach { file ->
            val code = file.inputStream.bufferedReader().use { it.readText() }
            this[root.relativize(file.toNioPath()).toString()] = code
        }
    }

    fun startProcess(userMessage: String) {
        val codeFiles = codeFiles
        val eventStatus = if (!codeFiles.all { File(it.key).isFile } || codeFiles.size > 2) """
      |Files:
      |${codeFiles.keys.joinToString("\n") { "* ${it}" }}  
    """.trimMargin() else {
            """
            |${
                virtualFiles.joinToString("\n\n") {
                    val path = root.relativize(it.toNioPath())
                    """
              |## $path
              |
              |${(codeFiles[path.toString()] ?: "").let { "```\n${it/*.indent("  ")*/}\n```" }}
            """.trimMargin()
                }
            }
          """.trimMargin()
        }
        val task = ui.newTask()
        val toInput = { it: String ->
            listOf(
                eventStatus,
                it
            )
        }
        val highLevelPlan = Acceptable(
            task = task,
            heading = renderMarkdown(userMessage, ui = ui),
            userMessage = userMessage,
            initialResponse = { it: String -> taskBreakdownActor.answer(toInput(it), api = api) },
            outputFn = { design: ParsedResponse<TaskBreakdownResult> ->
                displayMapInTabs(
                    mapOf(
                        "Text" to renderMarkdown(design.text, ui = ui),
                        "JSON" to renderMarkdown("```json\n${toJson(design.obj)/*.indent("  ")*/}\n```", ui = ui),
                    )
                )
            },
            ui = ui,
            reviseResponse = { userMessages: List<Pair<String, Role>> ->
                taskBreakdownActor.respond(
                    messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                        .toTypedArray<ApiModel.ChatMessage>()),
                    input = toInput(userMessage),
                    api = api
                )
            },
        ).call()

        try {
            val tasksByID =
                highLevelPlan.obj.tasksByID?.entries?.toTypedArray()?.associate { it.key to it.value } ?: mapOf()
            val pool: ThreadPoolExecutor = clientManager.getPool(session, user, dataStorage)
            val genState = GenState(tasksByID.toMutableMap())
            val diagramTask = ui.newTask()
            val diagramBuffer =
                diagramTask.add(
                    renderMarkdown(
                        "## Task Dependency Graph\n```mermaid\n${buildMermaidGraph(genState.subTasks)}\n```",
                        ui = ui
                    )
                )
            val taskTabs = object : TabbedDisplay(ui.newTask()) {
                override fun renderTabButtons(): String {
                    diagramBuffer?.set(
                        renderMarkdown(
                            "## Task Dependency Graph\n```mermaid\n${
                                buildMermaidGraph(
                                    genState.subTasks
                                )
                            }\n```", ui = ui
                        )
                    )
                    diagramTask.complete()
                    return buildString {
                        append("<div class='tabs'>\n")
                        super.tabs.withIndex().forEach { (idx, t) ->
                            val (taskId, taskV) = t
                            val subTask = genState.tasksByDescription[taskId]
                            if (null == subTask) {
                                log.warn("Task tab not found: $taskId")
                            }
                            val isChecked = if (taskId in genState.taskIdProcessingQueue) "checked" else ""
                            val style = when (subTask?.state) {
                                TaskState.Completed -> " style='text-decoration: line-through;'"
                                null -> " style='opacity: 20%;'"
                                TaskState.Pending -> " style='opacity: 30%;'"
                                else -> ""
                            }
                            append("<label class='tab-button' data-for-tab='${idx}'$style><input type='checkbox' $isChecked disabled /> $taskId</label><br/>\n")
                        }
                        append("</div>")
                    }
                }
            }
            genState.taskIdProcessingQueue.forEach { taskId ->
                val newTask = ui.newTask(false)
                genState.uitaskMap[taskId] = newTask
                val subtask = genState.subTasks[taskId]
                val description = subtask?.description
                log.debug("Creating task tab: $taskId ${System.identityHashCode(subtask)} $description")
                taskTabs[description ?: taskId] = newTask.placeholder
            }
            Thread.sleep(100)
            while (genState.taskIdProcessingQueue.isNotEmpty()) {
                val taskId = genState.taskIdProcessingQueue.removeAt(0)
                val subTask = genState.subTasks[taskId] ?: throw RuntimeException("Task not found: $taskId")
                genState.taskFutures[taskId] = pool.submit {
                    subTask.state = TaskState.Pending
                    taskTabs.update()
                    log.debug("Awaiting dependencies: ${subTask.task_dependencies?.joinToString(", ") ?: ""}")
                    subTask.task_dependencies
                        ?.associate { it to genState.taskFutures[it] }
                        ?.forEach { (id, future) ->
                            try {
                                future?.get() ?: log.warn("Dependency not found: $id")
                            } catch (e: Throwable) {
                                log.warn("Error", e)
                            }
                        }
                    subTask.state = TaskState.InProgress
                    taskTabs.update()
                    log.debug("Running task: ${System.identityHashCode(subTask)} ${subTask.description}")
                    runTask(
                        taskId = taskId,
                        subTask = subTask,
                        userMessage = userMessage,
                        highLevelPlan = highLevelPlan,
                        genState = genState,
                        task = genState.uitaskMap.get(taskId) ?: ui.newTask(false),
                        taskTabs = taskTabs
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
            log.warn("Error during incremental code generation process", e)
            task.error(ui, e)
        }
    }

    data class GenState(
        val subTasks: MutableMap<String, Task>,
        val tasksByDescription: MutableMap<String?, Task> = subTasks.entries.toTypedArray()
            .associate { it.value.description to it.value }.toMutableMap(),
        val taskIdProcessingQueue: MutableList<String> = executionOrder(subTasks).toMutableList(),
        val taskResult: MutableMap<String, String> = mutableMapOf(),
        val completedTasks: MutableList<String> = mutableListOf(),
        val taskFutures: MutableMap<String, Future<*>> = mutableMapOf(),
        val uitaskMap: MutableMap<String, SessionTask> = mutableMapOf(),
    )

    private fun runTask(
        taskId: String,
        subTask: Task,
        userMessage: String,
        highLevelPlan: ParsedResponse<TaskBreakdownResult>,
        genState: GenState,
        task: SessionTask,
        taskTabs: TabbedDisplay,
    ) {
        try {
            val dependencies = subTask.task_dependencies?.toMutableSet() ?: mutableSetOf()
            dependencies += getAllDependencies(subTask, genState.subTasks)
            val priorCode = dependencies
                .joinToString("\n\n\n") { dependency ->
                    """
          |# $dependency
          |
          |${genState.taskResult[dependency] ?: ""}
          """.trimMargin()
                }
            val codeFiles = codeFiles
            fun inputFileCode() = subTask.input_files?.joinToString("\n\n\n") {
                try {
                    """
        |# $it
        |
        |```
        |${codeFiles[it] ?: root.resolve(it).toFile().readText()}
        |```
        """.trimMargin()
                } catch (e: Throwable) {
                    log.warn("Error: root=$root    ", e)
                    ""
                }
            } ?: ""
            task.add(
                renderMarkdown(
                    """
          |## Task `${taskId}`
          |${subTask.description ?: ""}
          |
          |```json
          |${toJson(subTask)/*.indent("  ")*/}
          |```
          |
          |### Dependencies:
          |${dependencies.joinToString("\n") { "- $it" }}
          |
          """.trimMargin(), ui = ui
                )
            )

            when (subTask.taskType) {

                TaskType.NewFile -> {
                    val semaphore = Semaphore(0)
                    createFiles(
                        task = task,
                        userMessage = userMessage,
                        highLevelPlan = highLevelPlan,
                        priorCode = priorCode,
                        inputFileCode = ::inputFileCode,
                        subTask = subTask,
                        genState = genState,
                        taskId = taskId,
                        taskTabs = taskTabs,
                    ) { semaphore.release() }
                    try {
                        semaphore.acquire()
                    } catch (e: Throwable) {
                        log.warn("Error", e)
                    }

                }

                TaskType.EditFile -> {
                    val semaphore = Semaphore(0)
                    editFiles(
                        task = task,
                        userMessage = userMessage,
                        highLevelPlan = highLevelPlan,
                        priorCode = priorCode,
                        inputFileCode = ::inputFileCode,
                        subTask = subTask,
                        genState = genState,
                        taskId = taskId,
                        taskTabs = taskTabs,
                    ) { semaphore.release() }
                    try {
                        semaphore.acquire()
                    } catch (e: Throwable) {
                        log.warn("Error", e)
                    }
                }

                TaskType.Documentation -> {
                    val semaphore = Semaphore(0)
                    document(
                        task = task,
                        userMessage = userMessage,
                        highLevelPlan = highLevelPlan,
                        priorCode = priorCode,
                        inputFileCode = ::inputFileCode,
                        genState = genState,
                        taskId = taskId,
                        taskTabs = taskTabs,
                    ) {
                        semaphore.release()
                    }
                    try {
                        semaphore.acquire()
                    } catch (e: Throwable) {
                        log.warn("Error", e)
                    }
                }

                TaskType.Inquiry -> {
                    inquiry(
                        subTask = subTask,
                        userMessage = userMessage,
                        highLevelPlan = highLevelPlan,
                        priorCode = priorCode,
                        inputFileCode = ::inputFileCode,
                        genState = genState,
                        taskId = taskId,
                        task = task,
                        taskTabs = taskTabs,
                    )
                }

                TaskType.TaskPlanning -> {
                    if (taskPlanningEnabled) taskPlanning(
                        subTask = subTask,
                        userMessage = userMessage,
                        highLevelPlan = highLevelPlan,
                        priorCode = priorCode,
                        inputFileCode = ::inputFileCode,
                        genState = genState,
                        taskId = taskId,
                        task = task,
                        taskTabs = taskTabs,
                    )
                }

                TaskType.RunShellCommand -> {
                    if (shellCommandTaskEnabled) {
                        val semaphore = Semaphore(0)
                        runShellCommand(
                            task = task,
                            userMessage = userMessage,
                            highLevelPlan = highLevelPlan,
                            priorCode = priorCode,
                            inputFileCode = ::inputFileCode,
                            genState = genState,
                            taskId = taskId,
                            taskTabs = taskTabs,
                        ) {
                            semaphore.release()
                        }
                        try {
                            semaphore.acquire()
                        } catch (e: Throwable) {
                            log.warn("Error", e)
                        }
                        log.debug("Completed shell command: $taskId")
                    }
                }

                else -> null
            }
        } catch (e: Exception) {
            log.warn("Error during task execution", e)
            task.error(ui, e)
        } finally {
            genState.completedTasks.add(taskId)
            subTask.state = TaskState.Completed
            log.debug("Completed task: $taskId ${System.identityHashCode(subTask)}")
            taskTabs.update()
        }
    }

    private fun runShellCommand(
        task: SessionTask,
        userMessage: String,
        highLevelPlan: ParsedResponse<TaskBreakdownResult>,
        priorCode: String,
        inputFileCode: () -> String,
        genState: GenState,
        taskId: String,
        taskTabs: TabbedDisplay,
        function: () -> Unit
    ) {
        object : CodingAgent<ProcessInterpreter>(
            api = api,
            dataStorage = dataStorage,
            session = session,
            user = user,
            ui = ui,
            interpreter = shellCommandActor.interpreterClass as KClass<ProcessInterpreter>,
            symbols = shellCommandActor.symbols,
            temperature = shellCommandActor.temperature,
            details = shellCommandActor.details,
            model = shellCommandActor.model,
        ) {
            override fun displayFeedback(
                task: SessionTask,
                request: CodingActor.CodeRequest,
                response: CodingActor.CodeResult
            ) {
                val formText = StringBuilder()
                var formHandle: StringBuilder? = null
                formHandle = task.add(
                    """
          |<div style="display: flex;flex-direction: column;">
          |${if (!super.canPlay) "" else super.playButton(task, request, response, formText) { formHandle!! }}
          |${acceptButton(task, request, response, formText) { formHandle!! }}
          |</div>
          |${super.reviseMsg(task, request, response, formText) { formHandle!! }}
          """.trimMargin(), className = "reply-message"
                )
                formText.append(formHandle.toString())
                formHandle.toString()
                task.complete()
            }

            fun acceptButton(
                task: SessionTask,
                request: CodingActor.CodeRequest,
                response: CodingActor.CodeResult,
                formText: StringBuilder,
                formHandle: () -> StringBuilder
            ): String {
                return ui.hrefLink("Accept", "href-link play-button") {
                    genState.taskResult[taskId] = response.let {
                        """
                  |## Shell Command Output
                  |
                  |```
                  |${response.code}
                  |```
                  |
                  |```
                  |${response.renderedResponse}
                  |```
                  """.trimMargin()
                    }
                    function()
                }
            }
        }.apply {
            start(
                codeRequest(
                    listOf(
                        userMessage to Role.user,
                        highLevelPlan.text to Role.assistant,
                        priorCode to Role.assistant,
                        inputFileCode() to Role.assistant,
                    )
                )
            )
        }
    }

    private fun createFiles(
        task: SessionTask,
        userMessage: String,
        highLevelPlan: ParsedResponse<TaskBreakdownResult>,
        priorCode: String,
        inputFileCode: () -> String,
        subTask: Task,
        genState: GenState,
        taskId: String,
        taskTabs: TabbedDisplay,
        onComplete: () -> Unit
    ) {

        val process = { sb: StringBuilder ->
            val codeResult = newFileCreatorActor.answer(
                listOf(
                    userMessage,
                    highLevelPlan.text,
                    priorCode,
                    inputFileCode(),
                    subTask.description ?: "",
                ).filter { it.isNotBlank() }, api
            )
            genState.taskResult[taskId] = codeResult
            renderMarkdown(ui.socketManager.addSaveLinks(codeResult, task, ui = ui) { path, newCode ->
                val prev = codeFiles[path]
                if (prev != newCode) {
//          codeFiles[path] = newCode
                    val bytes = newCode.toByteArray(Charsets.UTF_8)
                    val saveFile = task.saveFile(path, bytes)
                    task.complete("<a href='$saveFile'>$path</a> Created")
                } else {
                    task.complete("No changes to $path")
                }
            }, ui = ui) + acceptButtonFooter(sb) {
                taskTabs.selectedTab = taskTabs.selectedTab + 1
                taskTabs.update()
                onComplete()
            }
        }
        object : Retryable(ui, task, process) {
            init {
                set(label(size), process(container!!))
            }
        }
    }

    private fun editFiles(
        task: SessionTask,
        userMessage: String,
        highLevelPlan: ParsedResponse<TaskBreakdownResult>,
        priorCode: String,
        inputFileCode: () -> String,
        subTask: Task,
        genState: GenState,
        taskId: String,
        taskTabs: TabbedDisplay,
        onComplete: () -> Unit,
    ) {
        val process = { sb: StringBuilder ->
            val codeResult = filePatcherActor.answer(
                listOf(
                    userMessage,
                    highLevelPlan.text,
                    priorCode,
                    inputFileCode(),
                    subTask.description ?: "",
                ).filter { it.isNotBlank() }, api
            )
            genState.taskResult[taskId] = codeResult
            renderMarkdown(
                ui.socketManager.addApplyFileDiffLinks(
                    root = root,
                    code = codeFiles,
                    response = codeResult,
                    handle = { newCodeMap ->
                        val codeFiles = codeFiles
                        newCodeMap.forEach { (path, newCode) ->
                            val prev = codeFiles[path]
                            if (prev != newCode) {
//            codeFiles[path] = newCode
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
                    },
                    ui = ui
                ) + acceptButtonFooter(sb) {
                    taskTabs.selectedTab += 1
                    taskTabs.update()
                    task.complete()
                    onComplete()
                }, ui = ui
            )
        }
        object : Retryable(ui, task, process) {
            init {
                set(label(size), process(container!!))
            }
        }
    }

    private fun document(
        task: SessionTask,
        userMessage: String,
        highLevelPlan: ParsedResponse<TaskBreakdownResult>,
        priorCode: String,
        inputFileCode: () -> String,
        genState: GenState,
        taskId: String,
        taskTabs: TabbedDisplay,
        onComplete: () -> Unit
    ) {
        val process = { sb: StringBuilder ->
            val docResult = documentationGeneratorActor.answer(
                listOf(
                    userMessage,
                    highLevelPlan.text,
                    priorCode,
                    inputFileCode(),
                ).filter { it.isNotBlank() }, api
            )
            genState.taskResult[taskId] = docResult
            renderMarkdown("## Generated Documentation\n$docResult", ui = ui) + acceptButtonFooter(sb) {
                taskTabs.selectedTab = taskTabs.selectedTab + 1
                taskTabs.update()
                task.complete()
                onComplete()
            }
        }
        object : Retryable(ui, task, process) {
            init {
                set(label(size), process(container!!))
            }
        }
    }

    private fun acceptButtonFooter(stringBuilder: StringBuilder, fn: () -> Unit): String {
        val footerTask = ui.newTask(false)
        lateinit var textHandle: StringBuilder
        textHandle = footerTask.complete(ui.hrefLink("Accept", classname = "href-link cmd-button") {
            try {
                textHandle.set("""<div class="cmd-button">Accepted</div>""")
                footerTask.complete()
            } catch (e: Throwable) {
                log.warn("Error", e)
            }
            fn()
        })!!
        return footerTask.placeholder
    }

    private fun inquiry(
        subTask: Task,
        userMessage: String,
        highLevelPlan: ParsedResponse<TaskBreakdownResult>,
        priorCode: String,
        inputFileCode: () -> String,
        genState: GenState,
        taskId: String,
        task: SessionTask,
        taskTabs: TabbedDisplay
    ) {
        val input1 = "Expand ${subTask.description ?: ""}"
        val toInput = { it: String ->
            listOf(
                userMessage,
                highLevelPlan.text,
                priorCode,
                inputFileCode(),
                it,
            ).filter { it.isNotBlank() }
        }
        val inquiryResult = Acceptable(
            task = task,
            userMessage = "Expand ${subTask.description ?: ""}\n${toJson(subTask)}",
            heading = "",
            initialResponse = { it: String -> inquiryActor.answer(toInput(it), api = api) },
            outputFn = { design: String ->
                renderMarkdown(design, ui = ui)
            },
            ui = ui,
            reviseResponse = { userMessages: List<Pair<String, Role>> ->
                inquiryActor.respond(
                    messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                        .toTypedArray<ApiModel.ChatMessage>()),
                    input = toInput("Expand ${subTask.description ?: ""}\n${toJson(subTask)}"),
                    api = api
                )
            },
            atomicRef = AtomicReference(),
            semaphore = Semaphore(0),
        ).call()
        genState.taskResult[taskId] = inquiryResult
    }

    private fun taskPlanning(
        subTask: Task,
        userMessage: String,
        highLevelPlan: ParsedResponse<TaskBreakdownResult>,
        priorCode: String,
        inputFileCode: () -> String,
        genState: GenState,
        taskId: String,
        task: SessionTask,
        taskTabs: TabbedDisplay
    ) {
        val toInput = { it: String ->
            listOf(
                userMessage,
                highLevelPlan.text,
                priorCode,
                inputFileCode(),
                it
            ).filter { it.isNotBlank() }
        }
        val input1 = "Expand ${subTask.description ?: ""}\n${toJson(subTask)}"
        val subPlan = Acceptable(
            task = task,
            userMessage = input1,
            heading = "",
            initialResponse = { it: String -> taskBreakdownActor.answer(toInput(it), api = api) },
            outputFn = { design: ParsedResponse<TaskBreakdownResult> ->
                displayMapInTabs(
                    mapOf(
                        "Text" to renderMarkdown(design.text, ui = ui),
                        "JSON" to renderMarkdown("```json\n${toJson(design.obj)/*.indent("  ")*/}\n```", ui = ui),
                    )
                )
            },
            ui = ui,
            reviseResponse = { userMessages: List<Pair<String, Role>> ->
                taskBreakdownActor.respond(
                    messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                        .toTypedArray<ApiModel.ChatMessage>()),
                    input = toInput(input1),
                    api = api
                )
            },
        ).call()
        genState.taskResult[taskId] = subPlan.text
        var newTasks = subPlan.obj.tasksByID
        newTasks?.forEach {
            val newTask = ui.newTask(false)
            genState.uitaskMap[it.key] = newTask
            genState.tasksByDescription[it.value.description] = it.value
            taskTabs[it.value.description ?: it.key] = newTask.placeholder
        }
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
        log.debug("New Tasks: ${newTasks?.keys}")
        genState.subTasks.putAll(newTasks ?: emptyMap())
        executionOrder(newTasks ?: emptyMap()).reversed().forEach { genState.taskIdProcessingQueue.add(0, it) }
        genState.subTasks.values.forEach {
            it.task_dependencies = it.task_dependencies?.map { dep ->
                when {
                    dep == taskId -> subPlan.obj.finalTaskID ?: dep
                    else -> dep
                }
            }
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
        subTasks.forEach { (taskId, task) ->
            val sanitizedTaskId = sanitizeForMermaid(taskId)
            val taskType = task.taskType?.name ?: "Unknown"
            val escapedDescription = escapeMermaidCharacters(task.description ?: "")
            graphBuilder.append("    ${sanitizedTaskId}[$escapedDescription]:::$taskType;\n")
            task.task_dependencies?.forEach { dependency ->
                val sanitizedDependency = sanitizeForMermaid(dependency)
                graphBuilder.append("    ${sanitizedDependency} --> ${sanitizedTaskId};\n")
            }
        }
        graphBuilder.append("    classDef default fill:#f9f9f9,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef NewFile fill:lightblue,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef EditFile fill:lightgreen,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef Documentation fill:lightyellow,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef Inquiry fill:orange,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef TaskPlanning fill:lightgrey,stroke:#333,stroke-width:2px;\n")
        return graphBuilder.toString()
    }

    private fun sanitizeForMermaid(input: String) = input
        .replace(" ", "_")
        .replace("\"", "\\\"")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .let { "`$it`" }

    private fun escapeMermaidCharacters(input: String) = input
        .replace("\"", "\\\"")
        .let { '"' + it + '"' }

    companion object {
        private val log = LoggerFactory.getLogger(TaskRunnerAgent::class.java)

        enum class ActorTypes {
            TaskBreakdown,
            DocumentationGenerator,
            NewFileCreator,
            FilePatcher,
            Inquiry,
            RunShellCommand,
        }

        fun executionOrder(tasks: Map<String, Task>): List<String> {
            val taskIds: MutableList<String> = mutableListOf()
            val taskMap = tasks.toMutableMap()
            while (taskMap.isNotEmpty()) {
                val nextTasks =
                    taskMap.filter { (_, task) -> task.task_dependencies?.all { taskIds.contains(it) } ?: true }
                if (nextTasks.isEmpty()) {
                    throw RuntimeException("Circular dependency detected in task breakdown")
                }
                taskIds.addAll(nextTasks.keys)
                nextTasks.keys.forEach { taskMap.remove(it) }
            }
            return taskIds
        }

        val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")
    }
}