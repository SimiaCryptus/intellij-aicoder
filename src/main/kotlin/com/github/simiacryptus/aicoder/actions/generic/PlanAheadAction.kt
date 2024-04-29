package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.diff.addApplyFileDiffLinks
import com.github.simiacryptus.diff.addSaveLinks
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys.VIRTUAL_FILE_ARRAY
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil.toJson
import com.simiacryptus.skyenet.Discussable
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
import java.awt.GridLayout
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import kotlin.reflect.KClass

class PlanAheadAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    data class PlanAheadSettings(
        var model: String = AppSettingsState.instance.smartModel,
        var temperature: Double = AppSettingsState.instance.temperature,
        var enableTaskPlanning: Boolean = false,
        var enableShellCommands: Boolean = true
    )

    class PlanAheadConfigDialog(
        project: Project?,
        private val settings: PlanAheadSettings
    ) : DialogWrapper(project) {
        private val items = ChatModels.values().toList().toTypedArray()
        private val modelComboBox: ComboBox<String> = ComboBox(items.map { it.first }.toTypedArray())
        // Replace JTextField with JSlider for temperature
        private val temperatureSlider = JSlider(0, 100, (settings.temperature * 100).toInt())

        private val taskPlanningCheckbox = JCheckBox("Enable Task Planning", settings.enableTaskPlanning)
        private val shellCommandsCheckbox = JCheckBox("Enable Shell Commands", settings.enableShellCommands)

        init {
            init()
            title = "Configure Plan Ahead Action"
            // Add change listener to update the settings based on slider value
            temperatureSlider.addChangeListener {
                settings.temperature = temperatureSlider.value / 100.0
            }
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(GridLayout(0, 2))
            panel.add(JLabel("Model:"))
            panel.add(modelComboBox)
            val indexOfFirst = items.indexOfFirst {
                it.second.name == settings.model || it.second.modelName == settings.model || it.first == settings.model
            }
            modelComboBox.selectedIndex = indexOfFirst
            panel.add(JLabel("Temperature:"))
            panel.add(temperatureSlider)
            panel.add(taskPlanningCheckbox)
            panel.add(shellCommandsCheckbox)
            return panel
        }

        override fun doOKAction() {
            if (modelComboBox.selectedItem == null) {
                JOptionPane.showMessageDialog(null, "Model selection cannot be empty", "Error", JOptionPane.ERROR_MESSAGE)
                return
            }
            settings.model = modelComboBox.selectedItem as String
            settings.enableTaskPlanning = taskPlanningCheckbox.isSelected
            settings.enableShellCommands = shellCommandsCheckbox.isSelected
            super.doOKAction()
        }
    }

    val path = "/taskDev"
    override fun handle(e: AnActionEvent) {
        val project = e.project
        val settings = PlanAheadSettings()

        val dialog = PlanAheadConfigDialog(project, settings)
        if (dialog.showAndGet()) {
            // Settings are applied only if the user clicks OK
            val session = StorageInterface.newGlobalID()
            val folder = UITools.getSelectedFolder(e)
            val root = folder?.toFile ?: getModuleRootForFile(UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException(""))

            DataStorage.sessionPaths[session] = root
            PlanAheadApp.agents[session] = PlanAheadApp(event = e, root = root, settings = settings)
            val server = AppServer.getServer(project)
            val app = PlanAheadApp.initApp(server, path)
            app.sessions[session] = app.newSession(null, session)
            openBrowser(server, session.toString())
        }
    }

    private fun openBrowser(server: AppServer, session: String) {
        Thread {
            Thread.sleep(500)
            try {
                Desktop.getDesktop().browse(server.server.uri.resolve("$path/#$session"))
            } catch (e: Throwable) {
                LoggerFactory.getLogger(PlanAheadAction::class.java).warn("Error opening browser", e)
            }
        }.start()
    }
}

