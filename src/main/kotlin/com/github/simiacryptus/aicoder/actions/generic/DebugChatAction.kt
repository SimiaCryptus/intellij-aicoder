package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebuggerManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File

class DebugChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/codeChat"
    val model = AppSettingsState.instance.smartModel.chatModel()

    override fun handle(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        val debugSessionInfo = getDebugSessionInfo(project)
        val systemPrompt = debugSessionInfo
        val userInterfacePrompt = ""
        val session = StorageInterface.newGlobalID()
        agents[session] = ChatSocketManager(
            session = session,
            model = model,
            initialAssistantPrompt = "",
            userInterfacePrompt = userInterfacePrompt,
            systemPrompt = systemPrompt,
            api = api,
            storage = ApplicationServices.dataStorageFactory(root),
            applicationClass = ApplicationServer::class.java,
        )

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

    private fun getDebugSessionInfo(project: Project?): String {
        val debugSession = XDebuggerManager.getInstance(project ?: return "No project found").currentSession
        val stackFrame = debugSession?.currentStackFrame ?: return "No stack frames found"
        val frameContext = stackFrame.sourcePosition?.let {
            val lines = it.file.toFile.readLines()
            val context = ((it.line - 2) until (it.line + 2)).map { line -> lines.getOrNull(line) }
            context.joinToString("\n")
        }
        //val frameVars = debugSession..map { it.name to it.value?.toString() }
        return frameContext.toString()
    }

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(CodeChatAction::class.java)
        private val agents = mutableMapOf<Session, SocketManager>()
        val root: File get() = File(AppSettingsState.instance.pluginHome, "code_chat")
        private fun initApp(server: AppServer, path: String): ChatServer {
            server.appRegistry[path]?.let { return it }
            val socketServer = object : ApplicationServer(
                applicationName = "Code Chat",
                path = path,
                showMenubar = false,
            ) {
                override val singleInput = false
                override val stickyInput = true
                override fun newSession(user: User?, session: Session) =
                    agents[session] ?: throw IllegalArgumentException("Unknown session: $session")
            }
            server.addApp(path, socketServer)
            return socketServer
        }

    }
}