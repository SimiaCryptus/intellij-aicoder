package com.github.simiacryptus.aicoder.actions.plan

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.actions.generic.toFile
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.general.PlanAheadApp
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import kotlin.collections.set


class PlanAheadAction : BaseAction() {
    val path = "/taskDev"

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        val dialog = PlanConfigDialog(
            e.project, PlanSettings(
                defaultModel = AppSettingsState.instance.smartModel.chatModel(),
                parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                command = listOf(
                    if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                ),
                temperature = AppSettingsState.instance.temperature,
                workingDir = UITools.getRoot(e),
                env = mapOf(),
                githubToken = AppSettingsState.instance.githubToken,
                googleApiKey = AppSettingsState.instance.googleApiKey,
                googleSearchEngineId = AppSettingsState.instance.googleSearchEngineId,
            )
        )
        if (dialog.showAndGet()) {
            // Settings are applied only if the user clicks OK
            val session = Session.newGlobalID()
            val folder = UITools.getSelectedFolder(e)
            val root = folder?.toFile ?: getModuleRootForFile(
                UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException("")
            )
            DataStorage.sessionPaths[session] = root
            val planSettings = dialog.settings.copy(
                env = mapOf(),
                workingDir = root.absolutePath,
                language = if (isWindows) "powershell" else "bash",
                command = listOf(
                    if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                ),
                parsingModel = AppSettingsState.instance.fastModel.chatModel(),
            )
            SessionProxyServer.chats[session] = PlanAheadApp(
                planSettings = planSettings,
                model = AppSettingsState.instance.smartModel.chatModel(),
                parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                showMenubar = false,
                api = api,
                api2 = api2,
            )
            ApplicationServer.appInfoMap[session] = AppInfoData(
                applicationName = "Code Chat",
                singleInput = true,
                stickyInput = false,
                loadImages = false,
                showMenubar = false
            )
            val server = AppServer.getServer(e.project)

            openBrowser(server, session.toString())
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
                LoggerFactory.getLogger(PlanAheadAction::class.java).warn("Error opening browser", e)
            }
        }.start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlanAheadAction::class.java)
    }
}