package aicoder.actions.agent

/**
 * Action that provides automated fixing of command execution issues through AI assistance
 */

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.isFile
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.config.CommandConfig
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.general.CmdPatchApp
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.util.commonRoot
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.util.JsonUtil.fromJson
import com.simiacryptus.util.toJson
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import javax.swing.*
import kotlin.collections.set

class CommandAutofixAction : BaseAction() {
  
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  
  /**
   * Handles the action execution.
   * Shows settings dialog, creates patch app session and opens browser interface.
   */
  override fun handle(e: AnActionEvent) {
    try {
      UITools.runAsync(e.project, "Initializing Command Autofix", true) { progress ->
        progress.isIndeterminate = true
        progress.text = "Getting settings..."
        val files = UITools.getSelectedFiles(e)
        val folders = UITools.getSelectedFolders(e).map { it.toFile.toPath() }
        val root = (folders + files.map { it.toFile.toPath() }).filterNotNull().toTypedArray().commonRoot()
        lateinit var settingsUI: SettingsUI
        val settings = run {
          var settings1: PatchApp.Settings? = null
          SwingUtilities.invokeAndWait {
            settingsUI = SettingsUI(workingDirectory = root.toFile(), folders)
            // If a single file is provided that is executable or has a whitelisted extension
            if (files.size == 1) {
              val defaultFile = files[0]
              val whitelist = listOf("sh", "py", "bat", "ps")
              val matchesWhitelist = whitelist.any { defaultFile.name.endsWith(".$it", ignoreCase = true) }
              if (defaultFile.isFile && (defaultFile.toFile.canExecute() || matchesWhitelist)) {
                // Update the default fields for the first (and only) command panel
                val first = settingsUI.commandsList.firstOrNull()
                if (first != null) {
                  first.commandField.selectedItem = defaultFile.toFile.absolutePath
                  first.workingDirectoryField.selectedItem = defaultFile.parent
                  first.argumentsField.selectedItem = ""
                }
              }
            }
            val dialog = CommandSettingsDialog(e.project, settingsUI)
            dialog.show()
            settings1 = if (dialog.isOK) {
              val commands = settingsUI.commandsList.map { cmdPanel ->
                val executable = File(
                  cmdPanel.commandField.selectedItem?.toString()
                    ?: throw IllegalArgumentException("No executable selected")
                )
                AppSettingsState.instance.executables?.plusAssign(executable.absolutePath)
                val argument = cmdPanel.argumentsField.selectedItem?.toString() ?: ""
                AppSettingsState.instance.recentArguments?.remove(argument)
                AppSettingsState.instance.recentArguments?.add(0, argument)
                AppSettingsState.instance.recentArguments?.apply {
                  if(size > MAX_RECENT_ARGUMENTS) dropLast(size - MAX_RECENT_ARGUMENTS)
                }
                val workingDir = cmdPanel.workingDirectoryField.selectedItem?.toString() ?: ""
                AppSettingsState.instance.recentWorkingDirs?.remove(workingDir)
                AppSettingsState.instance.recentWorkingDirs?.add(0, workingDir)
                AppSettingsState.instance.recentWorkingDirs?.apply {
                  if(size > MAX_RECENT_ARGUMENTS) dropLast(size - MAX_RECENT_DIRS)
                }
                require(executable.exists()) { "Executable file does not exist: ${executable}" }
                PatchApp.CommandSettings(
                  executable = executable,
                  arguments = argument,
                  workingDirectory = File(workingDir),
                  additionalInstructions = settingsUI.additionalInstructionsField.text
                )
              }.toList()
              PatchApp.Settings(
                commands = commands,
                exitCodeOption = when (settingsUI.exitCodeOption) {
                  SettingsUI.ExitCodeOption.ZERO -> "0"
                  SettingsUI.ExitCodeOption.ANY -> "any"
                  SettingsUI.ExitCodeOption.NONZERO -> "nonzero"
                },
                autoFix = settingsUI.autoFixCheckBox.isSelected,
                maxRetries = settingsUI.maxRetriesSlider.value,
                includeLineNumbers = settingsUI.includeLineNumbersCheckBox.isSelected,
                includeGitDiffs = settingsUI.includeGitDiffsCheckBox.isSelected
              )
            } else {
              null
            }
          }
          settings1
        } ?: return@runAsync
        val patchApp = CmdPatchApp(
          root = root,
          settings = settings,
          api = api.getChildClient().apply {
            budget = settingsUI.apiBudgetField.value as Double
          },
          files = files.map { it.toFile }.toTypedArray(),
          model = AppSettingsState.instance.smartModel.chatModel(),
          parsingModel = AppSettingsState.instance.fastModel.chatModel()
        )
        val session = Session.newGlobalID()
        SessionProxyServer.chats[session] = patchApp
        ApplicationServer.appInfoMap[session] = AppInfoData(
          applicationName = "Code Chat",
          singleInput = true,
          stickyInput = false,
          loadImages = false,
          showMenubar = false
        )
        val dateFormat = SimpleDateFormat("HH:mm:ss")
        val sessionName = "${javaClass.simpleName} @ ${dateFormat.format(System.currentTimeMillis())}"
        SessionProxyServer.metadataStorage.setSessionName(null, session, sessionName)
        val server = AppServer.getServer(e.project)
        Thread {
          Thread.sleep(500)
          try {
            val uri = server.server.uri.resolve("/#$session")
            BaseAction.log.info("Opening browser to $uri")
            browse(uri)
          } catch (e: Throwable) {
            log.warn("Error opening browser", e)
          }
        }.start()
      }
    } catch (e: Throwable) {
      log.error("Failed to execute command autofix", e)
      UITools.showErrorDialog("Failed to execute command autofix: ${e.message}", "Error")
    }
  }
  
