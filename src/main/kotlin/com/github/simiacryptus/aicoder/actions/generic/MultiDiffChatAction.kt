package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.diff.addApplyFileDiffLinks
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Path

class MultiDiffChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/multiDiffChat"

    override fun handle(e: AnActionEvent) {

        val dataContext = e.dataContext
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        val codeFiles = mutableMapOf<Path, String>()
        val folder = UITools.getSelectedFolder(e)
        val root = if (null != folder) {
            folder.toFile.toPath()
        } else {
            getModuleRootForFile(UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException("")).toPath()
        }

        virtualFiles?.forEach { file ->
            val relative = root.relativize(file.toNioPath())
            val path = relative
            codeFiles[path] = file.contentsToByteArray().toString(Charsets.UTF_8)
        }

        fun codeSummary() = codeFiles.entries.joinToString("\n\n") { (path, code) ->
            val extension = path.toString().split('.').lastOrNull()?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }
            """
            |# $path
            |```$extension
            |${code.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
            |```
            """.trimMargin()
        }

        val session = StorageInterface.newGlobalID()
        //DataStorage.sessionPaths[session] = root.toFile()

        val codeSummary = codeSummary()
        agents[session] = object : ChatSocketManager(
            session = session,
            model = AppSettingsState.instance.smartModel.chatModel(),
            userInterfacePrompt = """
                |
                |$codeSummary
                |
                """.trimMargin().trim(),
            systemPrompt = """
                |You are a helpful AI that helps people with coding.
                |
                |You will be answering questions about the following code:
                |
                |$codeSummary
                |
                |Response should use one or more code patches in diff format within ```diff code blocks.
                |Each diff should be preceded by a header that identifies the file being modified.
                |The diff format should use + for line additions, - for line deletions.
                |The diff should include 2 lines of context before and after every change.
                |
                |Example:
                |
                |Explanation text
                |
                |### scripts/filename.js
                |```diff
                |- const b = 2;
                |+ const a = 1;
                |```
                |
                |Continued text
                """.trimMargin(),
            api = api,
            applicationClass = ApplicationServer::class.java,
            storage = ApplicationServices.dataStorageFactory(DiffChatAction.root),
        ) {
            val ui by lazy { ApplicationInterface(this) }
            override fun renderResponse(response: String, task: SessionTask): String {
                val html = addApplyFileDiffLinks(
                    root = root,
                    code = { codeFiles },
                    response = response,
                    handle = { newCodeMap ->
                        newCodeMap.forEach { (path, newCode) ->
                            task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                        }
                    },
                    ui = ui,
                )
                return """<div>${renderMarkdown(html)}</div>"""
            }
        }

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

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(MultiDiffChatAction::class.java)
        private val agents = mutableMapOf<Session, SocketManager>()
        val root: File get() = File(AppSettingsState.instance.pluginHome, "mdiff_chat")
        private fun initApp(server: AppServer, path: String): ChatServer {
            server.appRegistry[path]?.let { return it }
            val socketServer = object : ApplicationServer(
                applicationName = "Multi-file Diff Chat",
                path = path,
                showMenubar = false,
            ) {
                override val singleInput = true
                override val stickyInput = false
                override fun newSession(user: User?, session: Session) = agents[session]!!
            }
            server.addApp(path, socketServer)
            return socketServer
        }

    }
}
