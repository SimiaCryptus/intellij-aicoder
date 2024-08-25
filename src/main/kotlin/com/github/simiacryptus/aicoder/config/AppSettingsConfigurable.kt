package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.util.UITools
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JScrollPane

open class AppSettingsConfigurable : UIAdapter<AppSettingsComponent, AppSettingsState>(AppSettingsState.instance) {
    override fun read(component: AppSettingsComponent, settings: AppSettingsState) {
        UITools.readKotlinUIViaReflection(component, settings)
    }

    override fun write(settings: AppSettingsState, component: AppSettingsComponent) {
        UITools.writeKotlinUIViaReflection(settings, component)
    }

    override fun getPreferredFocusedComponent() = component?.temperature

    override fun newComponent() = AppSettingsComponent()

    override fun newSettings() = AppSettingsState()
}