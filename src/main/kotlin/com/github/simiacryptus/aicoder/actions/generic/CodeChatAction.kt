package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.ApplicationEvents
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.dev.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.CodeChatSocketManager
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.session.SocketManager
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File

class CodeChatAction : BaseAction() {

  val path = "/codeChat"

  override fun handle(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return

    val session = StorageInterface.newGlobalID()
    val language = ComputerLanguage.getComputerLanguage(e)?.name ?: return
    val filename = FileDocumentManager.getInstance().getFile(editor.document)?.name ?: return
    agents[session] = CodeChatSocketManager(
      session = session,
      language = language,
      codeSelection = editor.caretModel.primaryCaret.selectedText ?: editor.document.text,
      filename = filename,
      api = api,
      model = AppSettingsState.instance.defaultChatModel(),
      storage = ApplicationServices.dataStorageFactory(root)
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

  override fun isEnabled(event: AnActionEvent) = true

  companion object {
    private val log = LoggerFactory.getLogger(CodeChatAction::class.java)
    private val agents = mutableMapOf<Session, SocketManager>()
    val root: File get() = File(ApplicationEvents.pluginHome, "code_chat")
    private fun initApp(server: AppServer, path: String): ChatServer {
      server.appRegistry[path]?.let { return it }
      val socketServer = object : ApplicationServer("Code Chat") {
        override val singleInput = false
        override fun newSession(user: User?, session: Session) = agents[session]!!
      }
      server.addApp(path, socketServer)
      return socketServer
    }

  }
}
