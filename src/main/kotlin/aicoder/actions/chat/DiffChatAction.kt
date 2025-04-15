package aicoder.actions.chat

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.CodeChatSocketManager
import com.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.AddApplyDiffLinks.Companion.addApplyDiffLinks
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import com.intellij.openapi.application.ApplicationManager as IntellijAppManager

class DiffChatAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  val path = "/diffChat"
  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    val editor = event.getData(CommonDataKeys.EDITOR) ?: return false
    val document = editor.document
    return FileDocumentManager.getInstance().getFile(document) != null
  }


  override fun handle(e: AnActionEvent) {
    try {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: return
      val session = Session.newGlobalID()
      val language = ComputerLanguage.getComputerLanguage(e)?.name ?: ""
      val document = editor.document
      val filename = FileDocumentManager.getInstance().getFile(document)?.name ?: return
      val (rawText, selectionStart, selectionEnd) = getSelectionDetails(editor)
      UITools.runAsync(e.project, "Initializing Chat", true) { progress ->
        progress.isIndeterminate = true
        progress.text = "Setting up chat session..."
        setupApplicationServer(session)
        setupSessionProxy(session, language, rawText, filename, editor, selectionStart, selectionEnd, document)
        openBrowserWindow(e, session)
      }
    } catch (ex: Throwable) {
      log.error("Error in DiffChat action", ex)
      UITools.showErrorDialog("Failed to initialize chat: ${ex.message}", "Error")
    }
  }

  private fun getSelectionDetails(editor: Editor): Triple<String, Int, Int> {
    val primaryCaret = editor.caretModel.primaryCaret
    val selectedText = primaryCaret.selectedText
    return if (selectedText != null) {
      Triple(
        selectedText.toString(),
        primaryCaret.selectionStart,
        primaryCaret.selectionEnd
      )
    } else {
      Triple(
        editor.document.text,
        0,
        editor.document.text.length
      )
    }
  }

  private fun setupApplicationServer(session: Session) {
    ApplicationServer.appInfoMap[session] = AppInfoData(
      applicationName = "Code Chat",
      singleInput = false,
      stickyInput = true,
      loadImages = false,
      showMenubar = false
    )
  }

  private fun setupSessionProxy(
    session: Session,
    language: String,
    rawText: String,
    filename: String,
    editor: Editor,
    selectionStart: Int,
    selectionEnd: Int,
    document: Document
  ) {
    var selectionEnd = selectionEnd

    SessionProxyServer.metadataStorage.setSessionName(
      null,
      session,
      "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
    )
    SessionProxyServer.agents[session] = object : CodeChatSocketManager(
      session = session,
      language = language,
      codeSelection = rawText,
      filename = filename,
      api = api,
      model = AppSettingsState.instance.smartModel.chatModel(),
      parsingModel = AppSettingsState.instance.fastModel.chatModel(),
      storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome)
    ) {

        // ... rest of the implementation
        override val systemPrompt: String
            @Language("Markdown")
            get() = super.systemPrompt + """
                  Please provide code modifications in the following diff format within triple-backtick diff code blocks. Each diff block should be preceded by a header that identifies the file being modified.
                  
                  The diff format rules are as follows:
                  - Use '-' at the beginning of a line to indicate a deletion.
                  - Use '+' at the beginning of a line to indicate an addition.
                  - Include 2 lines of context before and after every change to help identify the location of the change.
                  - If a line is part of the original code and hasn't been modified, simply include it without '+' or '-'.
                  - Lines starting with "@@" or "---" or "+++" are treated as headers and are ignored.
                  
                  
                """.trimIndent() + patchFormatPrompt
      override fun renderResponse(response: String, task: SessionTask): String = """<div>${
        renderMarkdown(
          addApplyDiffLinks(
            this,
            code = {
              editor.document.getText(TextRange(selectionStart, selectionEnd))
            },
            response = response,
            handle = { newCode: String ->
              WriteCommandAction.runWriteCommandAction(editor.project) {
                selectionEnd = selectionStart + newCode.length
                document.replaceString(selectionStart, selectionStart + rawText.length, newCode)
              }
            },
            task = task,
            ui = ui
          )
        )
      }</div>"""
    }
  }


  private fun openBrowserWindow(e: AnActionEvent, session: Session) {
    IntellijAppManager.getApplication().executeOnPooledThread {
      val server = AppServer.getServer(e.project)
      val uri = server.server.uri.resolve("/#$session")
      BaseAction.log.info("Opening browser to $uri")
      browse(uri)
    }
  }


  companion object {
    private val log = LoggerFactory.getLogger(DiffChatAction::class.java)
  }
}
