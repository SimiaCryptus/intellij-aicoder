package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.skyenet.apps.general.PlanAheadApp
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import org.slf4j.LoggerFactory
import java.awt.Desktop
import com.intellij.openapi.ui.Messages
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.apps.plan.PlanUtil

class PrePlanAction : BaseAction() {
    val path = "/prePlanTaskDev"

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        val jsonInput = Messages.showInputDialog(
            e.project,
            "Enter TaskBreakdownWithPrompt JSON:",
            "Pre-Plan Task",
            Messages.getQuestionIcon()
        ) ?: return

        try {
            val taskBreakdownWithPrompt = JsonUtil.fromJson<PlanUtil.TaskBreakdownWithPrompt>(jsonInput, PlanUtil.TaskBreakdownWithPrompt::class.java)
            
            val session = StorageInterface.newGlobalID()
            val folder = UITools.getSelectedFolder(e)
            val root = folder?.toFile ?: getModuleRootForFile(
                UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException("No file selected")
            )
            DataStorage.sessionPaths[session] = root
            
            SessionProxyServer.chats[session] = PlanAheadApp(
                rootFile = root,
                planSettings = PlanSettings(
                    model = AppSettingsState.instance.smartModel.chatModel(),
                    parsingModel = AppSettingsState.instance.defaultFastModel(),
                    command = listOf(
                        if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                    ),
                    temperature = AppSettingsState.instance.temperature,
                    workingDir = root.absolutePath,
                    env = mapOf(),
                    language = if (isWindows) "powershell" else "bash",
                ),
                model = AppSettingsState.instance.defaultSmartModel(),
                parsingModel = AppSettingsState.instance.defaultFastModel(),
                showMenubar = false,
                initialPlan = taskBreakdownWithPrompt
            )
            
            val server = AppServer.getServer(e.project)
            openBrowser(server, session.toString())
        } catch (ex: Exception) {
            Messages.showErrorDialog(e.project, "Invalid JSON input: ${ex.message}", "Error")
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
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(PrePlanAction::class.java)
    }
}