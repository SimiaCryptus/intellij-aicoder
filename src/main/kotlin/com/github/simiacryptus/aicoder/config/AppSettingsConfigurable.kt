package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.util.UITools

open class AppSettingsConfigurable : UIAdapter<AppSettingsComponent, AppSettingsState>(AppSettingsState.instance) {
  override fun read(component: AppSettingsComponent, settings: AppSettingsState) {
    UITools.readKotlinUIViaReflection(component, settings)
    component.editorActions.read(settings.editorActions)
    component.fileActions.read(settings.fileActions)
  }

  override fun write(settings: AppSettingsState, component: AppSettingsComponent) {
    UITools.writeKotlinUIViaReflection(settings, component)
    component.editorActions.write(settings.editorActions)
    component.fileActions.write(settings.fileActions)
  }

  override fun getPreferredFocusedComponent() = component?.temperature

  override fun newComponent() = AppSettingsComponent()

  override fun newSettings() = AppSettingsState()
}
