package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.TaskSettings
import com.simiacryptus.skyenet.apps.plan.TaskType
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class PlanAheadConfigDialog(
    project: Project?,
    val settings: PlanSettings,
) : DialogWrapper(project) {
    private val temperatureSlider = JSlider(0, 100, (settings.temperature * 100).toInt())
    private val autoFixCheckbox = JCheckBox("Auto-apply fixes", settings.autoFix)
    private val taskTableModel = object : DefaultTableModel(arrayOf("Enabled", "Task Type", "Model"), 0) {
        override fun getColumnClass(columnIndex: Int) = when (columnIndex) {
            0 -> java.lang.Boolean::class.java
            else -> super.getColumnClass(columnIndex)
        }

        override fun isCellEditable(row: Int, column: Int) = column == 0 || column == 2
    }
    private val taskTable = JBTable(taskTableModel).apply { putClientProperty("terminateEditOnFocusLost", true) }
        // Add a function to retrieve visible models
        private fun getVisibleModels() =
            ChatModels.values().map { it.value }.filter { isVisible(it) }.toList()
                .sortedBy { "${it.provider.name} - ${it.modelName}" }
        // Custom renderer to display provider name and model name
        private fun getModelRenderer() = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable,
                value: Any,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ): Component {
                val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
                if (value is String) {
                    val model = getVisibleModels().find { it.modelName == value }
                    label.text = "<html><b>${model?.provider?.name}</b> - <i>$value</i></html>"
                }
                return label
            }
        }
        companion object {
            fun isVisible(it: ChatModels): Boolean {
                val hasApiKey =
                    AppSettingsState.instance.apiKey?.filter { it.value.isNotBlank() }?.keys?.contains(it.provider.name)
                return false != hasApiKey
            }
        }

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
            taskTable.columnModel.getColumn(2).apply {
                preferredWidth = 200
                maxWidth = 250
                val modelComboBox = JComboBox(getVisibleModels().map { it.modelName }.toTypedArray())
                cellEditor = DefaultCellEditor(modelComboBox)
                cellRenderer = getModelRenderer()
            }

        init()
        title = "Configure Plan Ahead Action"
        // Add model combobox and change listener to update the settings based on slider value
            
        temperatureSlider.addChangeListener {
            settings.temperature = temperatureSlider.value / 100.0
        }
        // Update parsingModel based on modelComboBox selection
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
                    tableModel.addRow(arrayOf(true, newCommand))
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
            preferredWidth = 60
            maxWidth = 60
        }
        commandTable.selectionModel.addListSelectionListener {
            editCommandButton.isEnabled = commandTable.selectedRow != -1
        }
        editCommandButton.isEnabled = false
        // Initialize task table
            val values = TaskType.values()
            values.forEach { taskType ->
            val taskSettings = settings.getTaskSettings(taskType)
            taskTableModel.addRow(
                arrayOf(
                    taskSettings.enabled,
                    taskType.name,
                    taskSettings.model?.modelName ?: AppSettingsState.instance.smartModel,
                )
            )
        }
        taskTable.columnModel.getColumn(0).preferredWidth = 60
        taskTable.columnModel.getColumn(0).maxWidth = 60
        taskTable.columnModel.getColumn(1).preferredWidth = 200
        taskTable.columnModel.getColumn(2).preferredWidth = 200
        // Call setupCommandTable to initialize commandTable
        setupCommandTable()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("Temperature:"))
        panel.add(temperatureSlider)
        panel.add(JLabel("Task Types:"))
        val taskScrollPane = JBScrollPane(taskTable)
        taskScrollPane.preferredSize = Dimension(350, 150)
        val taskTablePanel = JPanel(BorderLayout())
        taskTablePanel.add(taskScrollPane, BorderLayout.CENTER)
        panel.add(taskTablePanel)

        panel.add(autoFixCheckbox)
        panel.add(JLabel("Auto-Fix Commands:"))
        val scrollPane = JBScrollPane(commandTable)
        scrollPane.preferredSize = Dimension(350, 100)
        val tablePanel = JPanel(BorderLayout())
        tablePanel.add(scrollPane, BorderLayout.CENTER)
        panel.add(tablePanel)
        val buttonPanel = JPanel(GridLayout(1, 3))
        buttonPanel.add(addCommandButton)
        buttonPanel.add(editCommandButton)
        panel.add(buttonPanel)
        commandTable.isEnabled = true
        addCommandButton.isEnabled = true
        return panel
    }

    override fun doOKAction() {
        // Update task settings
        for (i in 0 until taskTableModel.rowCount) {
            val taskType = TaskType.valueOf(taskTableModel.getValueAt(i, 1) as String)
            val modelName = taskTableModel.getValueAt(i, 2) as String
            val selectedModel = ChatModels.values().toList().find { it.first == modelName }?.second
            settings.setTaskSettings(taskType, TaskSettings(taskTableModel.getValueAt(i, 0) as Boolean).apply {
                this.model = selectedModel
            })
        }
        settings.autoFix = autoFixCheckbox.isSelected
        settings.commandAutoFixCommands = (0 until tableModel.rowCount)
            .filter { tableModel.getValueAt(it, 0) as Boolean }
            .map { tableModel.getValueAt(it, 1) as String }
        // Update the global tool collection without removing deselected commands
        settings.commandAutoFixCommands!!.forEach { command ->
            if (!AppSettingsState.instance.executables.contains(command)) {
                AppSettingsState.instance.executables.add(command)
            }
        }
        super.doOKAction()
    }

    private fun setupCommandTable() {
        commandTable.columnModel.getColumn(0).apply {
            cellEditor = DefaultCellEditor(JCheckBox())
            preferredWidth = 60
            maxWidth = 60
        }
        tableModel.addTableModelListener { e ->
            if (e.column == 0) {
                val row = e.firstRow
                val value = tableModel.getValueAt(row, 0) as Boolean
                checkboxStates[row] = value
            }
        }
    }
}