class PlanAheadApp(
    applicationName: String = "Task Planning v1.1",
    path: String = "/taskDev",
    val event: AnActionEvent,
    override val root: File,
    val settings: PlanAheadAction.PlanAheadSettings,
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
    override fun <T : Any> initSettings(session: Session): T = Settings(
        model = ChatModels.values().filter { settings.model == it.key || settings.model == it.value.name }.map { it.value }.first(), // Use the model from settings
        temperature = settings.temperature, // Use the temperature from settings
        taskPlanningEnabled = settings.enableTaskPlanning, // Use the task planning flag from settings
        shellCommandTaskEnabled = settings.enableShellCommands // Use the shell command flag from settings
    ) as T

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
            PlanAheadAgent(
                user = user,
                session = session,
                dataStorage = dataStorage,
                api = api,
                ui = ui,
                model = settings?.model ?: AppSettingsState.instance.smartModel.chatModel(),
                parsingModel = settings?.parsingModel ?: AppSettingsState.instance.fastModel.chatModel(),
                temperature = settings?.temperature ?: 0.3,
                event = event,
                workingDir = root.absolutePath,
                root = root.toPath(),
                taskPlanningEnabled = settings?.taskPlanningEnabled ?: false,
                shellCommandTaskEnabled = settings?.shellCommandTaskEnabled ?: true,
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
                applicationName = "Task Planning Agent",
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

        val agents = mutableMapOf<Session, PlanAheadApp>()
        private val log = LoggerFactory.getLogger(PlanAheadApp::class.java)
    }
}

