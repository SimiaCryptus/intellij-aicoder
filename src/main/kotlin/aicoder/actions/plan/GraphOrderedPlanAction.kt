package aicoder.actions.plan

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.graph.GraphOrderedPlanApp
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.io.File

/**
 * GraphOrderedPlanAction launches the GraphOrderedPlanApp which orders the nodes in a software graph,
 * generates sub-plans for each node and executes them as an aggregated DAG.
 */
class GraphOrderedPlanAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun isEnabled(event: AnActionEvent): Boolean {
        val file = UITools.getSelectedFile(event)
        // enable only if a JSON file is selected
        return file != null && file.extension.equals("json", ignoreCase = true)
    }

    override fun handle(e: AnActionEvent) {
        try {
            var dialogSettings: PlanSettings? = null
            val shellCmd = if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
            val selectedFile = UITools.getSelectedFile(e) ?: throw RuntimeException("No file selected")
            if (!selectedFile.extension.equals("json", ignoreCase = true)) {
                throw RuntimeException("Selected file must be a JSON file")
            }
            val workingDirectory = selectedFile.parent?.path ?: throw RuntimeException("No parent directory found")
            val dialog = PlanConfigDialog(
                project = e.project, settings = PlanSettings(
                    defaultModel = AppSettingsState.instance.smartModel.chatModel(),
                    parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                    shellCmd = listOf(shellCmd),
                    temperature = AppSettingsState.instance.temperature,
                    workingDir = workingDirectory,
                    env = mapOf(),
                    githubToken = AppSettingsState.instance.githubToken,
                    googleApiKey = AppSettingsState.instance.googleApiKey,
                    googleSearchEngineId = AppSettingsState.instance.googleSearchEngineId,
                )
            )
            if (!dialog.showAndGet()) {
                throw RuntimeException("Configuration cancelled by user")
            }
            dialogSettings = dialog.settings
            val settings = dialogSettings ?: throw RuntimeException("Failed to get dialog settings")
            val session = Session.newGlobalID()
            UITools.runAsync(e.project, "Initializing Graph Ordered Planning", true) { progress ->
                progress.text = "Validating selected file..."
                val root = File(workingDirectory)
                DataStorage.sessionPaths[session] = root
                progress.text = "Initializing graph ordered planning application..."
                SessionProxyServer.chats[session] = GraphOrderedPlanApp(
                    planSettings = settings.copy(
                        env = mapOf(),
                        workingDir = root.absolutePath,
                        language = shellCmd,
                        command = listOf(shellCmd),
                        parsingModel = AppSettingsState.instance.fastModel.chatModel()
                    ),
                    model = AppSettingsState.instance.smartModel.chatModel(),
                    parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                    showMenubar = false,
                    api = api,
                    api2 = api2,
                    graphFile = selectedFile.path
                )
                progress.text = "Configuring application interface..."
                ApplicationServer.appInfoMap[session] = AppInfoData(
                    applicationName = "Graph Ordered Planning",
                    singleInput = true,
                    stickyInput = false,
                    loadImages = false,
                    showMenubar = false
                )
                progress.text = "Launching browser interface..."
                openBrowser(AppServer.getServer(e.project), session.toString())
            }
        } catch (ex: Throwable) {
            UITools.error(log, "Failed to initialize Graph Ordered Planning", ex)
        }
    }

    private fun openBrowser(server: AppServer, session: String) {
        Thread {
            try {
                Thread.sleep(500)
                val uri = server.server.uri.resolve("/#$session")
                log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                log.error("Failed to open browser interface", e)
                throw RuntimeException("Failed to open browser interface: ${e.message}")
            }
        }.start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(GraphOrderedPlanAction::class.java)
    }
}