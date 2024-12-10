package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.MultiStepPatchAction.AutoDevApp.Settings
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.GPT4Tokenizer
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

/**
 * Action that enables multi-file code chat functionality.
 * Allows users to select multiple files and discuss them with an AI assistant.
 * Supports code modifications through patch application.
 *
 * @see BaseAction
 */

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
 # $path
 ```$extension
 ${code}
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

        try {
            UITools.runAsync(event.project, "Initializing Chat", true) { progress ->
                progress.isIndeterminate = true
                progress.text = "Setting up chat session..."
                val session = Session.newGlobalID()
                SessionProxyServer.metadataStorage.setSessionName(null, session, "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}")
                SessionProxyServer.chats[session] = PatchApp(root.toFile(), { codeSummary() }, codeFiles)
                ApplicationServer.appInfoMap[session] = AppInfoData(
                    applicationName = "Code Chat",
                    singleInput = false,
                    stickyInput = true,
                    loadImages = false,
                    showMenubar = false
                )
                val server = AppServer.getServer(event.project)
                launchBrowser(server, session.toString())
            }
        } catch (e: Throwable) {
            UITools.error(log, "Failed to initialize chat session", e)
        }
    }

    private fun launchBrowser(server: AppServer, session: String) {

        Thread {
            Thread.sleep(500)
            try {

                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!super.isEnabled(event)) return false
        val files = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)
        return files != null && files.isNotEmpty()
    }

    /** Application class that handles the chat interface and code modifications */
    private inner class PatchApp(
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
                model = AppSettingsState.instance.smartModel.chatModel()
            )

        /**
         * Handles user messages in the chat interface
         *
         * @throws RuntimeException if API calls fail
         * @throws IOException if file operations fail
         */

        override fun userMessage(
            session: Session,
            user: User?,
            userMessage: String,
            ui: ApplicationInterface,
            api: API
        ) {
            val settings = getSettings(session, user) ?: Settings()
            if (api is ChatClient) api.budget = settings.budget ?: 2.00

            val task = ui.newTask()
            val codex = GPT4Tokenizer()

            val api = (api as ChatClient).getChildClient().apply {
                val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
                createFile.second?.apply {
                    logStreams += this.outputStream().buffered()
                    task.verbose("API log: <a href=\"file:///$this\">$this</a>")
                }
            }
            task.echo(renderMarkdown(userMessage))
            task.verbose(renderMarkdown(codeFiles.joinToString("\n") { path ->
                "* $path - ${codex.estimateTokenCount(root.resolve(path.toFile()).readText())} tokens"
            }))
            val toInput = { it: String -> listOf(codeSummary(), it) }
            Retryable(
                ui = ui,
                task = task,
                process = { content ->
                    val design = mainActor.answer(toInput(userMessage), api = api)
                    """
                        |<div>${
                        renderMarkdown(design) {
                            ui.socketManager?.addApplyFileDiffLinks(
                                root = root.toPath(),
                                response = it,
                                handle = { newCodeMap ->
                                    newCodeMap.forEach { (path, newCode) ->
                                        content.append("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                                    }
                                },
                                ui = ui,
                                api = api,
                            )!!
                        }
                    }</div>
                    """.trimMargin()

                },
            )
        }
    }

    /**
     * Recursively collects files from the selected virtual files
     *
     * @param virtualFiles Array of selected virtual files
     * @param root Project root path
     * @return Set of relative paths to the selected files
     */


    private fun getFiles(
        virtualFiles: Array<out VirtualFile>?,
        root: Path
    ): Set<Path> {
        val codeFiles = mutableSetOf<Path>()
        virtualFiles?.forEach { file ->
            if (file.isDirectory && !file.name.startsWith(".")) {
                getFiles(file.children, root)
            } else {
                codeFiles.add(root.relativize(file.toNioPath()))
            }
        }
        return codeFiles
    }


    companion object {
        private val log = LoggerFactory.getLogger(MultiDiffChatAction::class.java)

    }
}