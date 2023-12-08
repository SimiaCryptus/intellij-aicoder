package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.ApplicationEvents
import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.File
import java.io.FileOutputStream
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class NonReflectionAppSettingsConfigurable : AppSettingsConfigurable() {
  override fun apply() {
    super.apply()
    if (settingsInstance.apiLog) {
      val file = File(ApplicationEvents.pluginHome, "openai.log")
      if(AppSettingsState.auxiliaryLog != file) {
        file.deleteOnExit()
        AppSettingsState.auxiliaryLog = file
        IdeaOpenAIClient.instance.logStreams.add(FileOutputStream(file, true).buffered())
      }
    } else {
      AppSettingsState.auxiliaryLog = null
      IdeaOpenAIClient.instance.logStreams.retainAll { it.close(); false }
    }
  }

  override fun build(component: AppSettingsComponent): JComponent {
    val tabbedPane = com.intellij.ui.components.JBTabbedPane()

    // Basic Settings Tab
    val basicSettingsPanel = JPanel(BorderLayout()).apply {
      add(JPanel(BorderLayout()).apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Model:"))
          add(component.modelName)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Temperature:"))
          add(component.temperature)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Human Language:"))
          add(component.humanLanguage)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Token Counter:"))
          add(component.tokenCounter)
          add(component.clearCounter)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("API Key:"))
          add(component.apiKey)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Server Port:"))
          add(component.listeningPort)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Ignore Errors:"))
          add(component.suppressErrors)
        })
      }, BorderLayout.NORTH)
    }
    tabbedPane.addTab("Basic Settings", basicSettingsPanel)

    tabbedPane.addTab("Developer Tools", JPanel(BorderLayout()).apply {
      add(JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Developer Tools:"))
          add(component.devActions)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Edit API Requests:"))
          add(component.editRequests)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Enable API Log:"))
          add(component.apiLog)
          add(component.openApiLog)
          add(component.clearApiLog)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("API Base:"))
          add(component.apiBase)
        })
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel("Server Endpoint:"))
          add(component.listeningEndpoint)
        })
      }, BorderLayout.NORTH)
    })

    tabbedPane.addTab("File Actions", JPanel(BorderLayout()).apply {
      add(component.fileActions, BorderLayout.CENTER)
    })

    tabbedPane.addTab("Editor Actions", JPanel(BorderLayout()).apply {
      add(component.editorActions, BorderLayout.CENTER)
    })

    return tabbedPane
  }

  override fun write(settings: AppSettingsState, component: AppSettingsComponent) {
    try {
      component.tokenCounter.text = settings.tokenCounter.toString()
      component.humanLanguage.text = settings.humanLanguage
      component.listeningPort.text = settings.listeningPort.toString()
      component.listeningEndpoint.text = settings.listeningEndpoint
      component.suppressErrors.isSelected = settings.suppressErrors
      component.modelName.selectedItem = settings.modelName
      component.apiLog.isSelected = settings.apiLog
      component.devActions.isSelected = settings.devActions
      component.editRequests.isSelected = settings.editRequests
      component.temperature.text = settings.temperature.toString()
      component.apiKey.text = settings.apiKey
      component.apiBase.text = settings.apiBase
      component.editorActions.read(settings.editorActions)
      component.fileActions.read(settings.fileActions)
    } catch (e: Exception) {
      log.warn("Error setting UI", e)
    }
  }

  override fun read(component: AppSettingsComponent, settings: AppSettingsState) {
    try {
      settings.tokenCounter = component.tokenCounter.text.safeInt()
      settings.humanLanguage = component.humanLanguage.text
      settings.listeningPort = component.listeningPort.text.safeInt()
      settings.listeningEndpoint = component.listeningEndpoint.text
      settings.suppressErrors = component.suppressErrors.isSelected
      settings.modelName = component.modelName.selectedItem as String
      settings.apiLog = component.apiLog.isSelected
      settings.devActions = component.devActions.isSelected
      settings.editRequests = component.editRequests.isSelected
      settings.temperature = component.temperature.text.safeDouble()
      settings.apiKey = String(component.apiKey.password)
      settings.apiBase = component.apiBase.text
      component.editorActions.write(settings.editorActions)
      component.fileActions.write(settings.fileActions)
    } catch (e: Exception) {
      log.warn("Error reading UI", e)
    }
  }

  companion object {
    val log = com.intellij.openapi.diagnostic.Logger.getInstance(NonReflectionAppSettingsConfigurable::class.java)
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
