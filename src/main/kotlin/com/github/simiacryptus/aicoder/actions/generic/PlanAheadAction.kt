package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.apps.general.PlanAheadApp
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator
import com.simiacryptus.skyenet.apps.plan.Settings
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import kotlin.collections.set


class PlanAheadAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    data class PlanAheadSettings(
        var model: String = AppSettingsState.instance.smartModel,
        var temperature: Double = AppSettingsState.instance.temperature,
        var enableTaskPlanning: Boolean = false,
        var enableShellCommands: Boolean = false,
        var enableDocumentation: Boolean = false,
        var enableFileModification: Boolean = true,
        var enableInquiry: Boolean = true,
        var enableCodeReview: Boolean = false,
        var enableTestGeneration: Boolean = false,
        var enableOptimization: Boolean = false,
        var enableSecurityAudit: Boolean = false,
        var enablePerformanceAnalysis: Boolean = false,
        var enableRefactorTask: Boolean = false,
        var enableForeachTask: Boolean = false,
        var autoFix: Boolean = false,
        var enableCommandAutoFix: Boolean = false,
        var commandAutoFixCommands: List<String> = listOf()
    )

    class PlanAheadConfigDialog(
        project: Project?,
        private val settings: PlanAheadSettings
    ) : DialogWrapper(project) {
        private val foreachTaskCheckbox = JCheckBox("Enable Foreach Task", settings.enableForeachTask)
        private val items = ChatModels.values().toList().toTypedArray()
        private val modelComboBox: ComboBox<String> = ComboBox(items.map { it.first }.toTypedArray())

        // Replace JTextField with JSlider for temperature
        private val temperatureSlider = JSlider(0, 100, (settings.temperature * 100).toInt())

        private val taskPlanningCheckbox = JCheckBox("Enable Task Planning", settings.enableTaskPlanning)
        private val shellCommandsCheckbox = JCheckBox("Enable Shell Commands", settings.enableShellCommands)
        private val documentationCheckbox = JCheckBox("Enable Documentation", settings.enableDocumentation)
        private val fileModificationCheckbox = JCheckBox("Enable File Modification", settings.enableFileModification)
        private val inquiryCheckbox = JCheckBox("Enable Inquiry", settings.enableInquiry)
        private val codeReviewCheckbox = JCheckBox("Enable Code Review", settings.enableCodeReview)
        private val testGenerationCheckbox = JCheckBox("Enable Test Generation", settings.enableTestGeneration)
        private val optimizationCheckbox = JCheckBox("Enable Optimization", settings.enableOptimization)
        private val securityAuditCheckbox = JCheckBox("Enable Security Audit", settings.enableSecurityAudit)
        private val performanceAnalysisCheckbox =
            JCheckBox("Enable Performance Analysis", settings.enablePerformanceAnalysis)
        private val refactorTaskCheckbox = JCheckBox("Enable Refactor Task", settings.enableRefactorTask)
        private val autoFixCheckbox = JCheckBox("Auto-apply fixes", settings.autoFix)
        private val checkboxStates = AppSettingsState.instance.executables.map { true }.toMutableList()
        private val tableModel = object : DefaultTableModel(arrayOf("Enabled", "Command"), 0) {

            init {
                AppSettingsState.instance.executables.forEach { command ->
                    addRow(arrayOf(true, command))
                }
            }

            override fun getColumnClass(columnIndex: Int) = when (columnIndex) {
                0 -> java.lang.Boolean::class.java
                else -> super.getColumnClass(columnIndex)
            }

            override fun isCellEditable(row: Int, column: Int) = column == 0

            override fun setValueAt(aValue: Any?, row: Int, column: Int) {
                super.setValueAt(aValue, row, column)
                if (column == 0 && aValue is Boolean) {
                    checkboxStates[row] = aValue
                } else {
                    throw IllegalArgumentException("Invalid column index: $column")
                }
            }

            override fun getValueAt(row: Int, column: Int): Any =
                if (column == 0) {
                    checkboxStates[row]
                } else super.getValueAt(row, column)
        }
        private val commandTable = JBTable(tableModel).apply { putClientProperty("terminateEditOnFocusLost", true) }
        private val addCommandButton = JButton("Add Command")
        private val editCommandButton = JButton("Edit Command")

        init {
            init()
            title = "Configure Plan Ahead Action"
            // Add change listener to update the settings based on slider value
            temperatureSlider.addChangeListener {
                settings.temperature = temperatureSlider.value / 100.0
            }
            val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select Command")
                .withDescription("Choose an executable file for the auto-fix command")
            addCommandButton.addActionListener {
                val chosenFile = FileChooser.chooseFile(fileChooserDescriptor, project, null)
                if (chosenFile != null) {
                    val newCommand = chosenFile.path
                    val confirmResult = JOptionPane.showConfirmDialog(
                        null,
                        "Add command: $newCommand?",
                        "Confirm Command",
                        JOptionPane.YES_NO_OPTION
                    )
                    if (confirmResult == JOptionPane.YES_OPTION) {
                        tableModel.addRow(arrayOf(false, newCommand))
                        checkboxStates.add(true)
                        AppSettingsState.instance.executables.add(newCommand)
                    }
                }
            }
            editCommandButton.addActionListener {
                val selectedRow = commandTable.selectedRow
                if (selectedRow != -1) {
                    val currentCommand = tableModel.getValueAt(selectedRow, 1) as String
                    val newCommand = JOptionPane.showInputDialog(
                        null,
                        "Edit command:",
                        currentCommand
                    )
                    if (newCommand != null && newCommand.isNotEmpty()) {
                        val confirmResult = JOptionPane.showConfirmDialog(
                            null,
                            "Update command to: $newCommand?",
                            "Confirm Edit",
                            JOptionPane.YES_NO_OPTION
                        )
                        if (confirmResult == JOptionPane.YES_OPTION) {
                            tableModel.setValueAt(newCommand, selectedRow, 1)
                            AppSettingsState.instance.executables.remove(currentCommand)
                            AppSettingsState.instance.executables.add(newCommand)
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Please select a command to edit.")
                }
            }
            commandTable.columnModel.getColumn(0).apply {
                val checkBoxes = mutableMapOf<Int, JBCheckBox>()
                fun jbCheckBox(
                    row: Int,
                    value: Any,
                    column: Int
                ) = checkBoxes.getOrPut(row) {
                    JBCheckBox().apply {
                        this.isSelected = value as Boolean
                        this.addActionListener {
                            tableModel.setValueAt(this.isSelected, row, column)
                        }
                    }
                }

                cellRenderer = object : DefaultTableCellRenderer() {
                    override fun getTableCellRendererComponent(
                        table: JTable,
                        value: Any,
                        isSelected: Boolean,
                        hasFocus: Boolean,
                        row: Int,
                        column: Int
                    ) = jbCheckBox(row, value, column)
                }
                cellEditor = object : DefaultCellEditor(JBCheckBox()) {
                    override fun getTableCellEditorComponent(
                        table: JTable,
                        value: Any,
                        isSelected: Boolean,
                        row: Int,
                        column: Int
                    ) = jbCheckBox(row, value, column)
                }
                preferredWidth = 60
                maxWidth = 60
            }
            commandTable.selectionModel.addListSelectionListener {
                editCommandButton.isEnabled = commandTable.selectedRow != -1
            }
            editCommandButton.isEnabled = false
        }


        override fun createCenterPanel(): JComponent {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.add(JLabel("Model:"))
            panel.add(modelComboBox)
            val indexOfFirst = items.indexOfFirst {
                it.second.name == settings.model || it.second.modelName == settings.model || it.first == settings.model
            }
            modelComboBox.selectedIndex = indexOfFirst
            panel.add(JLabel("Temperature:"))
            panel.add(temperatureSlider)
            panel.add(taskPlanningCheckbox)
            panel.add(shellCommandsCheckbox)
            panel.add(documentationCheckbox)
            panel.add(fileModificationCheckbox)
            panel.add(inquiryCheckbox)
            panel.add(codeReviewCheckbox)
            panel.add(testGenerationCheckbox)
            panel.add(optimizationCheckbox)
            panel.add(securityAuditCheckbox)
            panel.add(performanceAnalysisCheckbox)
            panel.add(refactorTaskCheckbox)
            panel.add(foreachTaskCheckbox)
            panel.add(autoFixCheckbox)
            panel.add(JLabel("Auto-Fix Commands:"))
            val scrollPane = JBScrollPane(commandTable)
            scrollPane.preferredSize = Dimension(350, 100)
            val tablePanel = JPanel(BorderLayout())
            tablePanel.add(scrollPane, BorderLayout.CENTER)
            panel.add(tablePanel)
            val buttonPanel = JPanel(GridLayout(1, 3))
            buttonPanel.add(addCommandButton)
            panel.add(buttonPanel)
            commandTable.isEnabled = true
            addCommandButton.isEnabled = true
            return panel
        }


        override fun doOKAction() {
            if (modelComboBox.selectedItem == null) {
                JOptionPane.showMessageDialog(
                    null,
                    "Model selection cannot be empty",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }
            settings.model = modelComboBox.selectedItem as String
            settings.enableTaskPlanning = taskPlanningCheckbox.isSelected
            settings.enableShellCommands = shellCommandsCheckbox.isSelected
            settings.enableDocumentation = documentationCheckbox.isSelected
            settings.enableFileModification = fileModificationCheckbox.isSelected
            settings.enableInquiry = inquiryCheckbox.isSelected
            settings.enableCodeReview = codeReviewCheckbox.isSelected
            settings.enableTestGeneration = testGenerationCheckbox.isSelected
            settings.enableOptimization = optimizationCheckbox.isSelected
            settings.enableSecurityAudit = securityAuditCheckbox.isSelected
            settings.enablePerformanceAnalysis = performanceAnalysisCheckbox.isSelected
            settings.enableRefactorTask = refactorTaskCheckbox.isSelected
            settings.enableForeachTask = foreachTaskCheckbox.isSelected
            settings.autoFix = autoFixCheckbox.isSelected
            settings.commandAutoFixCommands = (0 until tableModel.rowCount)
                .filter { tableModel.getValueAt(it, 0) as Boolean }
                .map { tableModel.getValueAt(it, 1) as String }
            settings.enableCommandAutoFix = settings.commandAutoFixCommands.isNotEmpty()
            // Update the global tool collection
            AppSettingsState.instance.executables.clear()
            AppSettingsState.instance.executables.addAll((0 until tableModel.rowCount).map {
                tableModel.getValueAt(it, 1) as String
            })
            super.doOKAction()
        }
    }


    val path = "/taskDev"
    override fun handle(e: AnActionEvent) {
        val project = e.project
        val settings = PlanAheadSettings()

        val dialog = PlanAheadConfigDialog(project, settings)
        if (dialog.showAndGet()) {
            // Settings are applied only if the user clicks OK
            val session = StorageInterface.newGlobalID()
            val folder = UITools.getSelectedFolder(e)
            val root = folder?.toFile ?: getModuleRootForFile(
                UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException("")
            )

            DataStorage.sessionPaths[session] = root
            SessionProxyServer.chats[session] = PlanAheadApp(
                rootFile = root,
                settings = Settings(
                    documentationEnabled = settings.enableDocumentation,
                    fileModificationEnabled = settings.enableFileModification,
                    inquiryEnabled = settings.enableInquiry,
                    codeReviewEnabled = settings.enableCodeReview,
                    testGenerationEnabled = settings.enableTestGeneration,
                    optimizationEnabled = settings.enableOptimization,
                    securityAuditEnabled = settings.enableSecurityAudit,
                    performanceAnalysisEnabled = settings.enablePerformanceAnalysis,
                    refactorTaskEnabled = settings.enableRefactorTask,
                    foreachTaskEnabled = settings.enableForeachTask,
                    model = settings.model.chatModel(), // Use the model from settings
                    temperature = settings.temperature, // Use the temperature from settings
                    taskPlanningEnabled = settings.enableTaskPlanning, // Use the task planning flag from settings
                    shellCommandTaskEnabled = settings.enableShellCommands, // Use the shell command flag from settings
                    autoFix = settings.autoFix, // Use the autoFix flag from settings
                    enableCommandAutoFix = settings.enableCommandAutoFix, // Use the enableCommandAutoFix flag from settings
                    commandAutoFixCommands = settings.commandAutoFixCommands, // Use the commandAutoFixCommands from settings
                    env = mapOf(),
                    workingDir = root.absolutePath,
                    language = if (PlanCoordinator.isWindows) "powershell" else "bash",
                    command = listOf(if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"),
                    parsingModel = AppSettingsState.instance.defaultFastModel(),
                ),
                model = AppSettingsState.instance.defaultSmartModel(),
                parsingModel = AppSettingsState.instance.defaultFastModel(),
                showMenubar = false
            )
            val server = AppServer.getServer(project)

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
                LoggerFactory.getLogger(PlanAheadAction::class.java).warn("Error opening browser", e)
            }
        }.start()
    }


    companion object {
        private val log = LoggerFactory.getLogger(PlanAheadAction::class.java)
    }
}