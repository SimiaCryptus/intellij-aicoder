package aicoder.actions.agent

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.simiacryptus.aicoder.config.AppSettingsState
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JScrollPane

class CommandSettingsDialog(project: Project?, private val settingsUI: CommandAutofixAction.Companion.SettingsUI) : DialogWrapper(project) {
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
                            AppSettingsState.instance.savedCommandConfigsJson?.remove(selected)
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
                cell(settingsUI.maxRetriesSlider)
                    .comment("Adjust the maximum number of automatic retry attempts (0-10)")
            }
            row("Additional Instructions") {
                cell(JScrollPane(settingsUI.additionalInstructionsField))
            }
            row("API Budget") {
                cell(settingsUI.apiBudgetField)
                    .comment("Set the API budget for this session")
            }
            row {
                cell(settingsUI.autoFixCheckBox)
                cell(settingsUI.includeGitDiffsCheckBox)
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