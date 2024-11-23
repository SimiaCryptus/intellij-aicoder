package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.general.CmdPatchApp
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import javax.swing.*
import kotlin.collections.set

class CommandAutofixAction : BaseAction() {
    /**
     * Returns the thread that should be used for action update.
     */
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
                val settings = getUserSettings(event) ?: return@runAsync
                val dataContext = event.dataContext
                val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
                setupAndLaunchSession(event, settings, virtualFiles)
            }
        } catch (e: Throwable) {
            log.error("Failed to execute command autofix", e)
            UITools.showErrorDialog(event.project, "Failed to execute command autofix: ${e.message}", "Error")
        }
    }
    /**
     * Sets up and launches the patch app session
     */
    private fun setupAndLaunchSession(event: AnActionEvent, settings: PatchApp.Settings, virtualFiles: Array<VirtualFile>?) {
        val folder = UITools.getSelectedFolder(event)
        val root = if (null != folder) {
            folder.toFile.toPath()
        } else {
            event.project?.basePath?.let { File(it).toPath() }
        }!!
        val session = Session.newGlobalID()
        val patchApp = CmdPatchApp(
            root,
            settings,
            api,
            virtualFiles?.map { it.toFile }?.toTypedArray(),
            AppSettingsState.instance.smartModel.chatModel()
        )
        SessionProxyServer.chats[session] = patchApp
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Code Chat",
            singleInput = true,
            stickyInput = false,
            loadImages = false,
            showMenubar = false
        )
        SessionProxyServer.metadataStorage.setSessionName(null, session, "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}")
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

        private fun getUserSettings(event: AnActionEvent?): PatchApp.Settings? {
            val root = UITools.getSelectedFolder(event ?: return null)?.toNioPath() ?: event.project?.basePath?.let {
                File(it).toPath()
            }
            val files = UITools.getSelectedFiles(event).map { it.path.let { File(it).toPath() } }.toMutableSet()
            if (files.isEmpty()) Files.walk(root)
                .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
                .toList().filterNotNull().forEach { files.add(it) }
            var settings: PatchApp.Settings? = null
            SwingUtilities.invokeAndWait {
                val settingsUI = SettingsUI(root!!.toFile())
                val dialog = CommandSettingsDialog(event.project, settingsUI)
                dialog.show()
                settings = if (dialog.isOK) {
                val executable = File(settingsUI.commandField.selectedItem?.toString() ?: throw IllegalArgumentException("No executable selected"))
                AppSettingsState.instance.executables += executable.absolutePath
                val argument = settingsUI.argumentsField.selectedItem?.toString() ?: ""
                AppSettingsState.instance.recentArguments.remove(argument)
                AppSettingsState.instance.recentArguments.add(0, argument)
                AppSettingsState.instance.recentArguments =
                    AppSettingsState.instance.recentArguments.take(10).toMutableList()
                PatchApp.Settings(
                    executable = executable,
                    arguments = argument,
                    workingDirectory = File(settingsUI.workingDirectoryField.text),
                    exitCodeOption = if (settingsUI.exitCodeZero.isSelected) "0" else if (settingsUI.exitCodeAny.isSelected) "any" else "nonzero",
                    additionalInstructions = settingsUI.additionalInstructionsField.text,
                    autoFix = settingsUI.autoFixCheckBox.isSelected
                )
            } else {
                null
            }
            }
            return settings
        }

        class SettingsUI(root: File) {
            val argumentsField = ComboBox<String>().apply {
                isEditable = true
                AppSettingsState.instance.recentArguments.forEach { addItem(it) }
                if (AppSettingsState.instance.recentArguments.isEmpty()) {
                    addItem("run build")
                }
            }
            val commandField = ComboBox(AppSettingsState.instance.executables.toTypedArray()).apply {
                isEditable = true
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
            val workingDirectoryField = JTextField(root.absolutePath).apply {
                isEditable = true
            }
            val workingDirectoryButton = JButton("...").apply {
                addActionListener {
                    val fileChooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        isMultiSelectionEnabled = false
                        this.selectedFile = File(workingDirectoryField.text)
                    }
                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        workingDirectoryField.text = fileChooser.selectedFile.absolutePath
                    }
                }
            }
            val exitCodeOptions = ButtonGroup()
            val exitCodeNonZero = JRadioButton("Patch nonzero exit code", true)
            val exitCodeZero = JRadioButton("Patch 0 exit code")
            val exitCodeAny = JRadioButton("Patch any exit code")
            val additionalInstructionsField = JTextArea().apply {
                rows = 3
                lineWrap = true
                wrapStyleWord = true
            }
            val autoFixCheckBox = JCheckBox("Auto-apply fixes").apply {
                isSelected = false
            }
        }

        class CommandSettingsDialog(project: Project?, private val settingsUI: SettingsUI) : DialogWrapper(project) {
            init {
                settingsUI.exitCodeOptions.add(settingsUI.exitCodeNonZero)
                settingsUI.exitCodeOptions.add(settingsUI.exitCodeZero)
                settingsUI.exitCodeOptions.add(settingsUI.exitCodeAny)
                title = "Command Autofix Settings"
                init()
            }

            override fun createCenterPanel(): JComponent {
                val panel = JPanel(BorderLayout()).apply {

                    val optionsPanel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        add(JLabel("Executable"))
                        add(JPanel(BorderLayout()).apply {
                            add(settingsUI.commandField, BorderLayout.CENTER)
                            add(settingsUI.commandButton, BorderLayout.EAST)
                        })
                        add(JLabel("Arguments"))
                        add(settingsUI.argumentsField)
                        add(JLabel("Working Directory"))
                        add(JPanel(BorderLayout()).apply {
                            add(settingsUI.workingDirectoryField, BorderLayout.CENTER)
                            add(settingsUI.workingDirectoryButton, BorderLayout.EAST)
                        })
                        add(JLabel("Exit Code Options"))
                        add(settingsUI.exitCodeNonZero)
                        add(settingsUI.exitCodeAny)
                        add(settingsUI.exitCodeZero)
                        add(JLabel("Additional Instructions"))
                        add(JScrollPane(settingsUI.additionalInstructionsField))
                        add(settingsUI.autoFixCheckBox)
                    }
                    add(optionsPanel, BorderLayout.SOUTH)
                }
                return panel
            }
        }

    }
}