class PlanAheadAgent(
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
    private val env: Map<String, String> = mapOf(),
    val workingDir: String = ".",
    val language: String = if (isWindows) "powershell" else "bash",
    private val command: List<String> = listOf(AppSettingsState.instance.shellCommand),
    private val actorMap: Map<ActorTypes, BaseActor<*, *>> = mapOf(
        ActorTypes.TaskBreakdown to planningActor(
            taskPlanningEnabled,
            shellCommandTaskEnabled,
            model,
            parsingModel,
            temperature
        ),
        ActorTypes.DocumentationGenerator to documentActor(model, temperature),
        ActorTypes.NewFileCreator to createFileActor(model, temperature),
        ActorTypes.FilePatcher to patchActor(model, temperature),
        ActorTypes.Inquiry to inquiryActor(
            taskPlanningEnabled,
            shellCommandTaskEnabled,
            model,
            temperature
        ),
    ) + (if (!shellCommandTaskEnabled) mapOf() else mapOf(
        ActorTypes.RunShellCommand to shellActor(env, workingDir, language, command, model, temperature),
    )),
    val event: AnActionEvent,
    val root: Path
) : ActorSystem<PlanAheadAgent.Companion.ActorTypes>(
    actorMap.map { it.key.name to it.value }.toMap(),
    dataStorage,
    user,
    session
) {
    private val documentationGeneratorActor by lazy { actorMap[ActorTypes.DocumentationGenerator] as SimpleActor }
    private val taskBreakdownActor by lazy { actorMap[ActorTypes.TaskBreakdown] as ParsedActor<TaskBreakdownResult> }
    private val newFileCreatorActor by lazy { actorMap[ActorTypes.NewFileCreator] as SimpleActor }
    private val filePatcherActor by lazy { actorMap[ActorTypes.FilePatcher] as SimpleActor }
    private val inquiryActor by lazy { actorMap[ActorTypes.Inquiry] as SimpleActor }
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

    private val virtualFiles by lazy {
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

    private val codeFiles
        get() = virtualFiles
            .filter { it.exists() && it.isFile }
            .filter { !it.name.startsWith(".") }
            .associate { file -> getKey(file) to getValue(file) }


    private fun getValue(file: VirtualFile) = try {
        file.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        log.warn("Error reading file", e)
        ""
    }

    private fun getKey(file: VirtualFile) = root.relativize(file.toNioPath())

    fun startProcess(userMessage: String) {
        val codeFiles = codeFiles
        val eventStatus = if (!codeFiles.all { it.key.toFile().isFile } || codeFiles.size > 2) """
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
              |${(codeFiles[path] ?: "").let { "```\n${it/*.indent("  ")*/}\n```" }}
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
        val highLevelPlan = Discussable(
            task = task,
            heading = renderMarkdown(userMessage, ui = ui),
            userMessage = { userMessage },
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

        initPlan(highLevelPlan, userMessage, task)
    }

    private fun initPlan(
        plan: ParsedResponse<TaskBreakdownResult>,
        userMessage: String,
        task: SessionTask
    ) {
        try {
            val tasksByID =
                plan.obj.tasksByID?.entries?.toTypedArray()?.associate { it.key to it.value } ?: mapOf()
            val pool: ThreadPoolExecutor = clientManager.getPool(session, user, dataStorage)
            val genState = GenState(tasksByID.toMutableMap())
            val diagramTask = ui.newTask(false).apply { task.add(placeholder) }
            val diagramBuffer =
                diagramTask.add(
                    renderMarkdown(
                        "## Task Dependency Graph\n```mermaid\n${buildMermaidGraph(genState.subTasks)}\n```",
                        ui = ui
                    )
                )
            val taskTabs = object : TabbedDisplay(ui.newTask(false).apply { task.add(placeholder) }) {
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
                        plan = plan,
                        genState = genState,
                        task = genState.uitaskMap.get(taskId) ?: ui.newTask(false).apply {
                            taskTabs[taskId] = placeholder
                        },
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
        val subTasks: Map<String, Task>,
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
        plan: ParsedResponse<TaskBreakdownResult>,
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
                    |${codeFiles[File(it).toPath()] ?: root.resolve(it).toFile().readText()}
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
                        highLevelPlan = plan,
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
                        highLevelPlan = plan,
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
                        highLevelPlan = plan,
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
                        highLevelPlan = plan,
                        priorCode = priorCode,
                        inputFileCode = ::inputFileCode,
                        genState = genState,
                        taskId = taskId,
                        task = task,
                        taskTabs = taskTabs,
                    )
                }

                TaskType.TaskPlanning -> {
                    if (!taskPlanningEnabled) throw RuntimeException("Task planning is disabled")
                    taskPlanning(
                        subTask = subTask,
                        userMessage = userMessage,
                        highLevelPlan = plan,
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
                            highLevelPlan = plan,
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
            mainTask = task,
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
                    val bytes = newCode.toByteArray(Charsets.UTF_8)
                    val saveFile = task.saveFile(path.toString(), bytes)
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
                set(label(size), process(container))
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
                    code = { codeFiles },
                    response = codeResult,
                    handle = { newCodeMap ->
                        newCodeMap.forEach { (path, newCode) ->
                            task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
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
                set(label(size), process(container))
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
                set(label(size), process(container))
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
        val inquiryResult = Discussable(
            task = task,
            userMessage = { "Expand ${subTask.description ?: ""}\n${toJson(subTask)}" },
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
        val subPlan: ParsedResponse<TaskBreakdownResult> = Discussable(
            task = task,
            userMessage = { input1 },
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
        initPlan(
            plan = subPlan,
            userMessage = userMessage,
            task = task,
        )
    }

    private fun getAllDependencies(subTask: Task, subTasks: Map<String, Task>): List<String> {
        return getAllDependenciesHelper(subTask, subTasks, mutableSetOf())
    }

    private fun getAllDependenciesHelper(
        subTask: Task,
        subTasks: Map<String, Task>,
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
        private val log = LoggerFactory.getLogger(PlanAheadAgent::class.java)

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

private fun documentActor(
    model: ChatModels,
    temperature: Double
) = SimpleActor(
    name = "DocumentationGenerator",
    prompt = """
        Create detailed and clear documentation for the provided code, covering its purpose, functionality, inputs, outputs, and any assumptions or limitations.
        Use a structured and consistent format that facilitates easy understanding and navigation. 
        Include code examples where applicable, and explain the rationale behind key design decisions and algorithm choices.
        Document any known issues or areas for improvement, providing guidance for future developers on how to extend or maintain the code.
      """.trimIndent(),
    model = model,
    temperature = temperature,
)

private fun planningActor(
    taskPlanningEnabled: Boolean,
    shellCommandTaskEnabled: Boolean,
    model: ChatModels,
    parsingModel: ChatModels,
    temperature: Double
): ParsedActor<PlanAheadAgent.TaskBreakdownResult> =
    ParsedActor(
        name = "TaskBreakdown",
        resultClass = PlanAheadAgent.TaskBreakdownResult::class.java,
        prompt = """
        |Given a user request, identify and list smaller, actionable tasks that can be directly implemented in code.
        |Detail files input and output as well as task execution dependencies.
        |Creating directories and initializing source control are out of scope.
        |
        |Tasks can be of the following types: 
        |
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
        |${
            if (!shellCommandTaskEnabled) "" else """
        |* RunShellCommand - Execute shell commands and provide the output
        |  ** Specify the command to be executed, or describe the task to be performed
        |  ** List input files/tasks to be examined when writing the command
        """.trimMargin().trim()
        }
        |${
            if (!taskPlanningEnabled) "" else """
        |* TaskPlanning - High-level planning and organization of tasks - identify smaller, actionable tasks based on the information available at task execution time.
        |  ** Specify the prior tasks and the goal of the task
        """.trimMargin().trim()
        }
      """.trimMargin(),
        model = model,
        parsingModel = parsingModel,
        temperature = temperature,
    )

private fun createFileActor(
    model: ChatModels,
    temperature: Double
) = SimpleActor(
    name = "NewFileCreator",
    prompt = """
        |Generate the necessary code for new files based on the given requirements and context.
        |For each file:
        |- Provide a clear relative file path based on the content and purpose of the file.
        |- Ensure the code is well-structured, follows best practices, and meets the specified functionality.
        |- Carefully consider how the new file fits into the existing project structure and architecture.
        |- Avoid creating files that duplicate functionality or introduce inconsistencies.
        |  
        |The response format should be as follows:
        |- Use triple backticks to create code blocks for each file.
        |- Each code block should be preceded by a header specifying the file path.
        |- The file path should be a relative path from the project root.
        |- Separate code blocks with a single blank line.
        |- Specify the language for syntax highlighting after the opening triple backticks.
        |
        |Example:
        |
        |Here are the new files:
        |
        |### src/utils/exampleUtils.js
        |```js
        |// Utility functions for example feature
        |const b = 2;
        |function exampleFunction() {
        |  return b + 1;
        |}
        |
        |```
        |
        |### tests/exampleUtils.test.js 
        |```js
        |// Unit tests for exampleUtils
        |const assert = require('assert');
        |const { exampleFunction } = require('../src/utils/exampleUtils');
        |
        |describe('exampleFunction', () => {
        |  it('should return 3', () => {
        |    assert.equal(exampleFunction(), 3);
        |  });
        |});
        |```
      """.trimMargin(),
    model = model,
    temperature = temperature,
)

private fun patchActor(
    model: ChatModels,
    temperature: Double
) = SimpleActor(
    name = "FilePatcher",
    prompt = """
        |Generate a patch for an existing file to modify its functionality or fix issues based on the given requirements and context. 
        |Ensure the modifications are efficient, maintain readability, and adhere to coding standards.
        |Carefully review the existing code and project structure to ensure the changes are consistent and do not introduce bugs.
        |Consider the impact of the modifications on other parts of the codebase.
        |
        |Provide a summary of the changes made.
        |  
        |Response should use one or more code patches in diff format within ```diff code blocks.
        |Each diff should be preceded by a header that identifies the file being modified.
        |The diff format should use + for line additions, - for line deletions.
        |The diff should include 2 lines of context before and after every change.
        |
        |Example:
        |
        |Here are the patches:
        |
        |### src/utils/exampleUtils.js
        |```diff
        | // Utility functions for example feature
        | const b = 2;
        | function exampleFunction() {
        |-   return b + 1;
        |+   return b + 2;
        | }
        |```
        |
        |### tests/exampleUtils.test.js
        |```diff
        | // Unit tests for exampleUtils
        | const assert = require('assert');
        | const { exampleFunction } = require('../src/utils/exampleUtils');
        | 
        | describe('exampleFunction', () => {
        |-   it('should return 3', () => {
        |+   it('should return 4', () => {
        |     assert.equal(exampleFunction(), 3);
        |   });
        | });
        |```
      """.trimMargin(),
    model = model,
    temperature = temperature,
)

private fun inquiryActor(
    taskPlanningEnabled: Boolean,
    shellCommandTaskEnabled: Boolean,
    model: ChatModels,
    temperature: Double
) = SimpleActor(
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
)

private fun shellActor(
    env: Map<String, String>,
    workingDir: String,
    language: String,
    command: List<String>,
    model: ChatModels,
    temperature: Double
) = CodingActor(
    name = "RunShellCommand",
    interpreterClass = ProcessInterpreter::class,
    details = """
        Execute the following shell command(s) and provide the output. Ensure to handle any errors or exceptions gracefully.
    
        Note: This task is for running simple and safe commands. Avoid executing commands that can cause harm to the system or compromise security.
      """.trimIndent(),
    symbols = mapOf(
        "env" to env,
        "workingDir" to File(workingDir).absolutePath,
        "language" to language,
        "command" to command,
    ),
    model = model,
    temperature = temperature,
)