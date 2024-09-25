package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.simiacryptus.skyenet.apps.general.CmdPatchApp
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import javax.swing.*
import kotlin.collections.set

class CommandAutofixAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(event: AnActionEvent) {
        val settings = getUserSettings(event) ?: return
        val dataContext = event.dataContext
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        val folder = UITools.getSelectedFolder(event)
        val root = if (null != folder) {
            folder.toFile.toPath()
        } else {
            event.project?.basePath?.let { File(it).toPath() }
        }!!
        val session = StorageInterface.newGlobalID()
        val patchApp = CmdPatchApp(root, session, settings, api, virtualFiles?.map { it.toFile }?.toTypedArray(), AppSettingsState.instance.defaultSmartModel())
        SessionProxyServer.chats[session] = patchApp
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Code Chat",
            singleInput = true,
            stickyInput = false,
            loadImages = false,
            showMenubar = false
        )
        val server = AppServer.getServer(event.project)
        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                Desktop.getDesktop().browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    override fun isEnabled(event: AnActionEvent) = true

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
            val settingsUI = SettingsUI(root!!.toFile())
            val dialog = CommandSettingsDialog(event.project, settingsUI)
            dialog.show()
            return if (dialog.isOK) {
                val executable = File(settingsUI.commandField.selectedItem?.toString() ?: return null)
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