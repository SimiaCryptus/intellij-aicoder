package aicoder.actions.git

import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.CodeChatSocketManager
import com.simiacryptus.aicoder.util.IdeaChatClient
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import java.text.SimpleDateFormat
import javax.swing.JOptionPane

class ChatWithWorkingCopyDiffAction : AnAction() {
  companion object {
    private val log = Logger.getInstance(ChatWithWorkingCopyDiffAction::class.java)
  }

  override fun actionPerformed(e: AnActionEvent) {
    log.info("Comparing HEAD with the working copy")
    val project = e.project ?: return
    val files = e.getData(VcsDataKeys.VIRTUAL_FILES)?.firstOrNull()
    val changeListManager = ChangeListManager.getInstance(project)

    Thread {
      try {
        val diffInfo = getWorkingCopyDiff(changeListManager)
        openChatWithDiff(e, diffInfo)
      } catch (e: Throwable) {
        log.error("Error comparing changes", e)
        JOptionPane.showMessageDialog(null, e.message, "Error", JOptionPane.ERROR_MESSAGE)
      }
    }.start()
  }

  private fun openChatWithDiff(e: AnActionEvent, diffInfo: String) {
    val session = Session.newGlobalID()
    SessionProxyServer.agents[session] = CodeChatSocketManager(
      session = session,
      language = "diff",
      codeSelection = diffInfo,
      filename = "working_copy_changes.diff",
      api = IdeaChatClient.instance,
      model = AppSettingsState.instance.smartModel.chatModel(),
      parsingModel = AppSettingsState.instance.fastModel.chatModel(),
      storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome)
    )
    ApplicationServer.appInfoMap[session] = AppInfoData(
      applicationName = "Code Chat",
      singleInput = false,
      stickyInput = true,
      loadImages = false,
      showMenubar = false
    )
    SessionProxyServer.metadataStorage.setSessionName(
      null,
      session,
      "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
    )

    val server = AppServer.getServer(e.project)

    Thread {
      Thread.sleep(500)
      try {
        val uri = server.server.uri.resolve("/#$session")
        log.info("Opening browser to $uri")
        browse(uri)
      } catch (e: Throwable) {
        log.warn("Error opening browser", e)
      }
    }.start()
  }


  private fun getWorkingCopyDiff(changeListManager: ChangeListManager): String {
    val changes = changeListManager.allChanges
    return changes.joinToString("\n\n") { change ->
      val diffForChange = getDiffForChange(change)
      "File: ${change.virtualFile?.path ?: "Unknown"}\n" +
          "Type: ${change.type}\n" +
          (diffForChange ?: "No diff available")
    }.ifEmpty { "No changes found" }
  }

  private fun getDiffForChange(change: com.intellij.openapi.vcs.changes.Change): String? {
    val beforeRevision = change.beforeRevision
    val afterRevision = change.afterRevision

    if (beforeRevision == null && afterRevision == null) {
      return null
    }

    val beforeContent = beforeRevision?.content ?: ""
    val afterContent = afterRevision?.content ?: ""

    return createSimpleDiff(beforeContent, afterContent)
  }

  private fun createSimpleDiff(beforeContent: String, afterContent: String): String {
    val beforeLines = beforeContent.lines()
    val afterLines = afterContent.lines()
    val diff = StringBuilder()

    for ((index, line) in afterLines.withIndex()) {
      if (index >= beforeLines.size) {
        diff.appendLine("+ $line")
      } else if (line != beforeLines[index]) {
        diff.appendLine("- ${beforeLines[index]}")
        diff.appendLine("+ $line")
      }
    }

    if (beforeLines.size > afterLines.size) {
      for (i in afterLines.size until beforeLines.size) {
        diff.appendLine("- ${beforeLines[i]}")
      }
    }

    return diff.toString()
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val vcs = e.getData(VcsDataKeys.VCS)
    e.presentation.isEnabledAndVisible = project != null && vcs != null
  }
}