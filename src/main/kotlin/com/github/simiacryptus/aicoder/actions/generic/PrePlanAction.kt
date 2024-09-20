package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.apps.general.PlanAheadApp
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanUtil
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class PrePlanAction : BaseAction() {
    val path = "/prePlanTaskDev"

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        var jsonInput = Messages.showMultilineInputDialog(
            e.project,
            "Enter TaskBreakdownWithPrompt JSON:",
            "Pre-Plan Task",
            "",
            Messages.getQuestionIcon(),
            null,
        ) ?: return
        jsonInput = fillInTemplate(jsonInput)

        try {
            val taskBreakdownWithPrompt = JsonUtil.fromJson<PlanUtil.TaskBreakdownWithPrompt>(jsonInput, PlanUtil.TaskBreakdownWithPrompt::class.java)
            
            val session = StorageInterface.newGlobalID()
            val folder = UITools.getSelectedFolder(e)
            val root = folder?.toFile ?: getModuleRootForFile(
                UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException("No file selected")
            )
            DataStorage.sessionPaths[session] = root

            var planSettings = PlanSettings(
                defaultModel = AppSettingsState.instance.defaultSmartModel(),
                parsingModel = AppSettingsState.instance.defaultFastModel(),
                command = listOf(
                    if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                ),
                temperature = AppSettingsState.instance.temperature,
                workingDir = root.absolutePath,
                env = mapOf(),
                language = if (isWindows) "powershell" else "bash",
            )
            planSettings = PlanAheadConfigDialog(e.project, planSettings).let {
                if (!it.showAndGet()) throw RuntimeException("User cancelled")
                it.settings
            }
            SessionProxyServer.chats[session] = PlanAheadApp(
                rootFile = root,
                planSettings = planSettings,
                model = AppSettingsState.instance.defaultSmartModel(),
                parsingModel = AppSettingsState.instance.defaultFastModel(),
                showMenubar = false,
                initialPlan = taskBreakdownWithPrompt,
                api = api,
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

        fun fillInTemplate(jsonInput: String): String {
            val variables = Regex("\\{\\{(\\w+)}}").findAll(jsonInput).map { it.groupValues[1] }.toSet()
            if (variables.isEmpty()) return jsonInput
            val formValues = showFormDialog(variables)
            return variables.fold(jsonInput) { acc, variable ->
                acc.replace("{{$variable}}", formValues[variable] ?: "")
            }
        }

        private fun showFormDialog(variables: Set<String>): Map<String, String> {
            val dialog = object : DialogWrapper(true) {
                val fields = variables.associateWith { JBTextField() }

                init {
                    init()
                    title = "Fill in Template Variables"
                }

                override fun createCenterPanel(): JComponent {
                    val panel = JPanel(GridBagLayout())
                    val gbc = GridBagConstraints().apply {
                        fill = GridBagConstraints.HORIZONTAL
                        weightx = 1.0
                    }
                    variables.forEach { variable ->
                        gbc.gridy++
                        gbc.gridx = 0
                        panel.add(com.intellij.ui.components.JBLabel(variable), gbc)
                        gbc.gridx = 1
                        panel.add(fields[variable], gbc)
                    }
                    return panel
                }

                fun getValues(): Map<String, String> {
                    return fields.mapValues { (_, field) -> field.text }
                }
            }
            return if (dialog.showAndGet()) {
                dialog.getValues()
            } else {
                mapOf()
            }
        }
    }
}