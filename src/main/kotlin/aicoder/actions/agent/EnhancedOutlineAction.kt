package aicoder.actions.agent

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.skyenet.core.platform.Session
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

class EnhancedOutlineAction : BaseAction() {
  private var settings = EnhancedOutlineConfigDialog.EnhancedOutlineSettings()

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun handle(e: AnActionEvent) {
    val project = e.project ?: return
    val configDialog = EnhancedOutlineConfigDialog(project, settings)
    if (!configDialog.showAndGet()) return
    settings = configDialog.settings
    try {
      UITools.runAsync(project, "Initializing Enhanced Outline Tool", true) { progress ->
        progress.isIndeterminate = true
        progress.text = "Setting up enhanced outline session..."
        val session = Session.newGlobalID()
        SessionProxyServer.metadataStorage.setSessionName(
          null,
          session,
          "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
        )
        // Convert settings from EnhancedOutlineConfigDialog into EnhancedSettings used by EnhancedOutlineApp.
        val enhancedSettings = com.simiacryptus.skyenet.apps.general.EnhancedSettings(
          parsingModel = settings.parsingModel,
          temperature = settings.temperature,
          minTokensForExpansion = settings.minTokensForExpansion,
          showProjector = settings.showProjector,
          writeFinalEssay = settings.writeFinalEssay,
          budget = settings.budget,
          phaseConfigs = settings.phases.map { phase ->
            com.simiacryptus.skyenet.apps.general.PhaseConfig(
              extract = phase.extract,
              expansionQuestion = phase.question,
              model = phase.model
            )
          }
        )
        // Instantiate and register the EnhancedOutlineApp.
        val enhancedApp = com.simiacryptus.skyenet.apps.general.EnhancedOutlineApp(
          applicationName = "Enhanced Outline Tool",
          domainName = "enhanced_outline",
          settings = enhancedSettings,
          api2 = com.simiacryptus.aicoder.util.IdeaOpenAIClient.instance
        )
        SessionProxyServer.chats[session] = enhancedApp
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

  override fun isEnabled(event: AnActionEvent): Boolean {
    if(!AppSettingsState.instance.devActions) return false
    return true
  }

  companion object {
    private val log = LoggerFactory.getLogger(EnhancedOutlineAction::class.java)
  }
}