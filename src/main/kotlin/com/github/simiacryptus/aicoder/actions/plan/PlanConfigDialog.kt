package com.github.simiacryptus.aicoder.actions.plan

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.TaskSettingsBase
import com.simiacryptus.skyenet.apps.plan.TaskType
import java.awt.CardLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.table.DefaultTableModel

class PlanConfigDialog(
  project: Project?,
  val settings: PlanSettings,
) : DialogWrapper(project) {
  companion object {
    private const val MIN_TEMP = 0
    private const val MAX_TEMP = 100
    private const val DEFAULT_LIST_WIDTH = 150
    private const val DEFAULT_LIST_HEIGHT = 200
    private const val DEFAULT_PANEL_WIDTH = 350
    private const val DEFAULT_PANEL_HEIGHT = 200
    private const val TEMPERATURE_SCALE = 100.0
    private const val TEMPERATURE_LABEL = "%.2f"

    fun isVisible(it: ChatModel): Boolean {
      return AppSettingsState.instance.apiKey
        ?.filter { it.value.isNotBlank() }
        ?.keys
        ?.contains(it.provider.name)
        ?: false
    }
  }

  private data class CommandTableEntry(
    var enabled: Boolean,
    val command: String
  )

  private val temperatureSlider = JSlider(MIN_TEMP, MAX_TEMP, (settings.temperature * TEMPERATURE_SCALE).toInt()).apply {
    addChangeListener {
      settings.temperature = value / TEMPERATURE_SCALE
      temperatureLabel.text = TEMPERATURE_LABEL.format(settings.temperature)
    }
  }
  private val temperatureLabel = JLabel(TEMPERATURE_LABEL.format(settings.temperature))
  private val autoFixCheckbox = JCheckBox("Auto-apply fixes", settings.autoFix)
  private val allowBlockingCheckbox = JCheckBox("Allow blocking", settings.allowBlocking)
  private val taskTypeList = JBList(TaskType.values())
  private val configPanelContainer = JPanel(CardLayout())
  private val taskConfigs = mutableMapOf<String, TaskTypeConfigPanel>()
  private val singleTaskModeCheckbox = JCheckBox("Single Task Mode", false)
  private val removeCommandButton = JButton("Remove Command").apply {
    isEnabled = false
  }

  private inner class TaskTypeListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>?,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      if (component is JLabel && value is TaskType<*, *>) {
        // Set tooltip with detailed HTML description
        toolTipText = getTaskTooltip(value)
        // Access settings directly through the outer class
        val isEnabled = settings.getTaskSettings(value).enabled
        // Set font style and color based on enabled status
        font = when (isEnabled) {
          true -> {
            font.deriveFont(Font.BOLD).deriveFont(14f)
          }

          false -> {
            font.deriveFont(Font.ITALIC).deriveFont(12f)
          }
        }
        foreground = if (isEnabled) {
          list?.foreground
        } else {
          list?.foreground?.darker()?.darker()
        }
        // Set task name and description
        text = buildString {
          val taskDescription = getTaskDescription(value)
          append(value.name)
          if (taskDescription.isNotEmpty()) {
            append(" - ")
            append(taskDescription)
          }
        }
      }
      return component
    }

    private fun getTaskTooltip(taskType: TaskType<*, *>): String = """
      <html>
      <body style='width: 300px; padding: 5px;'>
      <h3>${taskType.name}</h3>
      <p>${
      when (taskType) {
        TaskType.PerformanceAnalysis -> """
          Analyzes code performance and provides optimization recommendations.
          <ul>
            <li>Identifies performance bottlenecks and hotspots</li>
            <li>Measures execution time and resource usage</li>
            <li>Suggests algorithmic and structural improvements</li>
            <li>Provides quantitative performance metrics</li>
            <li>Recommends caching and optimization strategies</li>
          </ul>
        """

        TaskType.WebFetchAndTransform -> """
          Fetches content from web URLs and transforms it into desired formats.
          <ul>
            <li>Downloads and cleans HTML content</li>
            <li>Converts content to specified formats</li>
            <li>Handles content size limitations</li>
            <li>Supports custom transformation goals</li>
            <li>Integrates with markdown rendering</li>
          </ul>
        """

        TaskType.GitHubSearch -> """
          Performs comprehensive searches across GitHub's content.
          <ul>
            <li>Searches repositories, code, and issues</li>
            <li>Supports advanced search queries</li>
            <li>Filters results by various criteria</li>
            <li>Formats results with relevant details</li>
            <li>Handles API rate limiting</li>
          </ul>
        """

        TaskType.GoogleSearch -> """
          Executes Google web searches with customizable parameters.
          <ul>
            <li>Uses Google Custom Search API</li>
            <li>Supports result count configuration</li>
            <li>Includes metadata and snippets</li>
            <li>Formats results in markdown</li>
            <li>Handles URL encoding and safety</li>
          </ul>
        """

        TaskType.Search -> """
          Performs pattern-based searches across project files with context.
          <ul>
            <li>Supports both substring and regex search patterns</li>
            <li>Shows configurable context lines around matches</li>
            <li>Groups results by file with line numbers</li>
            <li>Filters for text-based files automatically</li>
            <li>Provides organized, readable output format</li>
          </ul>
        """

        TaskType.EmbeddingSearch -> """
          Performs semantic search using AI embeddings across indexed content.
          <ul>
            <li>Uses OpenAI embeddings for semantic matching</li>
            <li>Supports positive and negative search queries</li>
            <li>Configurable similarity metrics and thresholds</li>
            <li>Regular expression filtering capabilities</li>
            <li>Returns ranked results with context</li>
          </ul>
        """

        TaskType.KnowledgeIndexing -> """
          Indexes documents and code for semantic search capabilities.
          <ul>
            <li>Processes both documentation and source code</li>
            <li>Creates searchable content chunks</li>
            <li>Supports parallel processing</li>
            <li>Configurable chunking strategies</li>
            <li>Progress tracking and reporting</li>
          </ul>
        """

        TaskType.WebSearchAndIndex -> """
          Performs web searches and indexes results for future reference.
          <ul>
            <li>Integrates with Google Custom Search</li>
            <li>Downloads and processes search results</li>
            <li>Creates searchable indexes</li>
            <li>Handles content download and storage</li>
            <li>Supports batch processing</li>
          </ul>
        """

        TaskType.ForeachTask -> """
          Executes a set of subtasks for each item in a given list.
          <ul>
            <li>Handles sequential item processing</li>
            <li>Maintains subtask dependencies</li>
            <li>Supports parallel execution within items</li>
            <li>Provides progress tracking</li>
            <li>Configurable subtask definitions</li>
          </ul>
        """

        TaskType.CommandSession -> """
          Manages interactive command-line sessions with state persistence.
          <ul>
            <li>Creates and maintains command sessions</li>
            <li>Supports multiple concurrent sessions</li>
            <li>Configurable timeouts and cleanup</li>
            <li>Session state preservation</li>
            <li>Comprehensive output capture</li>
          </ul>
        """

        TaskType.SeleniumSession -> """
          Automates browser interactions using Selenium WebDriver.
          <ul>
            <li>Headless Chrome browser automation</li>
            <li>JavaScript command execution</li>
            <li>Session management capabilities</li>
            <li>Configurable timeouts</li>
            <li>Detailed execution results</li>
          </ul>
        """

        TaskType.RunShellCommand -> """
          Executes shell commands in a controlled environment.
          <ul>
            <li>Safe command execution handling</li>
            <li>Working directory configuration</li>
            <li>Output capture and formatting</li>
            <li>Error handling and reporting</li>
            <li>Interactive result review</li>
          </ul>
        """

        TaskType.TaskPlanning -> """
          Orchestrates complex development tasks by breaking them down into manageable subtasks.
          <ul>
            <li>Analyzes project requirements and constraints to create optimal task sequences</li>
            <li>Establishes clear task dependencies and relationships between components</li>
            <li>Optimizes task ordering for maximum parallel execution efficiency</li>
            <li>Provides interactive visual dependency graphs for progress tracking</li>
            <li>Supports both fully automated and interactive planning modes</li>
            <li>Estimates task complexity and resource requirements</li>
            <li>Identifies critical paths and potential bottlenecks</li>
          </ul>
        """

        TaskType.Inquiry -> """
          Provides detailed answers and insights about code implementation by analyzing specified files.
          <ul>
            <li>Answers detailed questions about code functionality and implementation</li>
            <li>Analyzes code patterns, relationships and architectural decisions</li>
            <li>Supports interactive discussions and follow-up questions in blocking mode</li>
            <li>Generates comprehensive markdown reports with code examples</li>
            <li>Handles multiple files and complex cross-reference queries</li>
            <li>Provides context-aware technical recommendations</li>
            <li>Explains trade-offs and rationale behind implementation choices</li>
          </ul>
        """

        TaskType.FileModification -> """
          Creates or modifies source files with AI assistance while maintaining code quality.
          <ul>
            <li>Shows proposed changes in diff format for easy review</li>
            <li>Supports both automated application and manual approval modes</li>
            <li>Maintains project coding standards and style consistency</li>
            <li>Handles complex multi-file operations and refactoring</li>
            <li>Provides clear documentation of all changes with rationale</li>
            <li>Implements proper error handling and edge cases</li>
            <li>Updates imports and dependencies automatically</li>
            <li>Preserves existing code formatting and structure</li>
          </ul>
        """

        TaskType.Documentation -> """
          Generates comprehensive documentation for code files and APIs.
          <ul>
            <li>Handles both inline comments and markdown files</li>
            <li>Generates detailed API documentation</li>
            <li>Documents design decisions and rationale</li>
            <li>Supports interactive approval workflow</li>
            <li>Maintains documentation consistency</li>
          </ul>
        """

        TaskType.CodeReview -> """
          Performs automated code reviews focusing on quality and best practices.
          <ul>
            <li>Analyzes code quality and potential issues</li>
            <li>Identifies bugs and performance problems</li>
            <li>Reviews security vulnerabilities</li>
            <li>Suggests specific improvements</li>
            <li>Provides actionable recommendations</li>
          </ul>
        """

        TaskType.TestGeneration -> """
          Creates comprehensive test suites for code reliability and correctness.
          <ul>
            <li>Generates unit and integration tests</li>
            <li>Creates positive and negative test cases</li>
            <li>Tests edge cases and boundary conditions</li>
            <li>Follows language-specific testing practices</li>
            <li>Organizes tests in appropriate directories</li>
          </ul>
        """

        TaskType.Optimization -> """
          Analyzes and optimizes code performance while maintaining readability.
          <ul>
            <li>Identifies performance bottlenecks</li>
            <li>Suggests algorithmic improvements</li>
            <li>Analyzes memory usage patterns</li>
            <li>Recommends caching strategies</li>
            <li>Provides impact estimates</li>
          </ul>
        """

        TaskType.SecurityAudit -> """
          Performs security analysis to identify and fix vulnerabilities.
          <ul>
            <li>Analyzes security vulnerabilities</li>
            <li>Reviews authentication/authorization</li>
            <li>Checks data handling practices</li>
            <li>Provides security recommendations</li>
            <li>Generates detailed audit reports</li>
          </ul>
        """

        TaskType.RefactorTask -> """
          Analyzes and improves code structure while maintaining functionality.
          <ul>
            <li>Suggests structural improvements</li>
            <li>Reduces code complexity</li>
            <li>Improves naming conventions</li>
            <li>Enhances code organization</li>
            <li>Shows changes in diff format</li>
          </ul>
        """

        else -> "No detailed description available"
      }
    }</p>
      </body>
      </html>
    """


    private fun getTaskDescription(taskType: TaskType<*, *>): String = when (taskType) {
      TaskType.Search -> "Search project files using patterns with contextual results"
      TaskType.EmbeddingSearch -> "Perform semantic search using AI embeddings"
      TaskType.KnowledgeIndexing -> "Index content for semantic search capabilities"
      TaskType.WebSearchAndIndex -> "Search web content and create searchable indexes"
      TaskType.ForeachTask -> "Execute subtasks for each item in a list"
      TaskType.CommandSession -> "Manage interactive command-line sessions"
      TaskType.SeleniumSession -> "Automate browser interactions with Selenium"
      TaskType.RunShellCommand -> "Execute shell commands safely"
      TaskType.TaskPlanning -> "Break down and coordinate complex development tasks with dependency management"
      TaskType.Inquiry -> "Analyze code and provide detailed explanations of implementation patterns"
      TaskType.FileModification -> "Create new files or modify existing code with AI-powered assistance"
      TaskType.Documentation -> "Generate comprehensive documentation for code, APIs, and architecture"
      TaskType.CodeReview -> "Perform thorough code review with quality and best practice analysis"
      TaskType.TestGeneration -> "Generate comprehensive test suites with full coverage analysis"
      TaskType.Optimization -> "Analyze performance bottlenecks and implement optimizations"
      TaskType.SecurityAudit -> "Identify security vulnerabilities and provide mitigation strategies"
      TaskType.RefactorTask -> "Improve code structure, readability and maintainability"
      TaskType.PerformanceAnalysis -> "Analyze and optimize code performance with detailed metrics"
      TaskType.WebFetchAndTransform -> "Fetch and transform web content into desired formats"
      TaskType.GitHubSearch -> "Search GitHub repositories, code, issues and users"
      TaskType.GoogleSearch -> "Perform Google web searches with custom filtering"
      else -> "No description available"
    }
  }

  private fun getVisibleModels() =
    ChatModel.values().map { it.value }.filter { isVisible(it) }.toList()
      .sortedBy { "${it.provider.name} - ${it.modelName}" }

  private inner class TaskTypeConfigPanel(val taskType: TaskType<*, *>) : JPanel() {
    val enabledCheckbox = JCheckBox("Enabled", settings.getTaskSettings(taskType).enabled)
    private val modelComboBox = ComboBox(getVisibleModels().map { it.modelName }.toTypedArray()).apply {
      maximumSize = Dimension(DEFAULT_PANEL_WIDTH - 50, 30)
      preferredSize = Dimension(DEFAULT_PANEL_WIDTH - 50, 30)
    }

    init {
      removeCommandButton.addActionListener {
        val selectedRow = commandTable.selectedRow
        if (selectedRow != -1) {
          val command = tableModel.getValueAt(selectedRow, 1) as String
          val confirmResult = JOptionPane.showConfirmDialog(
            null,
            "Remove command: $command?",
            "Confirm Remove",
            JOptionPane.YES_NO_OPTION
          )
          if (confirmResult == JOptionPane.YES_OPTION) {
            tableModel.removeRow(selectedRow)
            checkboxStates.removeAt(selectedRow)
            AppSettingsState.instance.executables.remove(command)
          }
        }
      }
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      alignmentX = Component.LEFT_ALIGNMENT
      add(enabledCheckbox.apply { alignmentX = Component.LEFT_ALIGNMENT })
      add(Box.createVerticalStrut(5))
      add(JLabel("Model:").apply { alignmentX = Component.LEFT_ALIGNMENT })
      add(Box.createVerticalStrut(2))
      add(modelComboBox.apply { alignmentX = Component.LEFT_ALIGNMENT })
      add(Box.createVerticalGlue())
      val currentModel = settings.getTaskSettings(taskType).model
      modelComboBox.selectedItem = currentModel?.modelName
      enabledCheckbox.addItemListener {
        // Update the settings immediately when checkbox state changes
        settings.setTaskSettings(taskType, TaskSettingsBase(taskType.name, enabledCheckbox.isSelected).apply {
          this.model = getVisibleModels().find { it.modelName == modelComboBox.selectedItem }
        })
        taskTypeList.repaint()
      }
      modelComboBox.addActionListener {
        settings.setTaskSettings(taskType, TaskSettingsBase(taskType.name, enabledCheckbox.isSelected).apply {
          this.model = getVisibleModels().find { it.modelName == modelComboBox.selectedItem }
        })
      }
    }

    fun saveSettings() {
      // Only need to save model selection since enabled state is saved immediately
      settings.setTaskSettings(taskType, TaskSettingsBase(taskType.name, enabledCheckbox.isSelected).apply {
        this.model = getVisibleModels().find { it.modelName == modelComboBox.selectedItem }
      })
    }
  }

  private val checkboxStates = AppSettingsState.instance.executables.map { true }.toMutableList()
  private val tableModel = object : DefaultTableModel(arrayOf("Enabled", "Command"), 0) {
    private val entries = mutableListOf<CommandTableEntry>()

    init {
      AppSettingsState.instance.executables.forEach { command ->
        entries.add(CommandTableEntry(true, command))
        addRow(arrayOf(true, command))
      }
    }

    override fun getColumnClass(columnIndex: Int) = when (columnIndex) {
      0 -> java.lang.Boolean::class.java
      else -> super.getColumnClass(columnIndex)
    }

    override fun isCellEditable(row: Int, column: Int) = column == 0

    override fun setValueAt(aValue: Any?, row: Int, column: Int) {
      if (column == 0 && aValue is Boolean) {
        entries[row].enabled = aValue
        super.setValueAt(aValue, row, column)
        fireTableCellUpdated(row, column)
      } else {
        throw IllegalArgumentException("Invalid column index: $column")
      }
    }

    override fun getValueAt(row: Int, column: Int): Any =
      try {
        if (column == 0) {
          entries[row].enabled
        } else super.getValueAt(row, column)
      } catch (e: IndexOutOfBoundsException) {
        false
      }

  }
  private val commandTable = JBTable(tableModel).apply {
    putClientProperty("terminateEditOnFocusLost", true)
    this.columnModel.getColumn(0).apply {
      preferredWidth = 50
      maxWidth = 100
    }
  }
  private val addCommandButton = JButton("Add Command")
  private val editCommandButton = JButton("Edit Command")

  init {
    // Set the custom cell renderer for the task type list
    taskTypeList.cellRenderer = TaskTypeListCellRenderer()

    taskTypeList.addListSelectionListener { e ->
      if (!e.valueIsAdjusting) {
        val selectedType = (taskTypeList.selectedValue as TaskType<*, *>).name
        (configPanelContainer.layout as CardLayout).show(configPanelContainer, selectedType)
        if (singleTaskModeCheckbox.isSelected) {
          // Disable all tasks except the selected one
          TaskType.values().forEach { taskType ->
            val isSelected = taskType.name == selectedType
            taskConfigs[taskType.name]?.enabledCheckbox?.isSelected = isSelected
          }
        }
      }
    }
    // Initialize config panels for each task type
    TaskType.values().forEach { taskType ->
      val configPanel = TaskTypeConfigPanel(taskType)
      taskConfigs[taskType.name] = configPanel
      configPanelContainer.add(configPanel, taskType.name)
    }

    init()
    title = "Configure Plan Ahead Action"
    // Add listener for single task mode checkbox
    singleTaskModeCheckbox.addItemListener { e ->
      if (e.stateChange == java.awt.event.ItemEvent.SELECTED) {
        // When enabled, only keep the currently selected task enabled
        val selectedType = (taskTypeList.selectedValue as TaskType<*, *>).name
        TaskType.values().forEach { taskType ->
          val isSelected = taskType.name == selectedType
          taskConfigs[taskType.name]?.enabledCheckbox?.isSelected = isSelected
        }
      }
    }
    // Add model combobox and change listener to update the settings based on slider value

    temperatureSlider.addChangeListener {
      settings.temperature = temperatureSlider.value / 100.0
    }
    // Update parsingModel based on modelComboBox selection
    val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
      .withTitle("Select Command")
      .withDescription("Choose an executable file for the auto-fix command")
    addCommandButton.addActionListener {
      val newCommand = if ((it as? java.awt.event.ActionEvent)?.modifiers?.and(java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
        // Control was held - prompt for direct path input
        JOptionPane.showInputDialog(
          null,
          "Enter command path:",
          "Add Command",
          JOptionPane.PLAIN_MESSAGE
        )
      } else {
        // Normal click - show file chooser
        FileChooser.chooseFile(fileChooserDescriptor, project, null)?.path
      }
      if (newCommand != null) {
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
      preferredWidth = 50
      maxWidth = 100
    }
    commandTable.selectionModel.addListSelectionListener {
      editCommandButton.isEnabled = commandTable.selectedRow != -1
      removeCommandButton.isEnabled = commandTable.selectedRow != -1
    }
    editCommandButton.isEnabled = false
    // Call setupCommandTable to initialize commandTable
    setupCommandTable()
  }

  override fun createCenterPanel(): JComponent = panel {
    group("Tasks") {
      row {
        cell(singleTaskModeCheckbox)
          .align(Align.FILL)
      }
      row {
        cell(
          JBSplitter(false, 0.3f).apply {
            firstComponent = JBScrollPane(taskTypeList).apply {
              minimumSize = Dimension(DEFAULT_LIST_WIDTH, DEFAULT_LIST_HEIGHT)
              preferredSize = Dimension(DEFAULT_LIST_WIDTH + 100, DEFAULT_LIST_HEIGHT)
            }
            secondComponent = JBScrollPane(configPanelContainer).apply {
              minimumSize = Dimension(DEFAULT_PANEL_WIDTH, DEFAULT_PANEL_HEIGHT / 2)
              preferredSize = Dimension(DEFAULT_PANEL_WIDTH, DEFAULT_PANEL_HEIGHT)
            }
            dividerWidth = 3
            isShowDividerControls = true
            isShowDividerIcon = true
          }
        )
          .align(Align.FILL)
          .resizableColumn()
      }
        .resizableRow()
    }
      .layout(RowLayout.PARENT_GRID)
      .resizableRow()
    group("Settings") {
      row {
        cell(autoFixCheckbox)
          .align(Align.FILL)
      }
      row {
        cell(allowBlockingCheckbox)
          .align(Align.FILL)
      }
      row("Temperature:") {
        cell(temperatureSlider).align(Align.FILL)
        cell(temperatureLabel)
      }
      row("Command Line Tools:") {
        cell(
          JBSplitter(true, 0.9f).apply {
            firstComponent = JBScrollPane(commandTable).apply {
              minimumSize = Dimension(350, 100)
              preferredSize = Dimension(350, 200)
            }
            secondComponent = JPanel().apply {
              layout = BoxLayout(this, BoxLayout.X_AXIS)
              add(addCommandButton)
              add(Box.createHorizontalStrut(5))
              add(editCommandButton)
              add(Box.createHorizontalStrut(5))
              add(removeCommandButton)
            }
            dividerWidth = 3
            isShowDividerControls = true
            isShowDividerIcon = true
            splitterProportionKey = "planAhead.commands.splitter"
          })
          .align(Align.FILL)
          .resizableColumn()
      }
    }
  }

  override fun doOKAction() {
    // Save settings from all task type config panels
    taskConfigs.values.forEach { configPanel ->
      configPanel.saveSettings()
    }
    settings.autoFix = autoFixCheckbox.isSelected
    settings.allowBlocking = allowBlockingCheckbox.isSelected
    settings.commandAutoFixCommands = (0 until tableModel.rowCount)
      .filter { tableModel.getValueAt(it, 0) as Boolean }
      .map { tableModel.getValueAt(it, 1) as String }
    // Update the global tool collection
    settings.commandAutoFixCommands?.forEach { command ->
      if (!AppSettingsState.instance.executables.contains(command)) {
        AppSettingsState.instance.executables.add(command)
      }
    }
    super.doOKAction()
  }

  private fun setupCommandTable() {
    commandTable.columnModel.getColumn(0).apply {
      cellEditor = DefaultCellEditor(JCheckBox())
      preferredWidth = 50
      maxWidth = 100
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