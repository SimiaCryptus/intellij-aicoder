package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.MultiStepPatchAction.AutoDevApp.Settings
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.GPT4Tokenizer
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.ClientManager
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class MultiCodeChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(event: AnActionEvent) {
        var root: Path? = null
        val codeFiles: MutableSet<Path> = mutableSetOf()
        fun codeSummary() = codeFiles.filter {
            root!!.resolve(it).toFile().exists()
        }.associateWith { root!!.resolve(it).toFile().readText(Charsets.UTF_8) }
            .entries.joinToString("\n\n") { (path, code) ->
                val extension = path.toString().split('.').lastOrNull()?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }
                """
            |# $path
            |```$extension
            |${code}
            |```
            """.trimMargin()
            }

        val dataContext = event.dataContext
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        val folder = UITools.getSelectedFolder(event)
        root = if (null != folder) {
            folder.toFile.toPath()
        } else {
            getModuleRootForFile(UITools.getSelectedFile(event)?.parent?.toFile ?: throw RuntimeException("")).toPath()
        }
        val files = getFiles(virtualFiles, root!!)
        codeFiles.addAll(files)

        val session = StorageInterface.newGlobalID()
        SessionProxyServer.chats[session] = PatchApp(root.toFile(), { codeSummary() }, codeFiles)
        val server = AppServer.getServer(event.project)

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

    inner class PatchApp(
        override val root: File,
        val codeSummary: () -> String,
        val codeFiles: Set<Path> = setOf(),
    ) : ApplicationServer(
        applicationName = "Multi-file Patch Chat",
        path = "/patchChat",
        showMenubar = false,
    ) {
        override val singleInput = false
        override val stickyInput = true
        private val mainActor: SimpleActor
            get() = SimpleActor(
                prompt = """
                        |You are a helpful AI that helps people with coding.
                        |
                        |You will be answering questions about the following code:
                        |
                        |${codeSummary()}
                        |
                        """.trimMargin(),
                model = AppSettingsState.instance.defaultSmartModel()
            )

        override fun userMessage(
            session: Session,
            user: User?,
            userMessage: String,
            ui: ApplicationInterface,
            api: API
        ) {
            val settings = getSettings(session, user) ?: Settings()
            if (api is ClientManager.MonitoredClient) api.budget = settings.budget ?: 2.00

            val task = ui.newTask()
            val codex = GPT4Tokenizer()
            task.header(renderMarkdown(codeFiles.joinToString("\n") { path ->
                "* $path - ${codex.estimateTokenCount(root.resolve(path.toFile()).readText())} tokens"
            }))
            val toInput = { it: String -> listOf(codeSummary(), it) }
            Discussable(
                task = task,
                userMessage = { userMessage },
                heading = renderMarkdown(userMessage),
                initialResponse = { it: String -> mainActor.answer(toInput(it), api = api) },
                outputFn = { design: String ->
                    var markdown = ui.socketManager?.addApplyFileDiffLinks(
                        root = root.toPath(),
                        response = design,
                        handle = { newCodeMap ->
                            newCodeMap.forEach { (path, newCode) ->
                                task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                            }
                        },
                        ui = ui,
                        api = api,
                    )
                    """<div>${renderMarkdown(markdown!!)}</div>"""
                },
                ui = ui,
                reviseResponse = { userMessages: List<Pair<String, Role>> ->
                    mainActor.respond(
                        messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                            .toTypedArray<ApiModel.ChatMessage>()),
                        input = toInput(userMessage),
                        api = api
                    )
                },
                atomicRef = AtomicReference(),
                semaphore = Semaphore(0),
            ).call()
        }
    }


    private fun getFiles(
        virtualFiles: Array<out VirtualFile>?,
        root: Path
    ): MutableSet<Path> {
        val codeFiles = mutableSetOf<Path>()
        virtualFiles?.forEach { file ->
            if (file.isDirectory) {
                getFiles(file.children, root)
            } else {
                codeFiles.add(root.relativize(file.toNioPath()))
            }
        }
        return codeFiles
    }

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(MultiDiffChatAction::class.java)

    }
}
