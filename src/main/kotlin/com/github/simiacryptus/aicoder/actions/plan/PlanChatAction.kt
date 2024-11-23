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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.general.PlanChatApp
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import kotlin.collections.set

/**
 * Action that opens a Plan Chat interface for executing and planning commands.
 * Supports both Windows (PowerShell) and Unix (Bash) environments.
 */

class PlanChatAction : BaseAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun isEnabled(e: AnActionEvent): Boolean {
        if (!super.isEnabled(e)) return false
        return UITools.getSelectedFolder(e) != null || UITools.getSelectedFile(e) != null
    }


    override fun handle(e: AnActionEvent) {
        try {
            UITools.runAsync(e.project, "Initializing Plan Chat", true) { progress ->
                progress.isIndeterminate = true
                progress.text = "Setting up chat environment..."
                initializeAndOpenChat(e)
            }
        } catch (ex: Throwable) {
            UITools.error(log, "Failed to initialize Plan Chat", ex)
        }
    }

    private fun initializeAndOpenChat(e: AnActionEvent) {
        val dialog = PlanAheadConfigDialog(
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
            setupChatSession(e, dialog.settings)
        }
    }

    private fun setupChatSession(e: AnActionEvent, settings: PlanSettings) {
        WriteCommandAction.runWriteCommandAction(e.project) {
            val session = Session.newGlobalID()
            val folder = UITools.getSelectedFolder(e)
            val root = folder?.toFile ?: getModuleRootForFile(
                UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException("")
            )
            DataStorage.sessionPaths[session] = root
            SessionProxyServer.chats[session] = PlanChatApp(
                planSettings = settings.copy(
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
        ApplicationManager.getApplication().invokeLater {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                log.error("Failed to open browser", e)
                LoggerFactory.getLogger(PlanChatAction::class.java).warn("Error opening browser", e)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlanChatAction::class.java)
    }
}