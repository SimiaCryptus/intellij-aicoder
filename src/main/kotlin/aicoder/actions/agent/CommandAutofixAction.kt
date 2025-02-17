package aicoder.actions.agent

/**
 * Action that provides automated fixing of command execution issues through AI assistance
 */

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.isFile
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.config.CommandConfig
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.general.CmdPatchApp
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.util.FileValidationUtils
import com.simiacryptus.skyenet.core.util.commonRoot
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
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

    override fun handle(event: AnActionEvent) {
        try {
            UITools.runAsync(event.project, "Initializing Command Autofix", true) { progress ->
                progress.isIndeterminate = true
                progress.text = "Getting settings..."
                val files = UITools.getSelectedFiles(event)//.map { it.toFile.toPath() }
                val folders = UITools.getSelectedFolders(event).map { it.toFile.toPath() }
                val root = folders.toTypedArray().commonRoot()
                val settings = run {
                    var settings1: PatchApp.Settings? = null
                    SwingUtilities.invokeAndWait {
                        val settingsUI = SettingsUI(workingDirectory = root.toFile(), folders)
                        // If a single file is provided that is executable or has a whitelisted extension
                        if (files.size == 1) {
                            val defaultFile = files[0]
                            val whitelist = listOf("sh", "py", "bat", "exe")
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
                        val dialog = CommandSettingsDialog(event.project, settingsUI)
                        dialog.show()
                        settings1 = if (dialog.isOK) {
                            val commands = settingsUI.commandsList.map { cmdPanel ->
                                val executable = File(
                                    cmdPanel.commandField.selectedItem?.toString()
                                        ?: throw IllegalArgumentException("No executable selected")
                                )
                                AppSettingsState.instance.executables += executable.absolutePath
                                val argument = cmdPanel.argumentsField.selectedItem?.toString() ?: ""
                                AppSettingsState.instance.recentArguments.remove(argument)
                                AppSettingsState.instance.recentArguments.add(0, argument)
                                AppSettingsState.instance.recentArguments =
                                    AppSettingsState.instance.recentArguments.take(MAX_RECENT_ARGUMENTS).toMutableList()
                                val workingDir = cmdPanel.workingDirectoryField.selectedItem?.toString() ?: ""
                                AppSettingsState.instance.recentWorkingDirs.remove(workingDir)
                                AppSettingsState.instance.recentWorkingDirs.add(0, workingDir)
                                AppSettingsState.instance.recentWorkingDirs =
                                    AppSettingsState.instance.recentWorkingDirs.take(MAX_RECENT_DIRS).toMutableList()
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
                                exitCodeOption = if (settingsUI.exitCodeZero?.component?.isSelected == true) "0" else if (settingsUI.exitCodeAny?.component?.isSelected == true) "any" else "nonzero",
                                autoFix = settingsUI.autoFixCheckBox.isSelected,
                                maxRetries = settingsUI.maxRetriesField.value as Int,
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
                    api = api,
                    files = files.map { it.toFile }.toTypedArray(),
                    model = AppSettingsState.instance.smartModel.chatModel()
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
                val server = AppServer.getServer(event.project)
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
        private const val TEXT_AREA_ROWS = 3

        class SettingsUI(val workingDirectory: File, val folders: List<Path>) {
            val commandsPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
            }
            val commandsList = mutableListOf<CommandPanel>()
            val savedConfigsCombo = ComboBox<String>().apply {
                preferredSize = Dimension(200, 30)
                AppSettingsState.instance.savedCommandConfigs.keys.sorted().forEach { addItem(it) }
            }
            var exitCodeNonZero: Cell<JBRadioButton>? = null
            var exitCodeZero: Cell<JBRadioButton>? = null
            var exitCodeAny: Cell<JBRadioButton>? = null

            init {
                addCommandPanel()
            }

            fun addCommandPanel() {
                val cmdPanel = CommandPanel(workingDirectory, folders)
                commandsList.add(cmdPanel)
                commandsPanel.add(cmdPanel)
                commandsPanel.revalidate()
                commandsPanel.repaint()
            }

            fun removeCommandPanel(panel: CommandPanel) {
                commandsList.remove(panel)
                commandsPanel.remove(panel)
                commandsPanel.revalidate()
                commandsPanel.repaint()
                SwingUtilities.getWindowAncestor(commandsPanel)?.pack()
            }

            val maxRetriesField = JSpinner(SpinnerNumberModel(3, 0, 10, 1)).apply {
                toolTipText = "Maximum number of auto-retry attempts (0-10)"
            }
            val additionalInstructionsField = JTextArea().apply {
                rows = TEXT_AREA_ROWS
                lineWrap = true
                wrapStyleWord = true
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
                    exitCodeOption = if (exitCodeZero?.component?.isSelected == true) "0" else if (exitCodeAny?.component?.isSelected == true) "any" else "nonzero",
                    autoFix = autoFixCheckBox.isSelected,
                    maxRetries = maxRetriesField.value as Int,
                    additionalInstructions = additionalInstructionsField.text
                )
                AppSettingsState.instance.savedCommandConfigs[configName] = config
                savedConfigsCombo.addItem(configName)
                savedConfigsCombo.selectedItem = configName
            }

            fun loadConfig(configName: String) {
                val config = AppSettingsState.instance.savedCommandConfigs[configName] ?: return
                commandsList.clear()
                commandsPanel.removeAll()
                config.commands.forEach {
                    val panel = CommandPanel(workingDirectory, folders)
                    panel.loadFromSettings(it)
                    commandsList.add(panel)
                    commandsPanel.add(panel)
                }
                exitCodeNonZero?.component?.isSelected = config.exitCodeOption == "nonzero"
                exitCodeZero?.component?.isSelected = config.exitCodeOption == "0"
                exitCodeAny?.component?.isSelected = config.exitCodeOption == "any"
                autoFixCheckBox.isSelected = config.autoFix
                maxRetriesField.value = config.maxRetries
                additionalInstructionsField.text = config.additionalInstructions
                commandsPanel.revalidate()
                commandsPanel.repaint()
            }


            class CommandPanel(workingDirectory: File, folders: List<Path>) : JPanel() {
                val workingDirectoryField = ComboBox<String>().apply {
                    isEditable = true
                    val items = mutableListOf<String>()
                    AppSettingsState.instance.recentWorkingDirs.forEach { addItem(it); items.add(it) }
                    if (AppSettingsState.instance.recentWorkingDirs.isEmpty()) {
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
                    maximumSize = preferredSize
                }
                val commandField = ComboBox(AppSettingsState.instance.executables.toTypedArray()).apply {
                    isEditable = true
                    maximumSize = preferredSize
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
                    AppSettingsState.instance.recentArguments.forEach { addItem(it) }
                    if (AppSettingsState.instance.recentArguments.isEmpty()) {
                        addItem("")
                    }
                    maximumSize = preferredSize
                }

                init {
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(5, 5, 5, 5),
                        BorderFactory.createEtchedBorder()
                    )
                    layout = BorderLayout()
                    val fieldsPanel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    }
                    // Command row
                    fieldsPanel.add(JPanel(BorderLayout(5, 0)).apply {
                        add(JLabel("Command:  "), BorderLayout.WEST)
                        add(commandField, BorderLayout.CENTER)
                        add(commandButton, BorderLayout.EAST)
                        maximumSize = preferredSize.apply { width = Int.MAX_VALUE }
                    })
                    fieldsPanel.add(Box.createVerticalStrut(5))
                    // Arguments row
                    fieldsPanel.add(JPanel(BorderLayout(5, 0)).apply {
                        add(JLabel("Arguments:"), BorderLayout.WEST)
                        add(argumentsField, BorderLayout.CENTER)
                        maximumSize = preferredSize.apply { width = Int.MAX_VALUE }
                    })
                    fieldsPanel.add(Box.createVerticalStrut(5))
                    // Working directory row
                    fieldsPanel.add(JPanel(BorderLayout(5, 0)).apply {
                        add(JLabel("Directory:"), BorderLayout.WEST)
                        add(workingDirectoryField, BorderLayout.CENTER)
                        add(workingDirectoryButton, BorderLayout.EAST)
                        maximumSize = preferredSize.apply { width = Int.MAX_VALUE }
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
                    workingDirectoryField.selectedItem = settings.workingDirectory?.absolutePath
                }

            }
        }

        /**
         * Dialog for configuring command autofix settings
         */

        class CommandSettingsDialog(project: Project?, private val settingsUI: SettingsUI) : DialogWrapper(project) {
            init {
                title = "Command Autofix Settings"
                init()
            }

            override fun createCenterPanel(): JComponent {
                return panel {
                    row("Saved Configs:") {
                        cell(settingsUI.savedConfigsCombo).align(Align.FILL)
                            .comment("Select a saved configuration to load or save current settings")
                        button("Save...") {
                            settingsUI.saveCurrentConfig()
                        }
                        button("Load") {
                            val selected = settingsUI.savedConfigsCombo.selectedItem as? String
                            if (selected != null) {
                                settingsUI.loadConfig(selected)
                            } else {
                                JOptionPane.showMessageDialog(
                                    null,
                                    "Please select a configuration to load",
                                    "No Configuration Selected",
                                    JOptionPane.WARNING_MESSAGE
                                )
                            }
                        }
                        button("Delete") {
                            val selected = settingsUI.savedConfigsCombo.selectedItem as? String
                            if (selected != null) {
                                val confirmResult = JOptionPane.showConfirmDialog(
                                    null,
                                    "Delete configuration '$selected'?",
                                    "Confirm Delete",
                                    JOptionPane.YES_NO_OPTION
                                )
                                if (confirmResult == JOptionPane.YES_OPTION) {
                                    AppSettingsState.instance.savedCommandConfigs.remove(selected)
                                    settingsUI.savedConfigsCombo.removeItem(selected)
                                }
                            } else {
                                JOptionPane.showMessageDialog(
                                    null,
                                    "Please select a configuration to delete",
                                    "No Configuration Selected",
                                    JOptionPane.WARNING_MESSAGE
                                )
                            }
                        }
                    }
                    row {
                        cell(settingsUI.commandsPanel)
                    }
                    row {
                        button("Add Command") {
                            settingsUI.addCommandPanel()
                        }
                        button("Remove Command") {
                            if (settingsUI.commandsList.size > 1) {
                                settingsUI.removeCommandPanel(settingsUI.commandsList.last())
                            }
                        }
                    }
                    row("Exit Code Options") {
                        // Radio buttons are already part of exitCodeOptions ButtonGroup
                        panel {
                            buttonsGroup {
                                row {
                                    settingsUI.exitCodeNonZero =
                                        radioButton("Fix commands that return nonzero exit code").apply {
                                            selected(true)
                                        }
                                }
                                row {
                                    settingsUI.exitCodeAny = radioButton("Fix commands regardless of exit code")
                                }
                                row {
                                    settingsUI.exitCodeZero = radioButton("Fix commands that return zero exit code")
                                }
                            }
                        }

                    }
                    row("Max Auto-Retries") {
                        cell(settingsUI.maxRetriesField)
                    }
                    row("Additional Instructions") {
                        cell(JScrollPane(settingsUI.additionalInstructionsField))
                    }
                    row {
                        cell(settingsUI.autoFixCheckBox)
                    }
                }
            }

            override fun doOKAction() {
                if (settingsUI.commandsList.isEmpty()) {
                    Messages.showErrorDialog("At least one command is required", "Validation Error")
                    return
                }
                super.doOKAction()
            }
        }

    }
}