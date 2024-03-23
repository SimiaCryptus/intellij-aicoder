package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.ApplicationEvents
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.dev.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.CodeChatSocketManager
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.addApplyDiffLinks
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File

class DiffChatAction : BaseAction() {

  val path = "/diffChat"

  override fun handle(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val session = StorageInterface.newGlobalID()
    val language = ComputerLanguage.getComputerLanguage(e)?.name ?: return
    val document = editor.document
    val filename = FileDocumentManager.getInstance().getFile(document)?.name ?: return
    val primaryCaret = editor.caretModel.primaryCaret
    val rawText: String
    val selectionStart: Int
    val selectionEnd: Int
    val selectedText = primaryCaret.selectedText
    if (null != selectedText) {
      rawText = selectedText
      selectionStart = primaryCaret.selectionStart
      selectionEnd = primaryCaret.selectionEnd
    } else {
      rawText = document.text
      selectionStart = 0
      selectionEnd = rawText.length
    }
    val numberedText = rawText.split("\n")
      .mapIndexed { lineNumber: Int, lineText: String ->
        lineText
//        String.format("%4d: %s", lineNumber + 1, lineText)
      }.joinToString("\n")
    agents[session] = object : CodeChatSocketManager(
      session = session,
      language = language,
      codeSelection = numberedText,
      filename = filename,
      api = api,
      model = AppSettingsState.instance.smartModel.chatModel(),
      storage = ApplicationServices.dataStorageFactory(root)
    ) {
      override val systemPrompt: String
        get() = super.systemPrompt + """
          Please provide code modifications in the following diff format within triple-backtick diff code blocks. Each diff block should be preceded by a header that identifies the file being modified.
          
          The diff format rules are as follows:
          - Use '---' at the beginning of a line to indicate a deletion.
          - Use '+++' at the beginning of a line to indicate an addition.
          - Include 2 lines of context before and after every change to help identify the location of the change.
          - If a line is part of the original code and hasn't been modified, simply include it without '+' or '-'.
          - Lines starting with "@@" are treated as hunk headers and should be skipped.
          
          Example:
          
          ### Path/To/YourFile.ext
          ```diff
          --- This line will be removed.
          +++ This line will be added.
          ```
          
          Note: The diff should accurately reflect the changes to be made to the code, including sufficient context to ensure the modifications can be correctly applied.
        """.trimIndent()

      override fun renderResponse(response: String, task: SessionTask): String {
        val withLinks = addApplyDiffLinks(rawText, response, handle = { newCode: String ->
          WriteCommandAction.runWriteCommandAction(e.project) {
            document.replaceString(selectionStart, selectionEnd, newCode)
          }
        }, task = task)
        val html = renderMarkdown(withLinks)
        return """<div>$html</div>"""
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
    private val log = LoggerFactory.getLogger(DiffChatAction::class.java)
    private val agents = mutableMapOf<Session, SocketManager>()
    val root: File get() = File(ApplicationEvents.pluginHome, "code_chat")
    private fun initApp(server: AppServer, path: String): ChatServer {
      server.appRegistry[path]?.let { return it }
      val socketServer = object : ApplicationServer("Code Chat", path) {
        override val singleInput = false
        override val stickyInput = true
        override fun newSession(user: User?, session: Session) = agents[session]!!
      }
      server.addApp(path, socketServer)
      return socketServer
    }

  }
}