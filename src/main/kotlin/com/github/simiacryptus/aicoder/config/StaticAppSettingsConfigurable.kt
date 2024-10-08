package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.util.IdeaChatClient
import com.simiacryptus.jopenai.models.APIProvider
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.File
import java.io.FileOutputStream
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

class StaticAppSettingsConfigurable : AppSettingsConfigurable() {
    override fun apply() {
        super.apply()
        if (settingsInstance.apiLog) {
            val file = File(AppSettingsState.instance.pluginHome, "openai.log")
            if (AppSettingsState.auxiliaryLog?.absolutePath?.lowercase() != file.absolutePath.lowercase()) {
                file.deleteOnExit()
                AppSettingsState.auxiliaryLog = file
                IdeaChatClient.instance.logStreams.add(FileOutputStream(file, true).buffered())
            }
        } else {
            AppSettingsState.auxiliaryLog = null
            IdeaChatClient.instance.logStreams.retainAll { it.close(); false }
        }
    }

    override fun build(component: AppSettingsComponent): JComponent {
        val tabbedPane = com.intellij.ui.components.JBTabbedPane()

        try {// Basic Settings Tab
            val basicSettingsPanel = JPanel(BorderLayout()).apply {
                add(JPanel(BorderLayout()).apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Smart Model:"))
                        add(component.smartModel)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Fast Model:"))
                        add(component.fastModel)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Main Image Model:"))
                        add(component.mainImageModel)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Temperature:"))
                        add(component.temperature)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Human Language:"))
                        add(component.humanLanguage)
                    })
                    add(JPanel(BorderLayout()).apply {
                        add(JLabel("API Configurations:"), BorderLayout.NORTH)
                        add(component.apis, BorderLayout.CENTER)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Executables:"))
                        add(component.executablesPanel)
                    })
                })
            }
            tabbedPane.addTab("Basic Settings", basicSettingsPanel)
        } catch (e: Exception) {
            log.warn("Error building Basic Settings", e)
        }

        tabbedPane.addTab("Developer Tools", JPanel(BorderLayout()).apply {
            try {
                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Developer Tools:"))
                        add(component.devActions)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Enable Legacy Actions:"))
                        add(component.enableLegacyActions)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        // Removed sections that reference non-existing components
                        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                            add(JLabel("Ignore Errors:"))
                            add(component.suppressErrors)
                        })
                    }, BorderLayout.NORTH)
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Edit API Requests:"))
                        add(component.editRequests)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Disable Auto-Open URLs:"))
                        add(component.disableAutoOpenUrls)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Enable API Log:"))
                        add(component.apiLog)
                        add(component.openApiLog)
                        add(component.clearApiLog)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Server Port:"))
                        add(component.listeningPort)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Server Endpoint:"))
                        add(component.listeningEndpoint)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Plugin Home:"))
                        add(component.pluginHome)
                        add(component.choosePluginHome)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(JLabel("Shell Command:"))
                        add(component.shellCommand)
                    })
                    add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        //add(JLabel("Show Welcome Screen:"))
                        add(component.showWelcomeScreen)
                    })
                }, BorderLayout.NORTH)
            } catch (e: Exception) {
                log.warn("Error building Developer Tools", e)
            }
        })

        tabbedPane.addTab("Usage", JPanel(BorderLayout()).apply {
            try {
                add(component.usage, BorderLayout.CENTER)
            } catch (e: Exception) {
                log.warn("Error building Usage", e)
            }
        })

        return tabbedPane
    }

    override fun write(settings: AppSettingsState, component: AppSettingsComponent) {
        try {
            // Update the executables in the settings
            settings.executables = component.getExecutables().toMutableSet()
            component.humanLanguage.text = settings.humanLanguage
            component.listeningPort.text = settings.listeningPort.toString()
            component.listeningEndpoint.text = settings.listeningEndpoint
            component.suppressErrors.isSelected = settings.suppressErrors
//      component.modelName.selectedItem = settings.modelName
            component.fastModel.selectedItem = settings.fastModel
            component.smartModel.selectedItem = settings.smartModel
            component.apiLog.isSelected = settings.apiLog
            component.devActions.isSelected = settings.devActions
            component.editRequests.isSelected = settings.editRequests
            component.mainImageModel.selectedItem = settings.mainImageModel
            component.temperature.text = settings.temperature.toString()
            component.pluginHome.text = settings.pluginHome.absolutePath
            component.shellCommand.text = settings.shellCommand
            val model = component.apis.model as DefaultTableModel
            model.rowCount = 0 // Clear existing rows
            APIProvider.values().forEach { value ->
                val key = value.name
                model.addRow(arrayOf(key, settings.apiKey?.get(key) ?: "", settings.apiBase?.get(key) ?: value.base))
            }
            component.showWelcomeScreen.isSelected = settings.showWelcomeScreen
            component.enableLegacyActions.isSelected = settings.enableLegacyActions
            component.setExecutables(settings.executables)
        } catch (e: Exception) {
            log.warn("Error setting UI", e)
        }
    }

    override fun read(component: AppSettingsComponent, settings: AppSettingsState) {
        try {
            //      settings.modelName = component.modelName.selectedItem as String
            // Update the UI with the executables from the settings
            settings.executables = component.getExecutables().toMutableSet()
            settings.humanLanguage = component.humanLanguage.text
            settings.listeningPort = component.listeningPort.text.safeInt()
            settings.listeningEndpoint = component.listeningEndpoint.text
            settings.suppressErrors = component.suppressErrors.isSelected
            //      settings.modelName = component.modelName.selectedItem as String
            settings.fastModel = component.fastModel.selectedItem as String
            settings.smartModel = component.smartModel.selectedItem as String
            settings.apiLog = component.apiLog.isSelected
            settings.devActions = component.devActions.isSelected
            settings.editRequests = component.editRequests.isSelected
            settings.disableAutoOpenUrls = component.disableAutoOpenUrls.isSelected
            settings.temperature = component.temperature.text.safeDouble()
            settings.mainImageModel = (component.mainImageModel.selectedItem as String)
            settings.pluginHome = File(component.pluginHome.text)
            settings.shellCommand = component.shellCommand.text
            //      settings.modelName = component.modelName.selectedItem as String
            settings.enableLegacyActions = component.enableLegacyActions.isSelected

            val tableModel = component.apis.model as DefaultTableModel
            for (row in 0 until tableModel.rowCount) {
                val provider = tableModel.getValueAt(row, 0) as String
                val key = tableModel.getValueAt(row, 1) as String
                val base = tableModel.getValueAt(row, 2) as String
                if (key.isNotBlank()) {
//          settings.apiKey?.put(provider, key)
                    settings.apiKey = settings.apiKey?.toMutableMap()?.apply { put(provider, key) }
//          settings.apiBase?.put(provider, base)
                    settings.apiBase = settings.apiBase?.toMutableMap()?.apply { put(provider, base) }
                } else {
//          settings.apiKey.remove(provider)
                    settings.apiKey = settings.apiKey?.toMutableMap()?.apply { remove(provider) }
//          settings.apiBase.remove(provider)
                    settings.apiBase = settings.apiBase?.toMutableMap()?.apply {
                        remove(provider)
                    }
                }
            }
            settings.showWelcomeScreen = component.showWelcomeScreen.isSelected
        } catch (e: Exception) {
            log.warn("Error reading UI", e)
        }
    }

    companion object {
        val log = com.intellij.openapi.diagnostic.Logger.getInstance(StaticAppSettingsConfigurable::class.java)
    }
}

fun String?.safeInt() = if (null == this) 0 else when {
    isEmpty() -> 0
    else -> try {
        toInt()
    } catch (e: NumberFormatException) {
        0
    }
}

fun String?.safeDouble() = if (null == this) 0.0 else when {
    isEmpty() -> 0.0
    else -> try {
        toDouble()
    } catch (e: NumberFormatException) {
        0.0
    }


}