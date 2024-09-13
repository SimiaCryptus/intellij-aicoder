package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.skyenet.apps.general.PlanChatApp
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import org.slf4j.LoggerFactory
import java.awt.Desktop
import kotlin.collections.set

class PlanChatAction : BaseAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        val dialog = PlanAheadConfigDialog(
            e.project, PlanSettings(
                model = AppSettingsState.instance.smartModel.chatModel(),
                parsingModel = AppSettingsState.instance.defaultFastModel(),
                command = listOf(
                    if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                ),
                temperature = AppSettingsState.instance.temperature,
                workingDir = UITools.getSelectedFolder(e)?.toFile?.absolutePath ?: "",
                env = mapOf()
            )
        )
        if (dialog.showAndGet()) {
            // Settings are applied only if the user clicks OK
            val session = StorageInterface.newGlobalID()
            val folder = UITools.getSelectedFolder(e)
            val root = folder?.toFile ?: getModuleRootForFile(
                UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException("")
            )
            DataStorage.sessionPaths[session] = root
            SessionProxyServer.chats[session] = PlanChatApp(
                rootFile = root,
                planSettings = dialog.settings.copy(
                    env = mapOf(),
                    workingDir = root.absolutePath,
                    language = if (isWindows) "powershell" else "bash",
                    command = listOf(
                        if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                    ),
                    parsingModel = AppSettingsState.instance.defaultFastModel(),
                ),
                model = AppSettingsState.instance.defaultSmartModel(),
                parsingModel = AppSettingsState.instance.defaultFastModel(),
                showMenubar = false,
                api = api,
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
                Desktop.getDesktop().browse(uri)
            } catch (e: Throwable) {
                LoggerFactory.getLogger(PlanChatAction::class.java).warn("Error opening browser", e)
            }
        }.start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlanChatAction::class.java)
    }
}