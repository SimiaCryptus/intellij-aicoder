package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.options.Configurable
import java.util.*
import javax.swing.*

class AppSettingsConfigurable : Configurable {
    var settingsComponent: AppSettingsComponent? = null

    @Volatile
    private var mainPanel: JPanel? = null
    override fun getDisplayName(): String {
        return "AICoder Settings"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return Objects.requireNonNull(settingsComponent)?.preferredFocusedComponent
    }

    override fun createComponent(): JComponent? {
        if (null == mainPanel) {
            synchronized(this) {
                if (null == mainPanel) {
                    settingsComponent = AppSettingsComponent()
                    mainPanel = UITools.build(settingsComponent!!, false)
                }
            }
        }
        return mainPanel
    }


    override fun isModified(): Boolean {
        val buffer = AppSettingsState()
        if (settingsComponent != null) {
            UITools.readKotlinUI(settingsComponent!!, buffer)
            settingsComponent?.editorActions?.read(buffer.editorActions)
        }
        return buffer != AppSettingsState.instance
    }

    override fun apply() {
        if (settingsComponent != null) {
            UITools.readKotlinUI(settingsComponent!!, AppSettingsState.instance)
            settingsComponent?.editorActions?.read(AppSettingsState.instance.editorActions)
        }
    }

    override fun reset() {
        if (settingsComponent != null) {
            UITools.writeKotlinUI(settingsComponent!!, AppSettingsState.instance)
            settingsComponent?.editorActions?.write(AppSettingsState.instance.editorActions)
        }
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}



