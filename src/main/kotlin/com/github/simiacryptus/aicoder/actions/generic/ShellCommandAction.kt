package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.interpreter.ProcessInterpreter
import java.awt.GridLayout
import java.io.File
import javax.swing.*

class ShellCommandAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
/*

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = UITools.getSelectedFolder(e) != null
    }
*/

    override fun handle(e: AnActionEvent) {
        val project = e.project
        val selectedFolder = UITools.getSelectedFolder(e)?.toFile
        
        if (selectedFolder == null) {
            Messages.showErrorDialog(project, "Please select a directory", "Error")
            return
        }

        val settings = ShellCommandSettings()
        val dialog = ShellCommandConfigDialog(project, settings)

        if (dialog.showAndGet()) {
            executeShellCommand(project, selectedFolder, settings)
        }
    }

    private fun executeShellCommand(project: Project?, selectedFolder: File, settings: ShellCommandSettings) {
        val api = API()
        val shellActor = CodingActor(
            name = "ShellCommandExecutor",
            interpreterClass = ProcessInterpreter::class,
            details = """
                Execute the following shell command(s) in the specified directory and provide the output.
                Ensure to handle any errors or exceptions gracefully.
                
                Note: This task is for running simple and safe commands. Avoid executing commands that can cause harm to the system or compromise security.
            """.trimIndent(),
            symbols = mapOf(
                "workingDir" to selectedFolder.absolutePath,
                "language" to if (isWindows) "powershell" else "bash",
                "command" to listOf(AppSettingsState.instance.shellCommand)
            ),
            model = settings.model,
            temperature = settings.temperature
        )

        //val result = shellActor.answer(CodingActor.CodeRequest(messages = settings.command), api)
        
        //Messages.showInfoMessage(project, result, "Shell Command Output")
    }

    data class ShellCommandSettings(
        var model: ChatModels = AppSettingsState.instance.smartModel.chatModel(),
        var temperature: Double = AppSettingsState.instance.temperature,
        var command: String = ""
    )

    class ShellCommandConfigDialog(
        project: Project?,
        private val settings: ShellCommandSettings
    ) : DialogWrapper(project) {
        private val modelComboBox = ComboBox(ChatModels.values().values.toTypedArray())
        private val temperatureSlider = JSlider(0, 100, (settings.temperature * 100).toInt())
        private val commandField = JTextField(settings.command)

        init {
            init()
            title = "Configure Shell Command"
            temperatureSlider.addChangeListener {
                settings.temperature = temperatureSlider.value / 100.0
            }
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(GridLayout(0, 2))
            panel.add(JLabel("Model:"))
            panel.add(modelComboBox)
            panel.add(JLabel("Temperature:"))
            panel.add(temperatureSlider)
            panel.add(JLabel("Command:"))
            panel.add(commandField)
            return panel
        }

        override fun doOKAction() {
            settings.model = modelComboBox.selectedItem as ChatModels
            settings.command = commandField.text
            super.doOKAction()
        }
    }

    companion object {
        private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    }
}
