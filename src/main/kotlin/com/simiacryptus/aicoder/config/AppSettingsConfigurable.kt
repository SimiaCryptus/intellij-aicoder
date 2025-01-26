package com.simiacryptus.aicoder.config

import com.simiacryptus.aicoder.util.UITools

open class AppSettingsConfigurable : UIAdapter<AppSettingsComponent, AppSettingsState>(
  AppSettingsState.instance
) {
  override fun read(component: AppSettingsComponent, settings: AppSettingsState) {
    UITools.readKotlinUIViaReflection(settings = settings, component = component)
  }

  override fun write(settings: AppSettingsState, component: AppSettingsComponent) {
    UITools.writeKotlinUIViaReflection(settings, component, AppSettingsState::class)
  }

  override fun getPreferredFocusedComponent() = component?.temperature

  override fun newComponent() = AppSettingsComponent()

  override fun newSettings() = AppSettingsState()
}