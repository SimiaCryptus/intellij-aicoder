package aicoder.actions.plan

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
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.TaskSettingsBase
import com.simiacryptus.skyenet.apps.plan.TaskType
import com.simiacryptus.skyenet.apps.plan.tools.CommandAutoFixTask
import java.awt.CardLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.table.DefaultTableModel

class PlanConfigDialog(
  project: Project?,
  val settings: PlanSettings,
  val singleTaskMode: Boolean = false,
) : DialogWrapper(project) {
  companion object {
    private const val CONFIG_COMBO_WIDTH = 200
    private const val CONFIG_COMBO_HEIGHT = 30
    private const val MIN_TEMP = 0
    private const val MAX_TEMP = 100
    private const val DEFAULT_LIST_WIDTH = 150
    private const val DEFAULT_LIST_HEIGHT = 200
    private const val DEFAULT_PANEL_WIDTH = 350
    private const val DEFAULT_PANEL_HEIGHT = 200
    private const val TEMPERATURE_SCALE = 100.0
    private const val TEMPERATURE_LABEL = "%.2f"
    private const val FONT_SIZE_ENABLED = 14f
    private const val FONT_SIZE_DISABLED = 12f
    private const val DIVIDER_PROPORTION = 0.3f

    fun isVisible(it: ChatModel): Boolean {
      return AppSettingsState.instance.apiKey?.get(it.provider.name)?.isNotBlank() ?: false
    }
  }

  private fun validateModelSelection(taskType: TaskType<*, *>, model: ChatModel?): Boolean {
    if (model == null && settings.getTaskSettings(taskType).enabled) {
      JOptionPane.showMessageDialog(
        null, "Please select a model for enabled task: ${taskType.name}", "Model Required", JOptionPane.WARNING_MESSAGE
      )
      return false
    }
    return true
  }

  private fun validateConfigName(name: String?) = when {
    name.isNullOrBlank() -> {
      JOptionPane.showMessageDialog(
        null, "Please enter a valid configuration name", "Invalid Name", JOptionPane.WARNING_MESSAGE
      )
      false
    }

    name.contains(Regex("[^a-zA-Z0-9_-]")) -> {
      JOptionPane.showMessageDialog(
        null, "Configuration name can only contain letters, numbers, underscores and hyphens", "Invalid Name", JOptionPane.WARNING_MESSAGE
      )
      false
    }

    else -> true
  }

  private inner class TaskTypeListCellRenderer : DefaultListCellRenderer() {
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

        else -> ""
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
      else -> ""
    }

    override fun getListCellRendererComponent(
      list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
      val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      if (component is JLabel && value is TaskType<*, *>) {
        toolTipText = getTaskTooltip(value)
        val isEnabled = settings.getTaskSettings(value).enabled
        font = when (isEnabled) {

          true -> font.deriveFont(Font.BOLD + Font.PLAIN, FONT_SIZE_ENABLED)
          false -> font.deriveFont(Font.ITALIC + Font.PLAIN, FONT_SIZE_DISABLED)
        }
        foreground = if (isEnabled) {
          list?.foreground
        } else {
          list?.foreground?.darker()?.darker()
        }
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
  }

  private inner class TaskTypeConfigPanel(val taskType: TaskType<*, *>) : JPanel() {
    val enabledCheckbox = JCheckBox("Enabled", settings.getTaskSettings(taskType).enabled)
    val modelComboBox = ComboBox(getVisibleModels().distinctBy { it.modelName }.map { it.modelName }.toTypedArray()).apply {
      maximumSize = Dimension(DEFAULT_PANEL_WIDTH - 50, 30)
      preferredSize = Dimension(DEFAULT_PANEL_WIDTH - 50, 30)
      if (itemCount > 0) {
        val currentModel = settings.getTaskSettings(taskType).model
        selectedItem = when {
          currentModel != null -> currentModel.modelName
          else -> defaultModel
        }
      }
    }
    private val commandList = if (taskType == TaskType.CommandAutoFix) {
      JBTable(object : DefaultTableModel(
        arrayOf("Enabled", "Command"), 0
      ) {

        private val entries = mutableListOf<CommandTableEntry>()

        init {
          val sortedExecutables = AppSettingsState.instance.executables.sortedWith(String.CASE_INSENSITIVE_ORDER)
          sortedExecutables.forEach { command ->
            val isEnabled =
              (settings.getTaskSettings(taskType) as? CommandAutoFixTask.CommandAutoFixTaskSettings)?.commandAutoFixCommands?.contains(command) ?: true
            entries.add(CommandTableEntry(isEnabled, command))
            addRow(arrayOf(isEnabled, command))
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
            updateCommandSettings()
            taskTypeList.repaint()
          } else {
            throw IllegalArgumentException("Invalid column index: $column")
          }
        }

        private fun updateCommandSettings() {
          val newSettings = CommandAutoFixTask.CommandAutoFixTaskSettings(
            taskType.name,
            settings.getTaskSettings(taskType).enabled,
            getVisibleModels().find { it.modelName == modelComboBox.selectedItem },
            entries.filter { it.enabled }.map { it.command })
          settings.setTaskSettings(taskType, newSettings)
        }
      }).apply {
        preferredScrollableViewportSize = Dimension(DEFAULT_PANEL_WIDTH - 50, 100)
        columnModel.getColumn(0).apply {
          preferredWidth = 50
          maxWidth = 100
          cellEditor = DefaultCellEditor(JCheckBox())
          headerValue = "<html>Enable/disable<br>command</html>"
        }
        columnModel.getColumn(1).apply {
          headerValue = "<html>Command path<br>or name</html>"
        }
      }
    } else null

    init {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      alignmentX = Component.LEFT_ALIGNMENT
      border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
      add(enabledCheckbox.apply { alignmentX = Component.LEFT_ALIGNMENT })
      add(Box.createVerticalStrut(5))
      add(JLabel("Model:").apply { alignmentX = Component.LEFT_ALIGNMENT })
      add(Box.createVerticalStrut(2))
      add(modelComboBox.apply { alignmentX = Component.LEFT_ALIGNMENT })
      if (commandList != null) {
        add(Box.createVerticalStrut(10))
        add(JLabel("Available Commands:").apply { alignmentX = Component.LEFT_ALIGNMENT })
        add(Box.createVerticalStrut(2))
        add(JBScrollPane(commandList).apply {
          alignmentX = Component.LEFT_ALIGNMENT
          preferredSize = Dimension(DEFAULT_PANEL_WIDTH - 50, DEFAULT_LIST_HEIGHT / 2)
          maximumSize = Dimension(DEFAULT_PANEL_WIDTH - 50, DEFAULT_LIST_HEIGHT / 2)
        })
        add(Box.createVerticalStrut(5))
        add(JPanel().apply {
          layout = BoxLayout(this, BoxLayout.X_AXIS)
          alignmentX = Component.LEFT_ALIGNMENT
          maximumSize = Dimension(DEFAULT_PANEL_WIDTH - 50, 30)
          add(JButton("Add Command").apply {
            maximumSize = Dimension(DEFAULT_PANEL_WIDTH / 2 - 30, 30)
            addActionListener {
              val command = JOptionPane.showInputDialog(
                this, "Enter command path:", "Add Command", JOptionPane.PLAIN_MESSAGE
              )
              if (command != null && command.isNotEmpty()) {
                (commandList.model as DefaultTableModel).addRow(arrayOf(true, command))
                AppSettingsState.instance.executables.add(command)
              }
            }
          })
          add(Box.createHorizontalStrut(5))
          add(JButton("Remove Command").apply {
            maximumSize = Dimension(DEFAULT_PANEL_WIDTH / 2 - 30, 30)
            addActionListener {
              val selectedRow = commandList.selectedRow
              if (selectedRow != -1) {
                val command = (commandList.model as DefaultTableModel).getValueAt(selectedRow, 1) as String
                val confirmResult = JOptionPane.showConfirmDialog(
                  null, "Remove command: $command?", "Confirm Remove", JOptionPane.YES_NO_OPTION
                )
                if (confirmResult == JOptionPane.YES_OPTION) {
                  (commandList.model as DefaultTableModel).removeRow(selectedRow)
                  AppSettingsState.instance.executables.remove(command)
                }
              } else {
                JOptionPane.showMessageDialog(
                  null, "Please select a command to remove."
                )
              }
            }
          })
        })
      }

      val currentModel = settings.getTaskSettings(taskType).model
      modelComboBox.selectedItem = currentModel?.modelName ?: defaultModel
      enabledCheckbox.addItemListener {
        val newSettings = when (taskType) {
          TaskType.CommandAutoFix -> CommandAutoFixTask.CommandAutoFixTaskSettings(
            taskType.name,
            enabledCheckbox.isSelected,
            getVisibleModels().find { it.modelName == modelComboBox.selectedItem },
            (0 until (commandList?.model?.rowCount ?: 0)).filter { row -> (commandList?.model?.getValueAt(row, 0) as? Boolean) ?: false }
              .map { row -> commandList?.model?.getValueAt(row, 1) as String })

          else -> TaskSettingsBase(taskType.name, enabledCheckbox.isSelected).apply {
            this.model = getVisibleModels().find { it.modelName == modelComboBox.selectedItem }
          }
        }
        settings.setTaskSettings(taskType, newSettings)
        taskTypeList.repaint()
      }
      modelComboBox.addActionListener {
        val newSettings = when (taskType) {
          TaskType.CommandAutoFix -> CommandAutoFixTask.CommandAutoFixTaskSettings(
            taskType.name,
            enabledCheckbox.isSelected,
            getVisibleModels().find { it.modelName == modelComboBox.selectedItem },
            (0 until (commandList?.model?.rowCount ?: 0)).map { row ->
              commandList?.model?.getValueAt(row, 1) as String
            })

          else -> TaskSettingsBase(taskType.name, enabledCheckbox.isSelected).apply {
            this.model = getVisibleModels().find { it.modelName == modelComboBox.selectedItem }
          }
        }
        settings.setTaskSettings(taskType, newSettings)
      }
    }

    fun saveSettings() {
      val newSettings = when (taskType) {
        TaskType.CommandAutoFix -> CommandAutoFixTask.CommandAutoFixTaskSettings(
          task_type = taskType.name,
          enabled = enabledCheckbox.isSelected,
          model = getVisibleModels().find { it.modelName == modelComboBox.selectedItem },
          commandAutoFixCommands = (0 until (commandList?.model?.rowCount ?: 0)).filter { row ->
            commandList?.model?.getValueAt(row, 0) as Boolean
          }.map { row -> commandList?.model?.getValueAt(row, 1) as String })

        else -> TaskSettingsBase(taskType.name, enabledCheckbox.isSelected).apply {
          this.model = getVisibleModels().find { it.modelName == modelComboBox.selectedItem }
        }
      }
      if (validateModelSelection(taskType, newSettings.model)) {
        settings.setTaskSettings(taskType, newSettings)
      }
    }
  }

  private data class CommandTableEntry(
    var enabled: Boolean, val command: String
  )

  private val temperatureSlider = JSlider(MIN_TEMP, MAX_TEMP, (settings.temperature * TEMPERATURE_SCALE).toInt()).apply {
    addChangeListener {
      settings.temperature = value / TEMPERATURE_SCALE
      temperatureLabel.text = TEMPERATURE_LABEL.format(settings.temperature)
    }
  }
  private val defaultModel = AppSettingsState.instance.smartModel
  private val fastModel = AppSettingsState.instance.fastModel
  private val temperatureLabel = JLabel(TEMPERATURE_LABEL.format(settings.temperature))
  private val autoFixCheckbox = JCheckBox("Auto-apply fixes", settings.autoFix)
  private val allowBlockingCheckbox = JCheckBox("Allow blocking", settings.allowBlocking)
  private val taskTypeList = JBList(TaskType.values())
  private val configPanelContainer = JPanel(CardLayout())
  private val taskConfigs = mutableMapOf<String, TaskTypeConfigPanel>()
  private val savedConfigsCombo = ComboBox<String>().apply {
    preferredSize = Dimension(CONFIG_COMBO_WIDTH, CONFIG_COMBO_HEIGHT)
    AppSettingsState.instance.savedPlanConfigs.keys.sorted().forEach { addItem(it) }
  }

  private fun getVisibleModels() = ChatModel.values().map { it.value }.filter { isVisible(it) }.toList().sortedBy { "${it.provider.name} - ${it.modelName}" }

  init {
    taskTypeList.cellRenderer = TaskTypeListCellRenderer()
    taskTypeList.addListSelectionListener { e ->
      if (!e.valueIsAdjusting) {
        val selectedType = (taskTypeList.selectedValue as TaskType<*, *>).name
        (configPanelContainer.layout as CardLayout).show(configPanelContainer, selectedType)
        if (singleTaskMode) {
          TaskType.values().forEach { taskType ->
            val isSelected = taskType.name == selectedType
            taskConfigs[taskType.name]?.enabledCheckbox?.isSelected = isSelected
          }
        }
      }
    }
    TaskType.values().forEach { taskType ->
      val configPanel = TaskTypeConfigPanel(taskType)
      taskConfigs[taskType.name] = configPanel
      configPanelContainer.add(configPanel, taskType.name)
    }
    taskTypeList.selectedIndex = 0
    init()
    title = "Configure Planning and Tasks"
    temperatureSlider.addChangeListener {
      settings.temperature = temperatureSlider.value / 100.0
    }
  }


  private fun saveCurrentConfig() {
    val configName = JOptionPane.showInputDialog(
      null, "Enter configuration name:", "Save Configuration", JOptionPane.PLAIN_MESSAGE
    )?.trim()

    if (!validateConfigName(configName)) {
      return
    }
    taskConfigs.values.forEach { it.saveSettings() }
    if (AppSettingsState.instance.savedPlanConfigs.containsKey(configName)) {
      val confirmResult = JOptionPane.showConfirmDialog(
        null, "Configuration '$configName' already exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION
      )
      if (confirmResult != JOptionPane.YES_OPTION) {
        return
      }
    }
    val taskSettingsMap = TaskType.values().associate { taskType ->
      val taskSettings = settings.getTaskSettings(taskType)
      taskType.name to AppSettingsState.TaskSettingsSerialized(
        enabled = taskSettings.enabled, modelName = taskSettings.model?.modelName, commandAutoFixCommands = if (taskType == TaskType.CommandAutoFix) {
          (taskSettings as? CommandAutoFixTask.CommandAutoFixTaskSettings)?.commandAutoFixCommands
        } else null
      )
    }
    val config = AppSettingsState.SavedPlanConfig(
      name = configName!!,
      temperature = settings.temperature,
      autoFix = settings.autoFix,
      allowBlocking = settings.allowBlocking,
      taskSettings = taskSettingsMap
    )
    AppSettingsState.instance.savedPlanConfigs[configName] = config
    savedConfigsCombo.addItem(configName)
    savedConfigsCombo.selectedItem = configName
  }

  private fun loadConfig(configName: String) {
    val config = AppSettingsState.instance.savedPlanConfigs[configName] ?: return
    val hasUnsavedChanges = TaskType.values().any { taskType ->
      val currentSettings = settings.getTaskSettings(taskType)
      val savedSettings = config.taskSettings[taskType.name]
      currentSettings.enabled != savedSettings?.enabled || currentSettings.model?.modelName != savedSettings.modelName
    }
    if (hasUnsavedChanges) {
      val confirmResult = JOptionPane.showConfirmDialog(
        null, "Loading will discard unsaved changes. Continue?", "Confirm Load", JOptionPane.YES_NO_OPTION
      )
      if (confirmResult != JOptionPane.YES_OPTION) {
        return
      }
    }
    try {
      val validatedTemp = config.temperature.coerceIn(0.0, 1.0)
      settings.temperature = validatedTemp
      temperatureSlider.value = (validatedTemp * TEMPERATURE_SCALE).toInt()
      temperatureLabel.text = TEMPERATURE_LABEL.format(validatedTemp)
      settings.autoFix = config.autoFix
      settings.allowBlocking = config.allowBlocking
      autoFixCheckbox.isSelected = config.autoFix
      allowBlockingCheckbox.isSelected = config.allowBlocking
      val taskUpdates = config.taskSettings.mapNotNull { (taskTypeName, serializedSettings) ->
        val taskType = TaskType.values().find { it.name == taskTypeName } ?: return@mapNotNull null
        val availableModels = getVisibleModels()
        val selectedModel = availableModels.find { it.modelName == serializedSettings.modelName } ?: availableModels.firstOrNull()
        Triple(taskType, serializedSettings, selectedModel)
      }
      taskUpdates.forEach { (taskType, serializedSettings, selectedModel) ->
        val newSettings = when (taskType) {
          TaskType.CommandAutoFix -> CommandAutoFixTask.CommandAutoFixTaskSettings(
            taskType.name, serializedSettings.enabled, selectedModel, serializedSettings.commandAutoFixCommands ?: emptyList()
          )

          else -> TaskSettingsBase(taskType.name, serializedSettings.enabled).apply {
            this.model = selectedModel
          }
        }
        settings.setTaskSettings(taskType, newSettings)
        taskConfigs[taskType.name]?.apply {
          enabledCheckbox.isSelected = serializedSettings.enabled
          if (modelComboBox.itemCount > 0 && selectedModel != null) {
            modelComboBox.selectedItem = selectedModel.modelName
          } else {
            modelComboBox.selectedItem = defaultModel
          }
        }
      }
      // Update UI once after all changes
      taskTypeList.repaint()
    } catch (e: Exception) {
      JOptionPane.showMessageDialog(
        null, "Error loading configuration: ${e.message}", "Load Error", JOptionPane.ERROR_MESSAGE
      )
    }
  }

  override fun createCenterPanel(): JComponent = panel {
    group("Settings") {
      if (!singleTaskMode) {
        row("Saved Configs:") {
          cell(savedConfigsCombo).align(Align.FILL).comment("Select a saved configuration to load or save current settings")
          button("Save...") {
            saveCurrentConfig()
          }
          button("Load") {
            val selected = savedConfigsCombo.selectedItem as? String
            if (selected != null) {
              loadConfig(selected)
            } else {
              JOptionPane.showMessageDialog(
                null, "Please select a configuration to load", "No Configuration Selected", JOptionPane.WARNING_MESSAGE
              )
            }
          }
          button("Delete") {
            val selected = savedConfigsCombo.selectedItem as? String
            if (selected != null) {
              val confirmResult = JOptionPane.showConfirmDialog(
                null, "Delete configuration '$selected'?", "Confirm Delete", JOptionPane.YES_NO_OPTION
              )
              if (confirmResult == JOptionPane.YES_OPTION) {
                AppSettingsState.instance.savedPlanConfigs.remove(selected)
                savedConfigsCombo.removeItem(selected)
              }
            } else {
              JOptionPane.showMessageDialog(
                null, "Please select a configuration to delete", "No Configuration Selected", JOptionPane.WARNING_MESSAGE
              )
            }
          }
        }
      }
      row {
        cell(autoFixCheckbox).align(Align.FILL).comment("Automatically apply suggested fixes without confirmation")
      }
      row {
        cell(allowBlockingCheckbox).align(Align.FILL).comment("Allow tasks to block UI while processing")
      }
      row("Temperature:") {
        cell(temperatureSlider).align(Align.FILL).comment("Adjust AI response creativity (higher = more creative)")
        cell(temperatureLabel)
      }
    }
    group("Tasks") {
      row {
        cell(
          JBSplitter(false, DIVIDER_PROPORTION).apply {
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
          }).align(Align.FILL).resizableColumn()
      }.resizableRow()
    }.layout(RowLayout.PARENT_GRID).resizableRow()
  }

  override fun doOKAction() {
    val invalidTasks = taskConfigs.values.filter { configPanel ->
      val isEnabled = configPanel.enabledCheckbox.isSelected
      val model = getVisibleModels().find { it.modelName == configPanel.modelComboBox.selectedItem }
      isEnabled && model == null
    }
    if (invalidTasks.isNotEmpty()) {
      val taskNames = invalidTasks.map { it.taskType.name }.joinToString(", ")
      JOptionPane.showMessageDialog(
        null, "Please select models for enabled tasks: $taskNames", "Missing Models", JOptionPane.WARNING_MESSAGE
      )
      return
    }
    taskConfigs.values.forEach { configPanel ->
      configPanel.saveSettings()
    }
    settings.autoFix = autoFixCheckbox.isSelected
    settings.allowBlocking = allowBlockingCheckbox.isSelected
    super.doOKAction()
  }

}