  /**
   * Checks if the action should be enabled
   */
  override fun isEnabled(event: AnActionEvent): Boolean {
    if (event.project == null) return false
    val folder = UITools.getSelectedFolder(event)
    val hasBasePath = event.project?.basePath != null
    return folder != null || hasBasePath
  }
  
  companion object {
    private val log = LoggerFactory.getLogger(CommandAutofixAction::class.java)
    private const val DEFAULT_ARGUMENT = "run build"
    private const val MAX_RECENT_ARGUMENTS = 10
    private const val MAX_RECENT_DIRS = 10
    private const val TEXT_AREA_ROWS = 6
    
    /**
     * Dialog for command settings configuration
     */
    class CommandSettingsDialog(project: com.intellij.openapi.project.Project?, private val settingsUI: SettingsUI) :
      DialogWrapper(project, true) {
      init {
        title = "Command Autofix Settings"
        init()
      }
      
      override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(800, 600)
        // Main content panel with scrolling
        val contentPanel = JPanel(BorderLayout()).apply {
          border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        // Add options panel
        val optionsPanel = panel {
          row { cell(settingsUI.commandsContainerPanel) }
          group("Saved Configurations") {
            row {
              cell(settingsUI.savedConfigsCombo)
              button("Load") {
                val configName = settingsUI.savedConfigsCombo.selectedItem as? String
                if (!configName.isNullOrBlank()) {
                  settingsUI.loadConfig(configName)
                }
              }
              button("Save") {
                settingsUI.saveCurrentConfig()
              }
            }
          }
          group("Code Analysis Options") {
            row {
              cell(settingsUI.autoFixCheckBox)
              cell(settingsUI.includeLineNumbersCheckBox)
              cell(settingsUI.includeGitDiffsCheckBox)
            }
          }
          group("Execution Options") {
            row("Max Retries:") {
              cell(settingsUI.maxRetriesSlider)
              cell(settingsUI.maxRetriesField)
            }
            row("Budget:") {
              cell(settingsUI.apiBudgetSlider)
              cell(settingsUI.apiBudgetField)
            }
          }
          group("Autofix On Exit Code:") {
            buttonsGroup {
              row {
                settingsUI.exitCodeNonZero = radioButton(
                  "Non-zero (Error)",
                  SettingsUI.ExitCodeOption.NONZERO
                )
                settingsUI.exitCodeZero = radioButton(
                  "Zero (Success)",
                  SettingsUI.ExitCodeOption.ZERO
                )
                settingsUI.exitCodeAny = radioButton(
                  "Any (Always Run)",
                  SettingsUI.ExitCodeOption.ANY
                )
              }
            }.apply {
              bind({ settingsUI.exitCodeOption }, { settingsUI.exitCodeOption = it })
            }
          }
          group("Additional Instructions") {
            row {
              cell(settingsUI.additionalInstructionsField)
            }
          }
        }
        contentPanel.add(optionsPanel, BorderLayout.CENTER)
        panel.add(contentPanel, BorderLayout.CENTER)
        return panel
      }
    }
    
    
    class SettingsUI(val workingDirectory: File, val folders: List<Path>) {
      val commandsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
      }
      val commandsScrollPane = JBScrollPane(commandsPanel).apply {
        border = BorderFactory.createLoweredBevelBorder()
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        preferredSize = Dimension(750, 250)
        // Improve scrolling speed
        verticalScrollBar.unitIncrement = 16
        // Set a minimum size to ensure scroll pane is always visible
        minimumSize = Dimension(600, 150)
      }
      val commandsList = mutableListOf<CommandPanel>()
      val savedConfigsCombo = ComboBox<String>().apply {
        preferredSize = Dimension(200, 30)
        AppSettingsState.instance.savedCommandConfigsJson?.keys?.sorted()?.forEach { addItem(it) }
      }
      
      // Radio button selection model
      enum class ExitCodeOption { NONZERO, ZERO, ANY }
      
      var exitCodeOption = ExitCodeOption.NONZERO
      var exitCodeNonZero: Cell<JBRadioButton>? = null
      var exitCodeZero: Cell<JBRadioButton>? = null
      var exitCodeAny: Cell<JBRadioButton>? = null
      val includeGitDiffsCheckBox = JCheckBox("Include Git Working Copy Diffs").apply {
        isSelected = false
        toolTipText = "Include git diffs between working copy and HEAD when analyzing code"
      }
      val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers").apply {
        isSelected = true
        toolTipText = "Show line numbers in code snippets for better context"
      }
      
