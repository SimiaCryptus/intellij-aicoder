package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.simiacryptus.skyenet.core.platform.UsageInterface
import org.jdesktop.swingx.JXTable
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class UsageTable(
    val usage: UsageInterface
) : JPanel(BorderLayout()) {

    private val buttonPanel = JPanel()
    val columnNames = arrayOf("Model", "Prompt", "Completion", "Cost")

    val rowData by lazy {
        usage.getUserUsageSummary(IdeaOpenAIClient.localUser).map {
            listOf(
                it.key.modelName,
                it.value.prompt_tokens.toString(),
                it.value.completion_tokens.toString(),
               String.format("%.2f", it.value.cost)
            ).toMutableList()
        }.toMutableList()
    }

    private val dataModel by lazy {
        object : AbstractTableModel() {
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
                return rowData[row][col]
            }

            override fun isCellEditable(row: Int, column: Int): Boolean {
                return true
            }

            override fun setValueAt(value: Any, row: Int, col: Int) {
                rowData[row][col] = value.toString()
                fireTableCellUpdated(row, col)
            }

        }
    }

    private val jtable by lazy { JBTable(dataModel) }

    private val scrollpane by lazy { JBScrollPane(jtable) }


    private val clearButton by lazy {
        JButton(object : AbstractAction("Clear") {
            override fun actionPerformed(e: ActionEvent?) {
                rowData.clear()
                usage.clear()
                this@UsageTable.parent.invalidate()
            }
        })
    }

    init {
        jtable.columnModel.getColumn(0).cellRenderer = DefaultTableCellRenderer()
        jtable.columnModel.getColumn(1).cellRenderer = DefaultTableCellRenderer()
        jtable.columnModel.getColumn(2).cellRenderer = DefaultTableCellRenderer()
        jtable.columnModel.getColumn(3).cellRenderer = DefaultTableCellRenderer()

        val editor = object : JXTable.GenericEditor() {
            override fun isCellEditable(anEvent: EventObject?) = false
        }
        jtable.columnModel.getColumn(0).cellEditor = editor
        jtable.columnModel.getColumn(1).cellEditor = editor
        jtable.columnModel.getColumn(2).cellEditor = editor
        jtable.columnModel.getColumn(3).cellEditor = editor

        jtable.columnModel.getColumn(0).headerRenderer = DefaultTableCellRenderer()
        jtable.columnModel.getColumn(1).headerRenderer = DefaultTableCellRenderer()
        jtable.columnModel.getColumn(2).headerRenderer = DefaultTableCellRenderer()
        jtable.columnModel.getColumn(3).headerRenderer = DefaultTableCellRenderer()

        // Set the preferred width for the first column (checkboxes) to the header label width
        initCol(0)
        initCol(1)
        initCol(2)
        initCol(3)

        jtable.tableHeader.defaultRenderer = DefaultTableCellRenderer()

        add(scrollpane, BorderLayout.CENTER)
        buttonPanel.add(clearButton)
        add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun initCol(idx: Int) {
        val headerRenderer = jtable.tableHeader.defaultRenderer
        val headerValue = jtable.columnModel.getColumn(idx).headerValue
        val headerComp = headerRenderer.getTableCellRendererComponent(jtable, headerValue, false, false, 0, idx)
        jtable.columnModel.getColumn(idx).preferredWidth = headerComp.preferredSize.width
    }

    companion object {
    }
}
