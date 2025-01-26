package com.simiacryptus.aicoder.config

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.simiacryptus.aicoder.util.IdeaChatClient
import com.simiacryptus.skyenet.core.platform.model.UsageInterface
import org.jdesktop.swingx.JXTable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class UsageTable(
  val usage: UsageInterface
) : JPanel(BorderLayout()) {

  private val buttonPanel = JPanel()
  val columnNames = arrayOf("Model", "Prompt", "Completion", "Cost")

  val rowData by lazy {
    val usageData = usage.getUserUsageSummary(IdeaChatClient.localUser).map { entry ->
      listOf(
        entry.key.modelName,
        entry.value.prompt_tokens.toString(),
        entry.value.completion_tokens.toString(),
        String.format("%.2f", entry.value.cost)
      ).toMutableList()
    }
    // Calculate totals
    val totalPromptTokens = usageData.sumOf { it[1].toString().toInt() }
    val totalCompletionTokens = usageData.sumOf { it[2].toString().toInt() }
    val totalCost = usageData.sumOf { it[3].toString().toDouble() }
    // Add totals row
    (usageData + listOf(
      listOf(
        "TOTAL",
        totalPromptTokens.toString(),
        totalCompletionTokens.toString(),
        String.format("%.2f", totalCost)
      ).toMutableList()
    )).toMutableList()
  }

  private val dataModel by lazy {
    object : AbstractTableModel() {
      override fun getColumnName(column: Int): String {
        return columnNames.get(column).toString()
      }

      override fun getValueAt(row: Int, col: Int): Any {
        return rowData[row][col]
      }

      override fun isCellEditable(row: Int, column: Int): Boolean {
        // Make the total row non-editable
        return row != rowData.size - 1
      }

      override fun getRowCount(): Int {
        return rowData.size
      }

      override fun getColumnCount(): Int {
        return columnNames.size
      }


      override fun setValueAt(value: Any, row: Int, col: Int) {
        // Prevent editing total row
        if (row == rowData.size - 1) return
        val strings = rowData[row]
        strings[col] = value.toString()
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
    // Custom renderer for the total row
    val totalRowRenderer = object : DefaultTableCellRenderer() {
      override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
      ): Component {
        val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (row == table?.model?.rowCount?.minus(1)) {
          font = font.deriveFont(font.style or java.awt.Font.BOLD)
        }
        return c
      }
    }

    jtable.columnModel.getColumn(0).cellRenderer = DefaultTableCellRenderer()
    jtable.columnModel.getColumn(1).cellRenderer = DefaultTableCellRenderer()
    jtable.columnModel.getColumn(2).cellRenderer = DefaultTableCellRenderer()
    jtable.columnModel.getColumn(3).cellRenderer = DefaultTableCellRenderer()
    // Apply the total row renderer to all columns
    for (i in 0..3) {
      val column = jtable.columnModel.getColumn(i)
      column.cellRenderer = totalRowRenderer
    }


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

  companion object
}