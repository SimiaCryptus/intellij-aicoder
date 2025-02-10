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
import com.simiacryptus.skyenet.apps.graph.GraphPlanApp
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.io.File

class GraphPlanAction : BaseAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent): Boolean {
        val file = UITools.getSelectedFile(event)
        return file != null && file.extension.equals("json", ignoreCase = true)
    }

    override fun handle(e: AnActionEvent) {
        val selectedFile = UITools.getSelectedFile(e) ?: return
        if (!selectedFile.extension.equals("json", ignoreCase = true)) return
        val graphFile = selectedFile.path
        val workingDirectory = selectedFile.parent?.path ?: throw RuntimeException("No parent directory found")
        val dialog = PlanConfigDialog(
            project = e.project,
            settings = PlanSettings(
                defaultModel = AppSettingsState.instance.smartModel.chatModel(),
                parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                shellCmd = listOf(
                    if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                ),
                temperature = AppSettingsState.instance.temperature,
                workingDir = workingDirectory,
                env = mapOf(),
                githubToken = AppSettingsState.instance.githubToken,
                googleApiKey = AppSettingsState.instance.googleApiKey,
                googleSearchEngineId = AppSettingsState.instance.googleSearchEngineId,
            )
        )

        if (dialog.showAndGet()) {
            val session = Session.newGlobalID()
            val root = File(workingDirectory)
            DataStorage.sessionPaths[session] = root
            SessionProxyServer.chats[session] = GraphPlanApp(
                planSettings = dialog.settings.copy(
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
                graphFile = graphFile
            )
            ApplicationServer.appInfoMap[session] = AppInfoData(
                applicationName = "Graph-Based Planning",
                singleInput = true,
                stickyInput = false,
                loadImages = false,
                showMenubar = false
            )
            openBrowser(AppServer.getServer(e.project), session.toString())
        }
    }

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

    companion object {
        private val log = LoggerFactory.getLogger(GraphPlanAction::class.java)
    }
}