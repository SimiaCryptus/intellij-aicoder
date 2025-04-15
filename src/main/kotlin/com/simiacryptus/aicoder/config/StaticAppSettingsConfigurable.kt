package com.simiacryptus.aicoder.config

import com.intellij.util.xmlb.XmlSerializerUtil
import com.simiacryptus.aicoder.PluginStartupActivity.Companion.addUserSuppliedModels
import com.simiacryptus.aicoder.config.AppSettingsState.UserSuppliedModel
import com.simiacryptus.aicoder.util.IdeaChatClient
import com.simiacryptus.jopenai.models.APIProvider
import com.simiacryptus.util.EncryptionUtil
import com.simiacryptus.util.JsonUtil
import com.simiacryptus.util.JsonUtil.fromJson
import com.simiacryptus.util.toJson
import java.awt.*
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel

class StaticAppSettingsConfigurable : AppSettingsConfigurable() {
  override fun apply() {
    super.apply()
    addUserSuppliedModels(settingsInstance.userSuppliedModels
      ?.map { fromJson(it, UserSuppliedModel::class.java) } ?: emptyList())
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
  
  private val password = JPasswordField()
  
  override fun build(component: AppSettingsComponent): JComponent {
    val tabbedPane = com.intellij.ui.components.JBTabbedPane()
    try {
      tabbedPane.addTab("Basic Settings", JPanel(BorderLayout()).apply {
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
            add(JLabel("Image Model:"))
            add(component.mainImageModel)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Temperature:"))
            add(component.temperature)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Executables:"))
            add(component.executablesPanel)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Password:"))
            add(password) 
            add(JLabel("Configuration:"))
            add(JButton("Export Config").apply {
              addActionListener {
                showExportConfigDialog()
              }
            })
            add(JButton("Import Config").apply {
              addActionListener {
                showImportConfigDialog()
              }
            })
          })
        })
      })
    } catch (e: Exception) {
      log.warn("Error building Basic Settings", e)
    }

    try {
      tabbedPane.addTab("Keys", JPanel(BorderLayout()).apply {
        add(JPanel(BorderLayout()).apply {
          layout = BoxLayout(this, BoxLayout.Y_AXIS)
          add(JPanel(BorderLayout()).apply {
            add(JLabel("API Configurations:"), BorderLayout.NORTH)
            add(component.apis, BorderLayout.CENTER)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("GitHub Token:"))
            add(component.githubToken)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Google API Key:"))
            add(component.googleApiKey)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Google Search Engine ID:"))
            add(component.googleSearchEngineId)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("AWS Profile:"))
            add(component.awsProfile)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("AWS Region:"))
            add(component.awsRegion)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("AWS Bucket:"))
            add(component.awsBucket)
          })
        })
      })
    } catch (e: Exception) {
      log.warn("Error building Configuration", e)
    }

    tabbedPane.addTab("Advanced Settings", JPanel(BorderLayout()).apply {
      try {
        add(JPanel().apply {
          layout = BoxLayout(this, BoxLayout.Y_AXIS)
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Developer Tools:"))
            add(component.devActions)
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
            add(JLabel("Enable Diff Logging:"))
            add(component.diffLoggingEnabled)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Edit API Requests:"))
            add(component.editRequests)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            // Removed sections that reference non-existing components
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
              add(JLabel("Ignore Errors:"))
              add(component.suppressErrors)
            })
          }, BorderLayout.NORTH)
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            //add(JLabel("Show Welcome Screen:"))
            add(component.showWelcomeScreen)
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
            add(JLabel("Enable Legacy Actions:"))
            add(component.enableLegacyActions)
          })
          add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Human Language:"))
            add(component.humanLanguage)
          })
        }, BorderLayout.NORTH)
      } catch (e: Exception) {
        log.warn("Error building Developer Tools", e)
      }
    })

    tabbedPane.addTab("OpenAI", JPanel(BorderLayout()).apply {
      try {
        add(JPanel().apply {
          layout = BoxLayout(this, BoxLayout.Y_AXIS)
          add(JPanel(BorderLayout()).apply {
            add(JLabel("Store Metadata (JSON):"), BorderLayout.NORTH)
            val scrollPane = JScrollPane(component.storeMetadata)
            scrollPane.preferredSize = Dimension(300, 100)
            add(scrollPane, BorderLayout.CENTER)
          })
          add(JPanel(BorderLayout()).apply {
            add(JLabel("User-Supplied Models:"), BorderLayout.NORTH)
            add(JScrollPane(component.userSuppliedModels).apply {
              preferredSize = Dimension(500, 200)
            }, BorderLayout.CENTER)
            add(JPanel(GridBagLayout()).apply {
              val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
              }
              add(component.addUserModelButton, gbc)
              gbc.gridx++
              add(component.removeUserModelButton, gbc)
            }, BorderLayout.SOUTH)
          })
        }, BorderLayout.NORTH)
      } catch (e: Exception) {
        log.warn("Error building Developer Tools", e)
      }
    })

