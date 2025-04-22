package aicoder.actions.dev

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

class LineFilterChatAction : BaseAction() {
  private lateinit var lines: List<String>

  data class ChatConfig(
    val language: String,
    val filename: String,
    val code: String,
    val lines: Array<String>
  )

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  val path = "/codeChat"

  override fun handle(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val session = Session.newGlobalID()
    val config = getConfig(e, editor) ?: run {
      UITools.error(log, "Error", Exception("Could not get required configuration"))
      return
    }
    try {
      setupChatManager(session, config)
      openBrowser(e.project, session)
    } catch (ex: Throwable) {
      log.error("Error setting up chat", ex)
      UITools.error(log, "Error", ex)
    }
  }

  private fun getConfig(e: AnActionEvent, editor: Editor): ChatConfig? {
    return try {
      val language = ComputerLanguage.getComputerLanguage(e)?.name ?: return null
      val filename = FileDocumentManager.getInstance().getFile(editor.document)?.name ?: return null
      val code = editor.caretModel.primaryCaret.selectedText ?: editor.document.text
      val lines = code.split("\n").toTypedArray()
      ChatConfig(language, filename, code, lines)
    } catch (e: Exception) {
      log.error("Error getting configuration", e)
      null
    }
  }

  private fun setupChatManager(session: Session, config: ChatConfig) {
    val codelines = config.lines.withIndex().joinToString("\n") { (i, line) ->
      "${i.toString().padStart(3, '0')} $line"
    }
    val userPrompt = buildString {
      append("# `${config.filename}`\n\n")
      append("```${config.language}\n")
      append(config.code)
      append("\n```")
    }
    val systemPrompt = buildString {
      append("You are a helpful AI that helps people with coding.\n\n")
      append("You will be answering questions about the following code located in `${config.filename}`:\n\n")
      append("```${config.language}\n")
      append(codelines)
      append("\n```\n\n")
      append("Responses may use markdown formatting. Lines from the prompt can be included ")
      append("by using the line number in a response line (e.g. `\\nLINE\\n`).\n\n")
      append("For example:\n\n")
      append("```text\n")
      append("001\n## Injected subtitle\n\n025\n026\n\n013\n014\n")
      append("```")
    }
    SessionProxyServer.metadataStorage.setSessionName(
      null,
      session,
      "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
    )
    SessionProxyServer.agents[session] = object : ChatSocketManager(
      session = session,
      model = AppSettingsState.instance.smartModel.chatModel(),
      parsingModel = AppSettingsState.instance.fastModel.chatModel(),
      systemPrompt = systemPrompt,
      api = api,
      applicationClass = ApplicationServer::class.java,
      storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome),
    ) {
      override fun canWrite(user: User?): Boolean = true
      override fun renderResponse(response: String, task: SessionTask): String {
        return com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown(response.split("\n").joinToString("\n") {
          when {
            // Is numeric, use line if in range
            it.toIntOrNull()?.let { i -> lines.indices.contains(i) } == true -> lines[it.toInt()]
            // Otherwise, use response
            else -> it
          }
        }
        )
      }
    }
  }

  private fun openBrowser(project: Project?, session: Session) {

    val server = AppServer.getServer(project)

    ApplicationManager.getApplication().executeOnPooledThread {
      Thread.sleep(500)
      try {
        val uri = server.server.uri.resolve("/#$session")
        BaseAction.log.info("Opening browser to $uri")
        browse(uri)
      } catch (ex: Throwable) {
        log.warn("Error opening browser", ex)
      }
    }
  }

  override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.devActions

  companion object {
    private val log = LoggerFactory.getLogger(LineFilterChatAction::class.java)

  }
}