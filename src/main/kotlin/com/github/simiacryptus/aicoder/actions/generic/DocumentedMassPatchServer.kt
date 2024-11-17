package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class DocumentedMassPatchServer(
    val config: DocumentedMassPatchAction.Settings,
    val api: ChatClient,
    val autoApply: Boolean
) : ApplicationServer(
    applicationName = "Documented Code Patch",
    path = "/patchChat",
    showMenubar = false,
) {
    private lateinit var _root: Path

    override val singleInput = false
    override val stickyInput = true

    private val mainActor: SimpleActor
        get() {
            return SimpleActor(
                prompt = """
 You are a helpful AI that helps people with coding.
 
 You will be reviewing code files based on documentation files and suggesting improvements.
 Please analyze both the documentation and code to ensure they are aligned and suggest improvements.
 
 Response should use one or more code patches in diff format within ```diff code blocks.
 Each diff should be preceded by a header that identifies the file being modified.
 The diff format should use + for line additions, - for line deletions.
 The diff should include 2 lines of context before and after every change.
 """.trimIndent(),
                model = AppSettingsState.instance.smartModel.chatModel(),
                temperature = AppSettingsState.instance.temperature,
            )
        }

    override fun newSession(user: User?, session: Session): SocketManager {
        val socketManager = super.newSession(user, session)
        val ui = (socketManager as ApplicationSocketManager).applicationInterface
        _root = config.project?.basePath?.let { Path.of(it) } ?: Path.of(".")
        val task = ui.newTask(true)
        val api = (api as ChatClient).getChildClient().apply {
            val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
            createFile.second?.apply {
                logStreams += this.outputStream().buffered()
                task.verbose("API log: <a href=\"file:///$this\">$this</a>")
            }
        }

        val tabs = TabbedDisplay(task)
        val userMessage = config.settings?.transformationMessage ?: "Review and update code according to documentation"

        // Process documentation files first
        val docSummary = config.settings?.documentationFiles?.joinToString("\n\n") { path ->
            """
             # Documentation: $path
             ```md
             ${_root.resolve(path).toFile().readText(Charsets.UTF_8)}
             ```
             """.trimIndent()
        } ?: ""

        // Then process code files
        config.settings?.codeFiles?.forEach { path ->
            socketManager.scheduledThreadPoolExecutor.schedule({
                socketManager.pool.submit {
                    try {
                        val codeSummary = """
                             $docSummary
                             
                             # Code: $path
                             ```${path.toString().split('.').lastOrNull()}
                             ${_root.resolve(path).toFile().readText(Charsets.UTF_8)}
                             ```
                         """.trimIndent()

                        val fileTask = ui.newTask(false).apply {
                            tabs[path.toString()] = placeholder
                        }

                        val toInput = { it: String -> listOf(codeSummary, it) }
                        Discussable(
                            task = fileTask,
                            userMessage = { userMessage },
                            heading = renderMarkdown(userMessage),
                            initialResponse = {
                                mainActor.answer(toInput(it), api = api)
                            },
                            outputFn = { design: String ->
                                """<div>${
                                    renderMarkdown(design) {
                                        ui.socketManager?.addApplyFileDiffLinks(
                                            root = _root,
                                            response = design,
                                            handle = { newCodeMap ->
                                                newCodeMap.forEach { (path, newCode) ->
                                                    fileTask.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                                                }
                                            },
                                            ui = ui,
                                            api = api as API,
                                            shouldAutoApply = { autoApply },
                                            model = AppSettingsState.instance.fastModel.chatModel(),
                                            defaultFile = _root.resolve(path).toFile().absolutePath
                                        ) ?: design
                                    }
                                }</div>"""
                            },
                            ui = ui,
                            reviseResponse = { userMessages ->
                                mainActor.respond(
                                    messages = userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                                        .toTypedArray(),
                                    input = toInput(userMessage),
                                    api = api
                                )
                            },
                            atomicRef = AtomicReference(),
                            semaphore = Semaphore(0),
                        ).call()
                    } catch (e: Exception) {
                        log.warn("Error processing $path", e)
                        task.error(ui, e)
                    }
                }
            }, 10, java.util.concurrent.TimeUnit.MILLISECONDS)
        }
        return socketManager
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(DocumentedMassPatchServer::class.java)
    }
}