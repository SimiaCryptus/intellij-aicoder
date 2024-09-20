package com.github.simiacryptus.aicoder.actions.generic

import ai.grazie.utils.mpp.UUID
import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil.toJson
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.*
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.util.commonRoot
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class MultiStepPatchAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/autodev"

    override fun handle(e: AnActionEvent) {
        val session = StorageInterface.newGlobalID()
        val storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome) as DataStorage?
        val selectedFile = UITools.getSelectedFolder(e)
        if (null != storage && null != selectedFile) {
            DataStorage.sessionPaths[session] = selectedFile.toFile
        }
        SessionProxyServer.chats[session] = AutoDevApp(event = e)
        val server = AppServer.getServer(e.project)

        Thread {
            Thread.sleep(500)
            try {

                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                Desktop.getDesktop().browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
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
        override fun userMessage(
            session: Session,
            user: User?,
            userMessage: String,
            ui: ApplicationInterface,
            api: API
        ) {
            val settings = getSettings(session, user) ?: Settings()
            if (api is ChatClient) api.budget = settings.budget ?: 2.00
            AutoDevAgent(
                api = api,
                dataStorage = dataStorage,
                session = session,
                user = user,
                ui = ui,
                model = settings.model!!,
                parsingModel = AppSettingsState.instance.defaultFastModel(),
                event = event,
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
        |Implement the changes to the codebase as described in the task list.
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
            val toInput = { it: String -> listOf(codeSummary(), it) }
            val architectureResponse = Discussable(
                task = task,
                userMessage = { userMessage },
                heading = userMessage,
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
                    pool.submit {
                        task.header("Task: $description")
                        Retryable(ui,task) {
                            try {
                                val filter = codeFiles.filter { path ->
                                    paths?.find { path.toString().contains(it) }?.isNotEmpty() == true
                                }
                                require(filter.isNotEmpty()) {
                                    """
                                      |No files found for $paths
                                      |
                                      |Root:
                                      |$root
                                      |
                                      |Files:
                                      |${codeFiles.joinToString("\n")}
                                      |
                                      |Paths:
                                      |${paths?.joinToString("\n") ?: ""}
                                      |
                                    """.trimMargin()
                                }
                                renderMarkdown(ui.socketManager!!.addApplyFileDiffLinks(
                                    root = root,
                                    response = taskActor.answer(listOf(
                                        codeSummary(),
                                        userMessage,
                                        filter.joinToString("\n\n") {
                                            "# ${it}\n```${
                                                it.toString().split('.').last().let { /*escapeHtml4*/it/*.indent("  ")*/ }
                                            }\n${ root.resolve(it).toFile().readText() }\n```"
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
                                ))
                            } catch (e: Exception) {
                                task.error(ui, e)
                                ""
                            }
                        }
                    }
                }.toTypedArray().forEach { it.get() }
            } catch (e : Exception) {
                log.warn("Error",e)
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