/*
    tabbedPane.addTab("Usage", JPanel(BorderLayout()).apply {
      try {
        add(component.usage, BorderLayout.CENTER)
      } catch (e: Exception) {
        log.warn("Error building Usage", e)
      }
    })
*/

    return tabbedPane
  }
  private fun showExportConfigDialog() {
    val dialog = JDialog(null as Frame?, "Export Configuration", true)
    dialog.layout = BorderLayout()
    
   // Encrypt keys before converting to JSON
   val encryptedSettings = AppSettingsState.instance.copy()
   encryptedSettings.apiKeys?.replaceAll { k, v -> EncryptionUtil.encrypt(v, password.text) ?: v }
   val configJson = JsonUtil.toJson(encryptedSettings)

    val textArea = JTextArea(configJson).apply {
      lineWrap = true
      wrapStyleWord = true
      font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }
    dialog.add(JScrollPane(textArea), BorderLayout.CENTER)
    val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
    val copyButton = JButton("Copy to Clipboard")
    copyButton.addActionListener {
      textArea.selectAll()
      textArea.copy()
      JOptionPane.showMessageDialog(dialog, "Configuration copied to clipboard", "Success", JOptionPane.INFORMATION_MESSAGE)
    }
    val saveButton = JButton("Save to File")
    saveButton.addActionListener {
      val fileChooser = JFileChooser().apply {
        dialogTitle = "Save Configuration"
        fileFilter = FileNameExtensionFilter("JSON Files", "json")
      }
      if (fileChooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
        val file = fileChooser.selectedFile
        val filePath = if (!file.name.lowercase().endsWith(".json")) {
          File("${file.absolutePath}.json")
        } else {
          file
        }
        try {
          FileWriter(filePath).use { writer ->
            writer.write(textArea.text)
          }
          JOptionPane.showMessageDialog(dialog, "Configuration saved to ${filePath.absolutePath}", "Success", JOptionPane.INFORMATION_MESSAGE)
        } catch (e: Exception) {
          JOptionPane.showMessageDialog(dialog, "Error saving configuration: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
          log.error("Error saving configuration", e)
        }
      }
    }
    val closeButton = JButton("Close")
    closeButton.addActionListener {
      dialog.dispose()
    }
    buttonPanel.add(copyButton)
    buttonPanel.add(saveButton)
    buttonPanel.add(closeButton)
    dialog.add(buttonPanel, BorderLayout.SOUTH)
    dialog.preferredSize = Dimension(800, 600)
    dialog.pack()
    dialog.setLocationRelativeTo(null)
    dialog.isVisible = true
  }
  private fun showImportConfigDialog() {
    val dialog = JDialog(null as Frame?, "Import Configuration", true)
    dialog.layout = BorderLayout()
    val textArea = JTextArea().apply {
      lineWrap = true
      wrapStyleWord = true
      font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }
    dialog.add(JScrollPane(textArea), BorderLayout.CENTER)
    val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
    val pasteButton = JButton("Paste from Clipboard")
    pasteButton.addActionListener {
      textArea.paste()
    }
    val loadButton = JButton("Load from File")
    loadButton.addActionListener {
      val fileChooser = JFileChooser().apply {
        dialogTitle = "Load Configuration"
        fileFilter = FileNameExtensionFilter("JSON Files", "json")
      }
      if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
        try {
          FileReader(fileChooser.selectedFile).use { reader ->
            textArea.text = reader.readText()
          }
        } catch (e: Exception) {
          JOptionPane.showMessageDialog(dialog, "Error loading configuration: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
          log.error("Error loading configuration", e)
        }
      }
    }
    val applyButton = JButton("Apply Configuration")
    applyButton.addActionListener {
      try {
        val importedSettings = fromJson<AppSettingsState>(textArea.text, AppSettingsState::class.java)
        // Confirm before applying
        val confirm = JOptionPane.showConfirmDialog(
          dialog, 
          "Are you sure you want to apply this configuration? This will overwrite your current settings.",
          "Confirm Import",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE
        )
        if (confirm == JOptionPane.YES_OPTION) {
          importedSettings.apiKeys?.replaceAll { k, v -> EncryptionUtil.decrypt(v, password.text) ?: v }
          XmlSerializerUtil.copyBean(importedSettings, AppSettingsState.instance)
          addUserSuppliedModels(importedSettings.userSuppliedModels?.map { fromJson(it, UserSuppliedModel::class.java) } ?: emptyList())
          // Update API keys and bases
          importedSettings.apiKeys?.forEach { (provider, key) ->
            AppSettingsState.instance.apiKeys?.put(provider, key)
          }
          importedSettings.apiBase?.forEach { (provider, base) ->
            AppSettingsState.instance.apiBase?.put(provider, base)
          }
          
          // Update UI to reflect imported settings
          write(AppSettingsState.instance, component!!)
          
          JOptionPane.showMessageDialog(
            dialog,
            "Configuration applied successfully. Please restart the IDE for all changes to take effect.",
            "Success",
            JOptionPane.INFORMATION_MESSAGE
          )
          dialog.dispose()
        }
      } catch (e: Exception) {
        JOptionPane.showMessageDialog(dialog, "Error applying configuration: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        log.error("Error applying configuration", e)
      }
    }
    val closeButton = JButton("Cancel")
    closeButton.addActionListener {
      dialog.dispose()
    }
    buttonPanel.add(pasteButton)
    buttonPanel.add(loadButton)
    buttonPanel.add(applyButton)
    buttonPanel.add(closeButton)
    dialog.add(buttonPanel, BorderLayout.SOUTH)
    dialog.preferredSize = Dimension(800, 600)
    dialog.pack()
    dialog.setLocationRelativeTo(null)
    dialog.isVisible = true
  }

  override fun write(settings: AppSettingsState, component: AppSettingsComponent) {
    try {
      component.diffLoggingEnabled.isSelected = settings.diffLoggingEnabled
      component.githubToken.text = settings.githubToken ?: ""
      component.googleApiKey.text = settings.googleApiKey ?: ""
      component.googleSearchEngineId.text = settings.googleSearchEngineId ?: ""
      component.awsProfile.text = settings.awsProfile ?: ""
      component.awsRegion.text = settings.awsRegion ?: ""
      component.awsBucket.text = settings.awsBucket ?: ""
      component.humanLanguage.text = settings.humanLanguage
      component.listeningPort.text = settings.listeningPort.toString()
      component.listeningEndpoint.text = settings.listeningEndpoint
      component.suppressErrors.isSelected = settings.suppressErrors
      component.disableAutoOpenUrls.isSelected = settings.disableAutoOpenUrls
      component.fastModel.selectedItem = settings.fastModel
      component.smartModel.selectedItem = settings.smartModel
      component.apiLog.isSelected = settings.apiLog
      component.devActions.isSelected = settings.devActions
      component.editRequests.isSelected = settings.editRequests
      component.mainImageModel.selectedItem = settings.mainImageModel
      component.storeMetadata.text = settings.storeMetadata ?: ""
      component.temperature.text = settings.temperature.toString()
      component.pluginHome.text = settings.pluginHome.absolutePath
      component.shellCommand.text = settings.shellCommand
      component.showWelcomeScreen.isSelected = settings.showWelcomeScreen
      component.enableLegacyActions.isSelected = settings.enableLegacyActions
      component.setExecutables(settings.executables ?: emptySet())
      component.setUserSuppliedModels(settings.userSuppliedModels?.map { fromJson(it, UserSuppliedModel::class.java) } ?: emptyList())
      val model = component.apis.model as DefaultTableModel
      model.rowCount = 0 // Clear existing rows
      model.rowCount = 0 // Clear existing rows
      val apiKeys = settings.apiKeys
      if(null == apiKeys) {
        log.warn("API keys are null")
        return
      }
      val apiBase = settings.apiBase
      if(null == apiBase) {
        log.warn("API base is null")
        return
      }
      APIProvider.values().forEach { value ->
        val name = value.name
        var key = apiKeys[name]
        if (key == null) {
          log.debug("Key is null for provider: $name")
          key = ""
        }
        var url = apiBase[name]
        if (url == null) {
          log.debug("URL is null for provider: $name")
          url = value.base
        }
        log.debug("Adding row to table model: $name, $key, $url")
        model.addRow(arrayOf(name, key, url))
      }
    } catch (e: Exception) {
      log.warn("Error setting UI", e)
    }
  }

  override fun read(component: AppSettingsComponent, settings: AppSettingsState) {
    try {
      settings.diffLoggingEnabled = component.diffLoggingEnabled.isSelected
      settings.githubToken = component.githubToken.text.takeIf { it.isNotBlank() }
      settings.googleApiKey = component.googleApiKey.text.takeIf { it.isNotBlank() }
      settings.googleSearchEngineId = component.googleSearchEngineId.text.takeIf { it.isNotBlank() }
      settings.awsProfile = component.awsProfile.text.takeIf { it.isNotBlank() }
      settings.awsRegion = component.awsRegion.text.takeIf { it.isNotBlank() }
      settings.awsBucket = component.awsBucket.text.takeIf { it.isNotBlank() }
      settings.executables?.clear()
      settings.executables?.plusAssign(component.getExecutables().toMutableSet())
      settings.humanLanguage = component.humanLanguage.text
      settings.listeningPort = component.listeningPort.text.safeInt()
      settings.listeningEndpoint = component.listeningEndpoint.text
      settings.suppressErrors = component.suppressErrors.isSelected
      settings.fastModel = component.fastModel.selectedItem as String
      settings.smartModel = component.smartModel.selectedItem as String
      settings.apiLog = component.apiLog.isSelected
      settings.devActions = component.devActions.isSelected
      settings.editRequests = component.editRequests.isSelected
      settings.disableAutoOpenUrls = component.disableAutoOpenUrls.isSelected
      settings.temperature = component.temperature.text.safeDouble()
      settings.storeMetadata = component.storeMetadata.text.takeIf { it.isNotBlank() }
      settings.mainImageModel = (component.mainImageModel.selectedItem as String)
      settings.pluginHome = File(component.pluginHome.text)
      settings.shellCommand = component.shellCommand.text
      settings.enableLegacyActions = component.enableLegacyActions.isSelected

      val tableModel = component.apis.model as DefaultTableModel
      log.debug("Reading API keys from table model: $tableModel with row count: ${tableModel.rowCount}")
      for (row in 0 until tableModel.rowCount) {
        val provider = tableModel.getValueAt(row, 0) as String
        val key = tableModel.getValueAt(row, 1) as String
        val base = tableModel.getValueAt(row, 2) as String
        log.info("Row $row: provider=$provider, key=$key, base=$base")
        if (key.isNotBlank()) {
         settings.apiKeys?.set(provider, key) ?: log.warn("API keys are blank")
          settings.apiBase?.set(provider, base) ?: log.warn("API base is blank")
        } else {
          log.info("Removing API key for provider: $provider")
          settings.apiKeys?.toMutableMap()?.apply { settings.apiKeys.remove(provider) }
          settings.apiBase?.toMutableMap()?.apply { settings.apiBase.remove(provider) }
        }
      }
      settings.showWelcomeScreen = component.showWelcomeScreen.isSelected
      settings.userSuppliedModels?.clear()
      settings.userSuppliedModels?.plusAssign(component.getUserSuppliedModels().map { it.toJson() }.toMutableList())
      log.info("Settings after reading: ${settings.toJson()}")
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