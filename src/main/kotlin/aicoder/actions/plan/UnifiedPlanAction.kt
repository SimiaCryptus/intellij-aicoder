package aicoder.actions.plan

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.SimpleCommandAction.Companion.tripleTilde
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.general.UnifiedPlanApp
import com.simiacryptus.skyenet.apps.graph.GraphOrderedPlanMode
import com.simiacryptus.skyenet.apps.plan.cognitive.AutoPlanMode
import com.simiacryptus.skyenet.apps.plan.cognitive.CognitiveModeStrategy
import com.simiacryptus.skyenet.apps.plan.cognitive.PlanAheadMode
import com.simiacryptus.skyenet.apps.plan.cognitive.SingleTaskMode
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.util.FileValidationUtils
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import javax.swing.JOptionPane

class UnifiedPlanAction : BaseAction() {
  private companion object {
    private const val MAX_FILE_SIZE = 512 * 1024
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun handle(e: AnActionEvent) {
    // The unified dialog now includes cognitive mode settings.
    val dialog = PlanConfigDialog(
      e.project, PlanSettings(
        defaultModel = AppSettingsState.instance.smartModel.chatModel(),
        parsingModel = AppSettingsState.instance.fastModel.chatModel(),
        shellCmd = listOf(
          if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
        ),
        temperature = AppSettingsState.instance.temperature.coerceIn(0.0, 1.0),
        workingDir = UITools.getRoot(e),
        env = mapOf(),
        githubToken = AppSettingsState.instance.githubToken,
        googleApiKey = AppSettingsState.instance.googleApiKey,
        googleSearchEngineId = AppSettingsState.instance.googleSearchEngineId,
      ),
      // singleTaskMode now depends on the cognitive mode selection in the dialog
      singleTaskMode = false
    )

    if (dialog.showAndGet()) {
      try {
        val planSettings = dialog.settings
        // Get cognitive mode selection from the dialog's combo box (cast as needed)
        val selectedCognitiveMode = dialog.cognitiveModeCombo.selectedItem as String
        // Convert the selection string to the appropriate CognitiveModeStrategy.
        val cognitiveMode = when (selectedCognitiveMode) {
          "Plan Ahead" -> PlanAheadMode.Companion
          "Single Task" -> SingleTaskMode.Companion
          "Graph" -> GraphOrderedPlanMode.Companion
          else -> AutoPlanMode.Companion
        }

        UITools.runAsync(e.project, "Initializing Unified Plan", true) { progress ->
          initializeChat(e, progress, planSettings, cognitiveMode)
        }
      } catch (ex: Exception) {
        log.error("Failed to initialize unified plan", ex)
        UITools.showError(e.project, "Failed to initialize unified plan: ${ex.message}")
      }
    }
  }

  private fun initializeChat(
    e: AnActionEvent, 
    progress: ProgressIndicator, 
    planSettings: PlanSettings,
    cognitiveStrategy: CognitiveModeStrategy
  ) {
    progress.text = "Setting up session..."
    val session = Session.newGlobalID()
    val root = getProjectRoot(e) ?: throw RuntimeException("Could not determine project root")
    progress.text = "Processing files..."
    setupChatSession(session, root, e, planSettings, cognitiveStrategy)
    progress.text = "Starting server..."
    val server = AppServer.getServer(e.project)
    openBrowser(server, session.toString())
  }

  private fun getProjectRoot(e: AnActionEvent): File? {
    val folder = UITools.getSelectedFolder(e)
    return folder?.toFile ?: UITools.getSelectedFile(e)?.parent?.toFile?.let { file ->
      getModuleRootForFile(file)
    }
  }

  private fun setupChatSession(
    session: Session, 
    root: File, 
    e: AnActionEvent, 
    planSettings: PlanSettings,
    cognitiveStrategy: CognitiveModeStrategy
  ) {
    DataStorage.sessionPaths[session] = root
    SessionProxyServer.chats[session] = createUnifiedPlanApp(root, e, planSettings, cognitiveStrategy)
    ApplicationServer.appInfoMap[session] = AppInfoData(
      applicationName = "Unified Planning",
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
  }

  private fun createUnifiedPlanApp(
    root: File, 
    e: AnActionEvent, 
    planSettings: PlanSettings,
    cognitiveStrategy: CognitiveModeStrategy
  ): UnifiedPlanApp = UnifiedPlanApp(
    applicationName = "Unified Planning",
    path = "/unifiedPlan",
    planSettings = planSettings.copy(
      env = mapOf(),
      workingDir = root.absolutePath,
      language = if (isWindows) "powershell" else "bash",
      command = listOf(
        if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
      ),
      parsingModel = AppSettingsState.instance.fastModel.chatModel(),
    ),
    model = AppSettingsState.instance.smartModel.chatModel(),
    parsingModel = AppSettingsState.instance.fastModel.chatModel(),
    showMenubar = false,
    api = api,
    api2 = api2,
    cognitiveStrategy = cognitiveStrategy
  )

  private fun openBrowser(server: AppServer, session: String) {
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
}