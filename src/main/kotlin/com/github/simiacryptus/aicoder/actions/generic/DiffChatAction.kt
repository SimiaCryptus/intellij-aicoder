package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.ApplicationEvents
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.dev.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.CodeChatSocketManager
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.SimpleDiffUtil
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
    val selectionStart : Int
    val selectionEnd : Int
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
      model = AppSettingsState.instance.defaultChatModel(),
      storage = ApplicationServices.dataStorageFactory(root)
    ) {
      override val systemPrompt: String
        get() = super.systemPrompt + """
          Provide code patches in diff format within ```diff code blocks.
          The diff format should use + for line additions, - for line deletions.
          The diff should include 2 lines of context before and after every change.
        """.trimIndent()

      val diffPattern = """(?s)```diff(.*?)```""".toRegex()
      var fullPatch = mutableListOf<String>()
      override fun renderResponse(response: String): String {
        val matches = diffPattern.findAll(response).distinct()
        val withLinks = matches.fold(response) { markdown, diffBlock ->
          val diffVal = diffBlock.value
          val hrefLink = hrefLink("Apply Diff") {
            try {
              if(fullPatch.contains(diffVal)) return@hrefLink
              fullPatch.add(diffVal)
              val newCode = fullPatch.fold(rawText) {
                lines, patch -> SimpleDiffUtil.patch(lines, patch)
              }
              WriteCommandAction.runWriteCommandAction(e.project) {
                document.replaceString(selectionStart, selectionEnd, newCode)
              }
              send(divInitializer(cancelable = false) + """<div class="user-message">Diff Applied</div>""")
            } catch (e: Throwable) {
              log.warn("Error applying diff", e)
              send(divInitializer(cancelable = false) + """<div class="error">${e.message}</div>""")
            }
          }
          markdown.replace(diffVal, diffVal + "\n" + hrefLink)
        }
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
