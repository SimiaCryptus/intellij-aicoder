package aicoder.actions.plan

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.config.AppSettingsState.SavedPlanConfig
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.TaskSettingsBase
import com.simiacryptus.skyenet.apps.plan.TaskType
import com.simiacryptus.skyenet.apps.plan.tools.CommandAutoFixTask
import com.simiacryptus.util.JsonUtil.fromJson
import com.simiacryptus.util.JsonUtil.toJson
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
  var apiBudget: Double = 10.0
) : DialogWrapper(project) {
  // New UI elements for AutoPlanMode parameters
  private val maxTaskHistoryCharsField = JBTextField("20000")
  private val maxTasksPerIterationField = JBTextField("3")
  private val maxIterationsField = JBTextField("100")
  
  // New UI elements for graph file input when "Graph" mode is selected.
  private val graphFileTextField = JTextField(com.simiacryptus.skyenet.apps.graph.GraphOrderedPlanMode.graphFile, 20)
  private val selectGraphFileButton = JButton("Select File")
  private val graphFilePanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.X_AXIS)
    add(graphFileTextField)
    add(Box.createHorizontalStrut(5))
    add(selectGraphFileButton)
    isVisible = false // initially hidden
  }
  
  // New panel for AutoPlan settings to appear only when "Auto Plan" is selected.
  private val autoPlanPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    add(JLabel("Max Task History Chars:"))
    add(maxTaskHistoryCharsField)
    add(Box.createVerticalStrut(5))
    add(JLabel("Max Tasks Per Iteration:"))
    add(maxTasksPerIterationField)
    add(Box.createVerticalStrut(5))
    add(JLabel("Max Iterations:"))
    add(maxIterationsField)
    isVisible = false
  }
  
  companion object {
    private const val CONFIG_COMBO_WIDTH = 200
    private const val CONFIG_COMBO_HEIGHT = 30
    private const val MIN_TEMP = 0
    private const val MAX_TEMP = 100
    private const val MIN_BUDGET = 1
    private const val MAX_BUDGET = 100
    private const val DEFAULT_BUDGET = 10
    private const val DEFAULT_LIST_WIDTH = 150
    private const val DEFAULT_LIST_HEIGHT = 200
    private const val DEFAULT_PANEL_WIDTH = 350
    private const val DEFAULT_PANEL_HEIGHT = 200
    private const val TEMPERATURE_SCALE = 100.0
    private const val TEMPERATURE_LABEL = "%.2f"
    private const val BUDGET_LABEL = "$%.2f"
    private const val FONT_SIZE_ENABLED = 14f
    private const val FONT_SIZE_DISABLED = 12f
    private const val DIVIDER_PROPORTION = 0.3f
    
    fun isVisible(it: ChatModel): Boolean {
      return AppSettingsState.instance.apiKeys?.get(it.provider.name)?.isNotBlank() ?: false
    }
  }
  
  val cognitiveModeCombo = ComboBox(arrayOf("Single Task", "Plan Ahead", "Auto Plan", "Graph")).apply {
    preferredSize = Dimension(200, 30)
    selectedIndex = 0 // default to "Auto Plan" for example
  }
  
  // Budget slider and label
  private val budgetSlider = JSlider(MIN_BUDGET, MAX_BUDGET, apiBudget.toInt()).apply {
    addChangeListener {
      apiBudget = value.toDouble()
      budgetLabel.text = BUDGET_LABEL.format(apiBudget)
    }
  }
  private val budgetLabel = JLabel(BUDGET_LABEL.format(apiBudget))
  
  
  private fun validateModelSelection(taskType: TaskType<*, *>, model: ChatModel?): Boolean {
    if (model == null && settings.getTaskSettings(taskType).enabled) {
      return false
    }
    return true
  }
  
  private fun validateConfigName(name: String?) = when {
    name.isNullOrBlank() -> {
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
      <p>${taskType.tooltipHtml}</p>
      </body>
      </html>
    """
    
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
          list?.foreground?.darker()?.darker()
        } else {
          list?.foreground?.darker()
        }
        text = buildString {
          val taskDescription = value.description ?: ""
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
          else -> AppSettingsState.instance.smartModel
        }
      }
    }
    private val commandList = if (taskType == TaskType.CommandAutoFixTask) {
      JBTable(object : DefaultTableModel(
        arrayOf("Enabled", "Command"), 0
      ) {
        
        private val entries = mutableListOf<CommandTableEntry>()
        
        init {
          val sortedExecutables = AppSettingsState.instance.executables?.sortedWith(String.CASE_INSENSITIVE_ORDER)
          sortedExecutables?.forEach { command ->
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
                AppSettingsState.instance.executables?.add(command)
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
                (commandList.model as DefaultTableModel).removeRow(selectedRow)
                AppSettingsState.instance.executables?.remove(command)
                
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
      modelComboBox.selectedItem = currentModel?.modelName ?: AppSettingsState.instance.smartModel
      enabledCheckbox.addItemListener {
        val newSettings = when (taskType) {
          TaskType.CommandAutoFixTask -> CommandAutoFixTask.CommandAutoFixTaskSettings(
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
          TaskType.CommandAutoFixTask -> CommandAutoFixTask.CommandAutoFixTaskSettings(
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
        TaskType.CommandAutoFixTask -> CommandAutoFixTask.CommandAutoFixTaskSettings(
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
  private val temperatureLabel = JLabel(TEMPERATURE_LABEL.format(settings.temperature))
  private val autoFixCheckbox = JCheckBox("Auto-apply fixes", settings.autoFix)
  private val taskTypeList = JBList(TaskType.values())
  private val configPanelContainer = JPanel(CardLayout())
  private val taskConfigs = mutableMapOf<String, TaskTypeConfigPanel>()
  private val savedConfigsCombo = ComboBox<String>().apply {
    preferredSize = Dimension(CONFIG_COMBO_WIDTH, CONFIG_COMBO_HEIGHT)
    AppSettingsState.instance.savedPlanConfigs?.keys?.sorted()?.forEach { addItem(it) }
  }
  
  private fun getVisibleModels() = ChatModel.values().map { it.value }.filter { isVisible(it) }.toList().sortedBy { "${it.provider.name} - ${it.modelName}" }
  
  init {
    taskTypeList.cellRenderer = TaskTypeListCellRenderer()
    taskTypeList.addListSelectionListener { e ->
      if (!e.valueIsAdjusting) {
        val selectedType = (taskTypeList.selectedValue as TaskType<*, *>).name
        (configPanelContainer.layout as CardLayout).show(configPanelContainer, selectedType)
        if (cognitiveModeCombo.selectedItem as String == "Single Task") {
          TaskType.values().forEach { taskType ->
            taskConfigs[taskType.name]?.enabledCheckbox?.apply {
              isSelected = (taskType.name == selectedType)
            }
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
    
    // Add an action listener to transition the UI when the cognitive mode changes.
    
    
    cognitiveModeCombo.addActionListener {
      val selected = cognitiveModeCombo.selectedItem as String
      // Show the graph file input only when "Graph" is selected.
      graphFilePanel.isVisible = (selected == "Graph")
      // Show the AutoPlan settings only when "Auto Plan" is selected.
      autoPlanPanel.isVisible = (selected == "Auto Plan")
      
      // For "Single Task" mode, keep taskTypeList enabled so the user can select a task,
      // while disabling all the task checkboxes.
      if (selected == "Single Task") {
        taskTypeList.isEnabled = true
        if (taskTypeList.selectedIndex == -1) {
          taskTypeList.selectedIndex = 0
        }
        // Disable all checkboxes
        taskConfigs.values.forEach { it.enabledCheckbox.isEnabled = false }
        // Automatically select the currently selected task only.
        val selectedType = (taskTypeList.selectedValue as TaskType<*, *>).name
        TaskType.values().forEach { taskType ->
          taskConfigs[taskType.name]?.enabledCheckbox?.isSelected = (taskType.name == selectedType)
        }
      } else {
        taskTypeList.isEnabled = true
        taskConfigs.values.forEach { it.enabledCheckbox.isEnabled = true }
      }
    }
    
    // Setup file select button to open a file chooser.
    selectGraphFileButton.addActionListener {
      val chooser = JFileChooser("")
      val result = chooser.showOpenDialog(null)
      if (result == JFileChooser.APPROVE_OPTION) {
        val selectedFile = chooser.selectedFile
        graphFileTextField.text = selectedFile.absolutePath
        // Update the graph file used by GraphOrderedPlanMode.
        com.simiacryptus.skyenet.apps.graph.GraphOrderedPlanMode.graphFile = selectedFile.absolutePath
      }
    }
    
    // Keep the text field in sync with the GraphOrderedPlanMode property.
    graphFileTextField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
      override fun insertUpdate(e: javax.swing.event.DocumentEvent?) {
        com.simiacryptus.skyenet.apps.graph.GraphOrderedPlanMode.graphFile = graphFileTextField.text
      }
      
      override fun removeUpdate(e: javax.swing.event.DocumentEvent?) {
        com.simiacryptus.skyenet.apps.graph.GraphOrderedPlanMode.graphFile = graphFileTextField.text
      }
      
      override fun changedUpdate(e: javax.swing.event.DocumentEvent?) {
        com.simiacryptus.skyenet.apps.graph.GraphOrderedPlanMode.graphFile = graphFileTextField.text
      }
    })
    
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
    if (AppSettingsState.instance.savedPlanConfigs?.containsKey(configName ?: "") == true) {
      val confirmResult = JOptionPane.showConfirmDialog(
        null, "Configuration '$configName' already exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION
      )
      if (confirmResult != JOptionPane.YES_OPTION) {
        return
      }
    }
    val taskSettingsMap = TaskType.values().associate { taskType ->
      val taskSettings = settings.getTaskSettings(taskType)
      taskType.name to TaskSettingsBase(
        task_type = taskType.name,
        enabled = taskSettings.enabled,
        model = taskSettings.model,
      )
    }
    val config = AppSettingsState.SavedPlanConfig(
      name = configName!!,
      temperature = settings.temperature,
      autoFix = settings.autoFix,
      apiBudget = apiBudget,
      taskSettings = taskSettingsMap
    )
    AppSettingsState.instance.savedPlanConfigs?.set(configName, toJson(config))
    savedConfigsCombo.addItem(configName)
    savedConfigsCombo.selectedItem = configName
  }
  
  private fun loadConfig(configName: String) {
    val config = AppSettingsState.instance.savedPlanConfigs?.get(configName)?.let<String, SavedPlanConfig?> { fromJson(it, SavedPlanConfig::class.java) } ?: return
    val hasUnsavedChanges = TaskType.values().any { taskType ->
      val currentSettings = settings.getTaskSettings(taskType)
      val savedSettings = config.taskSettings[taskType.name]
      currentSettings.enabled != savedSettings?.enabled || currentSettings.model?.modelName != savedSettings.model?.modelName
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
      autoFixCheckbox.isSelected = config.autoFix
      // Load API budget if available, otherwise use default
      apiBudget = config.apiBudget ?: DEFAULT_BUDGET.toDouble()
      budgetSlider.value = apiBudget.toInt()
      budgetLabel.text = BUDGET_LABEL.format(apiBudget)
      config.taskSettings.forEach { (taskTypeName: String, serializedSettings: TaskSettingsBase) ->
        val taskType = TaskType.values().find { it.name == taskTypeName } ?: return@forEach
        val availableModels = getVisibleModels()
        val selectedModel = availableModels.find { it.modelName == serializedSettings.model?.modelName } ?: availableModels.firstOrNull()
        settings.setTaskSettings(taskType, serializedSettings)
        taskConfigs[taskType.name]?.apply {
          enabledCheckbox.isSelected = serializedSettings.enabled
          if (modelComboBox.itemCount > 0 && selectedModel != null) {
            modelComboBox.selectedItem = selectedModel.modelName
          } else {
            modelComboBox.selectedItem = AppSettingsState.instance.smartModel
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
    
    group {
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
                AppSettingsState.instance.savedPlanConfigs?.remove(selected)
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
      row("Temperature:") {
        cell(temperatureSlider).align(Align.FILL).comment("Adjust AI response creativity (higher = more creative)")
        cell(temperatureLabel)
      }
      row("API Budget:") {
        cell(budgetSlider).align(Align.FILL).comment("Set maximum spending limit for this session (in USD)")
        cell(budgetLabel)
      }
      
      group("Planning Settings") {
        row("Cognitive Mode:") {
          cell(cognitiveModeCombo).align(Align.FILL).comment("Select the cognitive strategy for planning")
        }
        row {
          cell(autoPlanPanel).align(Align.FILL)
        }
        row {
          cell(graphFilePanel).align(Align.FILL)
        }
      }
      
      group("Task Settings") {
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
    // Save AutoPlanMode settings
    settings.maxTaskHistoryChars = maxTaskHistoryCharsField.text.toIntOrNull() ?: 20000
    settings.maxTasksPerIteration = maxTasksPerIterationField.text.toIntOrNull() ?: 3
    settings.maxIterations = maxIterationsField.text.toIntOrNull() ?: 100
    super.doOKAction()
  }
  
}