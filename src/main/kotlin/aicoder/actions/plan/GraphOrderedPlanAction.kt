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
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
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
        val selectedFile = UITools.getSelectedFile(e)
        if (selectedFile == null || !selectedFile.extension.equals("json", ignoreCase = true)) {
            log.info("No JSON file selected or file extension invalid.")
            return
        }
        val graphFile = selectedFile.path
        val workingDirectory = selectedFile.parent?.path
                ?: throw RuntimeException("No parent directory found for the selected file: $graphFile")
        // Determine OS once to avoid duplicate checks
        val osName = System.getProperty("os.name").lowercase()
        val isOsWindows = osName.contains("win")
        val shellCmd = if (isOsWindows) "powershell" else "bash"
        val dialog = PlanConfigDialog(
            project = e.project,
            settings = PlanSettings(
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
        if (!dialog.showAndGet()) return
        val session = Session.newGlobalID()
        val root = File(workingDirectory)
        DataStorage.sessionPaths[session] = root
        // Create a new session with GraphOrderedPlanApp instead of GraphPlanApp 
        SessionProxyServer.chats[session] = GraphOrderedPlanApp(
            planSettings = dialog.settings.copy(
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
            graphFile = graphFile
        )
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Graph Ordered Planning",
            singleInput = true,
            stickyInput = false,
            loadImages = false,
            showMenubar = false
        )
        openBrowser(AppServer.getServer(e.project), session.toString())
    }
    private fun openBrowser(server: AppServer, session: String) {
        Thread {
            try {
                // Give the server time to start up
                Thread.sleep(500)
                val uri = server.server.uri.resolve("/#$session")
                log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                log.warn("Error while attempting to open the browser", e)
            }
        }.start()
    }
            }
    }
    companion object {
        private val log = LoggerFactory.getLogger(GraphOrderedPlanAction::class.java)
    }
}