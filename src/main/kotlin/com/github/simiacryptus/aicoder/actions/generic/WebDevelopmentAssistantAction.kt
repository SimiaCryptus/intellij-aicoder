package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.diff.addApplyFileDiffLinks
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.*
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.servlet.ToolServlet
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

val VirtualFile.toFile: File get() = File(this.path)

class WebDevelopmentAssistantAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/webDev"

    override fun handle(e: AnActionEvent) {
        val session = StorageInterface.newGlobalID()
        val storage = ApplicationServices.dataStorageFactory(DiffChatAction.root) as DataStorage?
        val selectedFile = UITools.getSelectedFolder(e)
        if (null != storage && null != selectedFile) {
            DataStorage.sessionPaths[session] = selectedFile.toFile
        }
        agents[session] = WebDevApp(root=selectedFile)
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

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.getSelectedFile(event)?.isDirectory == false) return false
        return super.isEnabled(event)
    }

    open class WebDevApp(
        applicationName: String = "Web Development Agent",
        val temperature: Double = 0.1,
        root: VirtualFile?,
    ) : ApplicationServer(
        applicationName = applicationName,
        path = "/webdev",
        showMenubar = false,
        root = root?.toFile!!,
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
            WebDevAgent(
                api = api,
                dataStorage = dataStorage,
                session = session,
                user = user,
                ui = ui,
                tools = settings.tools,
                model = settings.model,
                root = root,
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

    class WebDevAgent(
        val api: API,
        dataStorage: StorageInterface,
        session: Session,
        user: User?,
        val ui: ApplicationInterface,
        val model: ChatModels,
        val tools: List<String> = emptyList(),
        private val actorMap: Map<ActorTypes, BaseActor<*, *>> = mapOf(
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
            ActorTypes.ArchitectureDiscussionActor to ParsedActor(
                resultClass = PageResourceList::class.java,
                prompt = """
              Translate the user's idea into a detailed architecture for a simple web application. 
              Suggest specific frameworks/libraries to import and provide CDN links for them.
              Specify user interactions and how the application will respond to them.
              Identify key HTML classes and element IDs that will be used to bind the application to the HTML.
              Identify coding styles and patterns to be used.
              List all files to be created, and for each file, describe the public interface / purpose / content summary.
            """.trimIndent(),
                model = model,
                parsingModel = model,
            ),
            ActorTypes.CodeReviewer to SimpleActor(
                prompt = """
              |Analyze the code summarized in the user's header-labeled code blocks.
              |Review, look for bugs, and provide fixes. 
              |Provide implementations for missing functions.
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
            ),
            ActorTypes.EtcCodingActor to SimpleActor(
                prompt = """
              You will translate the user request into a file for use in a web application.
            """.trimIndent(),
                model = model
            ),
        ),
        val root: File,
    ) :
        ActorSystem<WebDevAgent.ActorTypes>(
            actorMap.map { it.key.name to it.value }.toMap(),
            dataStorage,
            user,
            session
        ) {
        enum class ActorTypes {
            HtmlCodingActor,
            JavascriptCodingActor,
            CssCodingActor,
            ArchitectureDiscussionActor,
            CodeReviewer,
            EtcCodingActor,
        }

        private val architectureDiscussionActor by lazy { getActor(ActorTypes.ArchitectureDiscussionActor) as ParsedActor<PageResourceList> }
        private val htmlActor by lazy { getActor(ActorTypes.HtmlCodingActor) as SimpleActor }
        private val javascriptActor by lazy { getActor(ActorTypes.JavascriptCodingActor) as SimpleActor }
        private val cssActor by lazy { getActor(ActorTypes.CssCodingActor) as SimpleActor }
        private val codeReviewer by lazy { getActor(ActorTypes.CodeReviewer) as SimpleActor }
        private val etcActor by lazy { getActor(ActorTypes.EtcCodingActor) as SimpleActor }

        private val codeFiles = mutableMapOf<Path, String>()

        fun start(
            userMessage: String,
        ) {
            val task = ui.newTask()
            val toInput = { it: String -> listOf(it) }
            val architectureResponse = Discussable(
                task = task,
                userMessage = userMessage,
                initialResponse = { it: String -> architectureDiscussionActor.answer(toInput(it), api = api) },
                outputFn = { design: ParsedResponse<PageResourceList> ->
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
                heading = userMessage
            ).call()

            fun outputFn(
                task: SessionTask,
                design: String,
            ) = renderMarkdown(
                ui.socketManager.addApplyFileDiffLinks(
                    root = root.toPath(),
                    code = { codeFiles },
                    response = design,
                    handle = { newCodeMap ->
                        newCodeMap.forEach { (path, newCode) ->
                            task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                        }
                    },
                    ui = ui
                )
            )

            try {
                val toolSpecs = tools.map { ToolServlet.tools.find { t -> t.path == it } }
                    .joinToString("\n\n") { it?.let { JsonUtil.toJson(it.openApiDescription) } ?: "" }
                var messageWithTools = userMessage
                if (toolSpecs.isNotBlank()) messageWithTools += "\n\nThese services are available:\n$toolSpecs"
                task.echo(
                    renderMarkdown(
                        "```json\n${JsonUtil.toJson(architectureResponse.obj)/*.indent("  ")*/}\n```",
                        ui = ui
                    )
                )
                val fileTabs = TabbedDisplay(task)
                architectureResponse.obj.resources.filter {
                    !it.path!!.startsWith("http")
                }.map { (path, description) ->
                    val task = ui.newTask(false).apply { fileTabs[path.toString()] = placeholder }
                    task.header("Drafting $path")
                    codeFiles[File(path).toPath()] = ""
                    pool.submit {
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
                fun codeSummary() = codeFiles.entries.joinToString("\n\n") { (path, code) ->
                    "# $path\n```${
                        path.toString().split('.').last().let { /*escapeHtml4*/(it)/*.indent("  ")*/ }
                    }\n${code.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}\n```"
                }

                try {
                    var task = ui.newTask(false).apply { task.add(placeholder) }
                    task.header("Code Iteration")
                    task.verbose(message = renderMarkdown(codeSummary(), ui = ui))
                    var design = codeReviewer.answer(listOf(element = codeSummary()), api = api)
                    task.complete(outputFn(task, design))
                    val feedbackGuard = AtomicBoolean(false)
                    var textInputHandle: StringBuilder? = null
                    textInputHandle = task.complete(ui.textInput { userResponse ->
                        if (feedbackGuard.getAndSet(true)) return@textInput
                        textInputHandle?.clear()
                        task = ui.newTask(false).apply { task.complete(placeholder) }
                        task.echo(renderMarkdown(userResponse, ui = ui))
                        Retryable(ui, task) {
                            val codeSummary = codeSummary()
                            design = codeReviewer.respond(
                                messages = codeReviewer.chatMessages(
                                    listOf(
                                        codeSummary,
                                        userResponse,
                                    )
                                ),
                                input = listOf(element = codeSummary),
                                api = api
                            )
                            outputFn(task, design)
                        }
                        feedbackGuard.set(false)
                    })
                } catch (e: Throwable) {
                    //val task = ui.newTask(false).apply { task.add(placeholder) }
                    task.error(ui = ui, e = e)
                    throw e
                }
            } catch (e: Throwable) {
                log.warn("Error", e)
                task.error(ui, e)
            }
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
                    userMessage = "Drafting $path",
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
                            input = listOf(element = (request.toList() + userMessages.map {
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
                codeFiles[path] = code
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
        private val agents = mutableMapOf<Session, WebDevApp>()
        val root: File get() = File(AppSettingsState.instance.pluginHome, "code_chat")
        private fun initApp(server: AppServer, path: String): ChatServer {
            server.appRegistry[path]?.let { return it }
            val socketServer = object : ApplicationServer(
                applicationName = "Web Development Agent",
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

        data class PageResourceList(
            @Description("List of resources in this project; don't forget the index.html file!")
            val resources: List<PageResource> = emptyList()
        ) : ValidatedObject {
            override fun validate(): String? = when {
                resources.isEmpty() -> "Resources are required"
                resources.any { it.validate() != null } -> "Invalid resource"
                else -> null
            }
        }

        data class PageResource(
            val path: String? = "",
            val description: String? = ""
        ) : ValidatedObject {
            override fun validate(): String? = when {
                path.isNullOrBlank() -> "Path is required"
                path.contains(" ") -> "Path cannot contain spaces"
                !path.contains(".") -> "Path must contain a file extension"
                else -> null
            }
        }

    }

}
