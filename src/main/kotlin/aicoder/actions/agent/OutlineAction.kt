package aicoder.actions.agent

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.skyenet.apps.general.OutlineApp
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

class OutlineAction : BaseAction() {
  private var settings = OutlineConfigDialog.OutlineSettings()

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun handle(e: AnActionEvent) {
    val project = e.project ?: return
    val configDialog = OutlineConfigDialog(project, settings)
    if (!configDialog.showAndGet()) return
    settings = configDialog.settings

    try {
      UITools.runAsync(project, "Initializing Outline Tool", true) { progress ->
        progress.isIndeterminate = true
        progress.text = "Setting up outline session..."

        val session = Session.newGlobalID()
        SessionProxyServer.metadataStorage.setSessionName(
          null,
          session,
          "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
        )

        val outlineApp = OutlineApp(
          applicationName = "AI Outline Tool",
          domainName = "outline",
          api2 = IdeaOpenAIClient.instance,
          settings = OutlineApp.Settings(
            models = settings.expansionSteps.map { it.model },
            temperature = settings.temperature,
            parsingModel = settings.parsingModel,
            minTokensForExpansion = settings.minTokensForExpansion,
            showProjector = settings.showProjector,
            writeFinalEssay = settings.writeFinalEssay,
            budget = settings.budget
          )
        )

        SessionProxyServer.chats[session] = outlineApp

        ApplicationServer.appInfoMap[session] = AppInfoData(
          applicationName = "AI Outline Tool",
          singleInput = true,
          stickyInput = false,
          loadImages = false,
          showMenubar = false
        )

        val server = AppServer.getServer(project)
        val uri = server.server.uri.resolve("/#$session")

        ApplicationManager.getApplication().executeOnPooledThread {
          try {
            BaseAction.log.info("Opening browser to $uri")
            browse(uri)
          } catch (e: Throwable) {
            UITools.error(log, "Failed to open browser", e)
          }
        }
      }
    } catch (e: Throwable) {
      log.warn("Error opening browser", e)
    }
  }

  override fun isEnabled(event: AnActionEvent) = true

  companion object {
    private val log = LoggerFactory.getLogger(OutlineAction::class.java)
  }
}