      // Container panel for commands with add button
      val commandsContainerPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createTitledBorder("Commands")
        preferredSize = Dimension(750, 300)
      }
      
      // Button to add new command panels
      val addCommandButton = JButton("Add Command").apply {
        addActionListener {
          addCommandPanel()
        }
      }
      
      
      init {
        // Set up the commands container with scroll pane and add button
        commandsContainerPanel.add(commandsScrollPane, BorderLayout.CENTER)
        val buttonPanel = JPanel(BorderLayout()).apply {
          border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
          add(addCommandButton, BorderLayout.EAST)
          // Add a button to clear all commands except the first one
          add(JButton("Clear All").apply {
            addActionListener {
              if (commandsList.size > 0) {
                // Keep the first panel
                val firstPanel = commandsList.firstOrNull()
                commandsList.clear()
                commandsPanel.removeAll()
                if (firstPanel != null) {
                  commandsList.add(firstPanel)
                  commandsPanel.add(firstPanel)
                  commandsPanel.add(Box.createVerticalStrut(5))
                } else {
                  addCommandPanel()
                }
                commandsPanel.revalidate()
                commandsPanel.repaint()
              }
            }
          }, BorderLayout.WEST)
        }
        commandsContainerPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        addCommandPanel()
      }
      
      fun addCommandPanel() {
        val cmdPanel = CommandPanel(workingDirectory, folders)
        commandsList.add(cmdPanel)
        commandsPanel.add(cmdPanel)
        commandsPanel.add(Box.createVerticalStrut(5))
        commandsPanel.revalidate()
        commandsPanel.repaint()
        // Ensure the newest panel is visible by scrolling to it after UI updates
        SwingUtilities.invokeLater {
          // Scroll to the bottom after UI is updated
          commandsScrollPane.revalidate()
          commandsScrollPane.repaint()
          // Schedule scrolling after layout is complete
          SwingUtilities.invokeLater {
            commandsScrollPane.viewport.viewPosition =
              java.awt.Point(0, commandsPanel.height - commandsScrollPane.viewport.height)
          }
        }
      }
      
      fun removeCommandPanel(panel: CommandPanel) {
        // Don't allow removing the last panel
        if (commandsList.size <= 1) {
          return
        }
        
        commandsList.remove(panel)
        commandsPanel.remove(panel)
        // Also remove the vertical strut that follows the panel if it exists
        if (commandsPanel.componentCount > commandsList.size * 2) {
          commandsPanel.remove(commandsPanel.getComponentZOrder(panel) + 1)
        }
        commandsPanel.revalidate()
        commandsPanel.repaint()
        // Adjust the container size after removing a panel
        SwingUtilities.invokeLater {
          SwingUtilities.getWindowAncestor(commandsPanel)?.pack()
        }
      }
      
      val maxRetriesField: JSpinner = JSpinner(SpinnerNumberModel(3, 0, 10, 1)).apply {
        toolTipText = "Maximum number of auto-retry attempts (0-10)"
        addChangeListener {
          // Sync slider with spinner value
          maxRetriesSlider.value = value as Int
        }
      }
      val maxRetriesSlider = JSlider(JSlider.HORIZONTAL, 0, 10, 3).apply {
        majorTickSpacing = 2
        minorTickSpacing = 1
        paintTicks = true
        paintLabels = true
        toolTipText = "Maximum number of auto-retry attempts (0-10)"
        addChangeListener {
          // Sync spinner with slider value
          maxRetriesField.value = value
        }
      }
      val apiBudgetSlider: JSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
        majorTickSpacing = 20
        minorTickSpacing = 5
        paintTicks = true
        paintLabels = true
        toolTipText = "API budget for this session (0.0 - 10.0)"
        addChangeListener {
          // Convert slider value (0-100) to budget (0.0-10.0)
          apiBudgetField.value = value / 10.0
        }
      }
      val additionalInstructionsField = JTextArea().apply {
        rows = TEXT_AREA_ROWS
        columns = 60
        lineWrap = true
        wrapStyleWord = true
        border = BorderFactory.createLoweredBevelBorder()
        minimumSize = Dimension(400, 100)
        //preferredSize = Dimension(600, 100)
      }
      val apiBudgetField = JSpinner(SpinnerNumberModel(0.0, 0.0, 1000.0, 0.1)).apply {
        toolTipText = "Specify the API budget for this session (0.0 - 1000.0)"
        addChangeListener {
          // Convert budget value (0.0-10.0) to slider (0-100)
          val budgetValue = value as Double
          if (budgetValue <= 10.0) {
            apiBudgetSlider.value = (budgetValue * 10).toInt()
          }
        }
      }
      val autoFixCheckBox = JCheckBox("Auto-apply fixes").apply {
        isSelected = false
      }
      
      fun saveCurrentConfig() {
        val configName = JOptionPane.showInputDialog(
          null, "Enter configuration name:", "Save Configuration", JOptionPane.PLAIN_MESSAGE
        )?.trim()
        if (configName.isNullOrBlank()) {
          JOptionPane.showMessageDialog(
            null, "Please enter a valid configuration name", "Invalid Name", JOptionPane.WARNING_MESSAGE
          )
          return
        }
        val config = CommandConfig(
          commands = commandsList.map { it.toCommandSettings() },
          exitCodeOption = when (exitCodeOption) {
            ExitCodeOption.ZERO -> "0"
            ExitCodeOption.ANY -> "any"
            ExitCodeOption.NONZERO -> "nonzero"
          },
          autoFix = autoFixCheckBox.isSelected,
          maxRetries = maxRetriesSlider.value,
          includeGitDiffs = includeGitDiffsCheckBox.isSelected,
          includeLineNumbers = includeLineNumbersCheckBox.isSelected,
          additionalInstructions = additionalInstructionsField.text,
          apiBudget = apiBudgetField.value as Double
        )
        AppSettingsState.instance.savedCommandConfigsJson?.set(configName, config.toJson())
        savedConfigsCombo.addItem(configName)
        savedConfigsCombo.selectedItem = configName
      }
      
      fun loadConfig(configName: String) {
        val config =
          AppSettingsState.instance.savedCommandConfigsJson?.get(configName)
            ?.let<String, CommandConfig?> { fromJson(it, CommandConfig::class.java) } ?: return
        commandsList.clear()
        commandsPanel.removeAll()
        config.commands.forEach {
          val panel = CommandPanel(workingDirectory, folders)
          panel.loadFromSettings(it)
          // Ensure working directory is set to the currently selected directory
          panel.workingDirectoryField.selectedItem = workingDirectory.absolutePath
          commandsList.add(panel)
          commandsPanel.add(panel)
          commandsPanel.add(Box.createVerticalStrut(5))
        }
        exitCodeOption = when (config.exitCodeOption) {
          "0" -> ExitCodeOption.ZERO
          "any" -> ExitCodeOption.ANY
          else -> ExitCodeOption.NONZERO
        }
        autoFixCheckBox.isSelected = config.autoFix
        maxRetriesSlider.value = config.maxRetries
        includeGitDiffsCheckBox.isSelected = config.includeGitDiffs
        includeLineNumbersCheckBox.isSelected = config.includeLineNumbers ?: true
        additionalInstructionsField.text = config.additionalInstructions
        // Set budget value
        val budgetValue = if (config.apiBudget != null) config.apiBudget else 0.0
        apiBudgetField.value = budgetValue
        if (budgetValue <= 10.0) {
          apiBudgetSlider.value = (budgetValue * 10).toInt()
        }
        commandsPanel.revalidate()
        commandsPanel.repaint()
      }
      
      
      class CommandPanel(workingDirectory: File, folders: List<Path>) : JPanel() {
        val workingDirectoryField = ComboBox<String>().apply {
          isEditable = true
          val items = mutableListOf<String>()
          AppSettingsState.instance.recentWorkingDirs?.forEach { addItem(it); items.add(it) }
          if (AppSettingsState.instance.recentWorkingDirs?.isEmpty() == true) {
            addItem(workingDirectory.absolutePath)
          }
          folders.forEach {
            val absolutePath = it.toFile().absolutePath
            if (!items.contains(absolutePath)) {
              addItem(absolutePath)
              items.add(absolutePath)
            }
          }
          selectedItem = workingDirectory.absolutePath
          preferredSize = Dimension(400, preferredSize.height)
        }
        val commandField = ComboBox(AppSettingsState.instance.executables?.toTypedArray() ?: emptyArray()).apply {
          isEditable = true
          preferredSize = Dimension(400, preferredSize.height)
        }
        val workingDirectoryButton = JButton("...").apply {
          addActionListener {
            val fileChooser = JFileChooser().apply {
              fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
              isMultiSelectionEnabled = false
              this.selectedFile =
                File(workingDirectoryField.selectedItem?.toString() ?: workingDirectory.absolutePath)
            }
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
              workingDirectoryField.selectedItem = fileChooser.selectedFile.absolutePath
            }
          }
        }
        
        val commandButton = JButton("...").apply {
          addActionListener {
            val fileChooser = JFileChooser().apply {
              fileSelectionMode = JFileChooser.FILES_ONLY
              isMultiSelectionEnabled = false
            }
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
              commandField.selectedItem = fileChooser.selectedFile.absolutePath
            }
          }
        }
        val argumentsField = ComboBox<String>().apply {
          isEditable = true
          AppSettingsState.instance.recentArguments?.forEach { addItem(it) }
          if (AppSettingsState.instance.recentArguments?.isEmpty() == true) {
            addItem("")
          }
          preferredSize = Dimension(450, preferredSize.height)
        }
        
        init {
          border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createEtchedBorder()
          )
          layout = BorderLayout()
          // Set a minimum size to prevent the panel from collapsing
          minimumSize = Dimension(650, 120)
          //preferredSize = Dimension(700, 120)
          val fieldsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
          }
          // Add a remove button in the top-right corner
          val headerPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
          }
          val removeButton = JButton("Remove").apply {
            addActionListener {
              val parent = SwingUtilities.getAncestorOfClass(SettingsUI::class.java, this@CommandPanel) as? SettingsUI
              parent?.removeCommandPanel(this@CommandPanel)
            }
          }
          headerPanel.add(removeButton, BorderLayout.EAST)
          add(headerPanel, BorderLayout.NORTH)
          
          // Command row
          fieldsPanel.add(JPanel(BorderLayout(5, 0)).apply {
            add(JLabel("Command:", SwingConstants.RIGHT).apply {
              preferredSize = Dimension(100, preferredSize.height)
            }, BorderLayout.WEST)
            add(commandField, BorderLayout.CENTER)
            add(commandButton, BorderLayout.EAST)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            alignmentX = LEFT_ALIGNMENT
          })
          fieldsPanel.add(Box.createVerticalStrut(5))
          // Arguments row
          fieldsPanel.add(JPanel(BorderLayout(5, 0)).apply {
            add(JLabel("Arguments:", SwingConstants.RIGHT).apply {
              preferredSize = Dimension(100, preferredSize.height)
            }, BorderLayout.WEST)
            add(argumentsField, BorderLayout.CENTER)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            alignmentX = LEFT_ALIGNMENT
          })
          fieldsPanel.add(Box.createVerticalStrut(5))
          // Working directory row
          fieldsPanel.add(JPanel(BorderLayout(5, 0)).apply {
            add(JLabel("Directory:", SwingConstants.RIGHT).apply {
              preferredSize = Dimension(100, preferredSize.height)
            }, BorderLayout.WEST)
            add(workingDirectoryField, BorderLayout.CENTER)
            add(workingDirectoryButton, BorderLayout.EAST)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            alignmentX = LEFT_ALIGNMENT
          })
          add(fieldsPanel, BorderLayout.CENTER)
        }
        
        fun toCommandSettings(): PatchApp.CommandSettings {
          return PatchApp.CommandSettings(
            executable = File(commandField.selectedItem?.toString() ?: ""),
            arguments = argumentsField.selectedItem?.toString() ?: "",
            workingDirectory = File(workingDirectoryField.selectedItem?.toString() ?: ""),
            additionalInstructions = ""
          )
        }
        
        fun loadFromSettings(settings: PatchApp.CommandSettings) {
          commandField.selectedItem = settings.executable.absolutePath
          argumentsField.selectedItem = settings.arguments
          // Don't override the working directory from settings to maintain context
          // of the currently selected directory/file in the IDE
        }
        
      }
    }
    
  }
  
}