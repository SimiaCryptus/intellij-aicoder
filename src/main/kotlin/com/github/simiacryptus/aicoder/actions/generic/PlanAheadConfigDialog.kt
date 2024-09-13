package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.simiacryptus.skyenet.apps.plan.TaskType
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.apps.plan.TaskSettings
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class PlanAheadConfigDialog(
    project: Project?,
    val settings: PlanSettings,
) : DialogWrapper(project) {
    private val modelComboBox: ComboBox<String> = ComboBox(
        ChatModels.values().toList().toTypedArray<Pair<String, ChatModels>>().map { it.first }.toTypedArray())

    private val temperatureSlider = JSlider(0, 100, (settings.temperature * 100).toInt())

    private val autoFixCheckbox = JCheckBox("Auto-apply fixes", settings.autoFix)
    private val taskTableModel = object : DefaultTableModel(arrayOf("Enabled", "Task Type"), 0) {
        override fun getColumnClass(columnIndex: Int) = when (columnIndex) {
            0 -> java.lang.Boolean::class.java
            else -> super.getColumnClass(columnIndex)
        }
        override fun isCellEditable(row: Int, column: Int) = column == 0
    }
    private val taskTable = JBTable(taskTableModel).apply { putClientProperty("terminateEditOnFocusLost", true) }

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
        // Initialize task table
        TaskType.values().forEach { taskType ->
            val taskSettings = settings.getTaskSettings(taskType)
            taskTableModel.addRow(arrayOf(taskSettings.enabled, taskType.name))
        }
        taskTable.columnModel.getColumn(0).preferredWidth = 60
        taskTable.columnModel.getColumn(0).maxWidth = 60
        taskTable.columnModel.getColumn(1).preferredWidth = 200
    }


    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("Model:"))
        panel.add(modelComboBox)
        val indexOfFirst = ChatModels.values().toList().toTypedArray<Pair<String, ChatModels>>().indexOfFirst {
            it.second.name == settings.model.modelName || it.second.modelName == settings.model.modelName || it.first == settings.model.modelName
        }
        modelComboBox.selectedIndex = indexOfFirst
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
        settings.model = (modelComboBox.selectedItem as String).chatModel()
        // Update task settings
        for (i in 0 until taskTableModel.rowCount) {
            settings.setTaskSettings(TaskType.valueOf(taskTableModel.getValueAt(i, 1) as String), TaskSettings(taskTableModel.getValueAt(i, 0) as Boolean))
        }
        
        settings.autoFix = autoFixCheckbox.isSelected
        settings.commandAutoFixCommands = (0 until tableModel.rowCount)
            .filter { tableModel.getValueAt(it, 0) as Boolean }
            .map { tableModel.getValueAt(it, 1) as String }
        settings.enableCommandAutoFix = settings.commandAutoFixCommands?.isNotEmpty() ?: false
        // Update the global tool collection
        AppSettingsState.instance.executables.clear()
        AppSettingsState.instance.executables.addAll((0 until tableModel.rowCount).map {
            tableModel.getValueAt(it, 1) as String
        })
        super.doOKAction()
    }
}