package com.github.simiacryptus.aicoder.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.table.JBTable
import org.jdesktop.swingx.JXTable
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class ActionTable(
    val actionSettings: MutableList<ActionSettingsRegistry.ActionSettings>
) : JPanel(BorderLayout()) {

    fun read(registry: ActionSettingsRegistry) {
        registry.actionSettings.clear()
        rowData.map { row ->
            val copy = (actionSettings.find { it.id == row[2] })!!.copy(
                enabled = ((row[0] as String) == "true"),
                displayText = row[1] as String
            )
            registry.actionSettings.put(copy.id, copy)
        }
    }

    fun write(registry: ActionSettingsRegistry) {
        registry.actionSettings.values.forEach { actionSetting ->
            val row = rowData.find { it[2] == actionSetting.id }
            row?.let {
                actionSetting.enabled = (it[0] as String) == "true"
                actionSetting.displayText = it[1] as String
            }
        }
    }

    companion object {
        private val log = Logger.getInstance(ActionTable::class.java)
    }

    val buttonPanel = JPanel()
    val columnNames = arrayOf("Enabled", "Display Text", "ID")

    val rowData = actionSettings.map {
        listOf(it.enabled.toString(), it.displayText, it.id).toMutableList()
    }.toMutableList()

    val dataModel = object : AbstractTableModel() {
        override fun getColumnName(column: Int): String {
            return columnNames.get(column).toString()
        }

        override fun getRowCount(): Int {
            return rowData.size
        }

        override fun getColumnCount(): Int {
            return columnNames.size
        }

        override fun getValueAt(row: Int, col: Int): Any {
            return rowData[row][col]!!
        }

        override fun isCellEditable(row: Int, column: Int): Boolean {
            return true
        }

        override fun setValueAt(value: Any, row: Int, col: Int) {
            rowData[row][col] = value.toString()
            fireTableCellUpdated(row, col)
        }

    }

    val jtable = JBTable(dataModel)

    val scrollpane = JBScrollPane(jtable)

    val cloneButton = JButton(object : AbstractAction("Clone") {
        override fun actionPerformed(e: ActionEvent?) {

            if (jtable.selectedRows.size != 1) {
                JOptionPane.showMessageDialog(null, "Please select a single row to clone")
                return
            }

            val selectedRowIndex = jtable.selectedRow
            val selectedSettings = actionSettings.find {
                it.id == dataModel.getValueAt(selectedRowIndex, 2)
            }

            val panel = JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP))
            val classnameField = JTextField(100)
            classnameField.text = dataModel.getValueAt(selectedRowIndex, 2).toString()
            panel.add(with(JPanel(HorizontalLayout(2))) {
                add(JLabel("New class name:"))
                add(classnameField)
                this
            })
            val displayField = JTextField(100)
            displayField.text = dataModel.getValueAt(selectedRowIndex, 1).toString()
            panel.add(with(JPanel(HorizontalLayout(2))) {
                add(JLabel("New description:"))
                add(displayField)
                this
            })
            val options = arrayOf<Any>("OK", "Cancel")
            if (JOptionPane.showOptionDialog(
                    null,
                    panel,
                    "API Key",
                    JOptionPane.NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[1]
                ) == JOptionPane.OK_OPTION
            ) {
                if ((0 until dataModel.rowCount).toList().any { dataModel.getValueAt(it, 2) == classnameField.text }) {
                    JOptionPane.showMessageDialog(null, "Class name already exists")
                } else {
                    val newRow = mutableListOf<String?>()
                    newRow.add("true")
                    newRow.add(displayField.text)
                    newRow.add(classnameField.text)
                    val newSettings = selectedSettings!!.copy(
                        id = classnameField.text,
                        displayText = displayField.text,
                        enabled = true,
                        isDynamic = true
                    )
                    newSettings.file.writeText(
                        selectedSettings.file.readText().replace(
                            ("""(?<![\w\d])${selectedSettings.className}(?![\w\d])""").toRegex(),
                            newSettings.className
                        )
                    )
                    rowData.add(newRow)
                    actionSettings.add(newSettings)
                    this@ActionTable.parent.invalidate()
                }
            }
        }
    })

    val editButton = JButton((object : AbstractAction("Edit") {
        override fun actionPerformed(e: ActionEvent?) {
            val id = dataModel.getValueAt(jtable.selectedRow, 2)
            val actionSetting = actionSettings.find { it.id == id }
            actionSetting?.file?.let {
                val project = ApplicationManager.getApplication().runReadAction<Project> {
                    com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
                }
                if (it.exists()) {
                    ApplicationManager.getApplication().invokeLater {
                        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(it)
                        FileEditorManager.getInstance(project!!).openFile(virtualFile!!, true)
                    }
                } else {
                    log.warn("File not found: ${it.absolutePath}")
                }
            }
        }
    }))

    val removeButton = JButton(object : AbstractAction("Remove") {
        override fun actionPerformed(e: ActionEvent?) {
            if (jtable.selectedRows.size != 1) {
                JOptionPane.showMessageDialog(null, "Please select a single row to clone")
                return
            }
            val selectedRow = jtable.selectedRow
            val selectedSettings = actionSettings.find {
                it.id == dataModel.getValueAt(selectedRow, 2)
            }
            if (!(selectedSettings?.isDynamic ?: false)) {
                JOptionPane.showMessageDialog(null, "Cannot remove non-dynamic action")
                return
            }
            rowData.removeIf() {
                it[2] == selectedSettings?.id
            }
            this@ActionTable.parent.invalidate()
        }
    })

    init {
        jtable.columnModel.getColumn(0).cellRenderer = BooleanTableCellRenderer()
        jtable.columnModel.getColumn(1).cellRenderer = DefaultTableCellRenderer()
        jtable.columnModel.getColumn(2).cellRenderer = DefaultTableCellRenderer()

        jtable.columnModel.getColumn(0).cellEditor = BooleanTableCellEditor()
        jtable.columnModel.getColumn(1).cellEditor = JXTable.GenericEditor()
        jtable.columnModel.getColumn(2).cellEditor = object : JXTable.GenericEditor() {
            override fun isCellEditable(anEvent: EventObject?) = false
        }

        jtable.columnModel.getColumn(0).headerRenderer = DefaultTableCellRenderer()
        jtable.columnModel.getColumn(1).headerRenderer = DefaultTableCellRenderer()
        jtable.columnModel.getColumn(2).headerRenderer = DefaultTableCellRenderer()

        jtable.tableHeader.defaultRenderer = DefaultTableCellRenderer()

        add(scrollpane, BorderLayout.CENTER)
        buttonPanel.add(cloneButton)
        buttonPanel.add(editButton)
        buttonPanel.add(removeButton)
        add(buttonPanel, BorderLayout.SOUTH)
    }
}
