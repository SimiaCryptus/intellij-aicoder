package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

abstract class UIAdapter<C : Any, S : Any>(
  protected val settingsInstance: S,
  protected var component: C? = null,
) : Configurable {

  @Volatile
  private var mainPanel: JComponent? = null
  override fun getDisplayName(): String {
    return "AICoder Settings"
  }

  override fun getPreferredFocusedComponent(): JComponent? = null

  override fun createComponent(): JComponent? {
    if (null == mainPanel) {
      synchronized(this) {
        if (null == mainPanel) {
          val component = newComponent()
          this.component = component
          mainPanel = build(component)
          write(settingsInstance, component)
        }
      }
    }
    return mainPanel
  }

  abstract fun newComponent(): C
  abstract fun newSettings(): S
  private fun getSettings(component : C? = this.component) = when (component) {
    null -> settingsInstance
    else -> {
      val buffer = newSettings()
      read(component, buffer)
      buffer
    }
  }

  override fun isModified() = when {
    component == null -> false
    getSettings() != settingsInstance -> true
    else -> false
  }

  override fun apply() {
    if (component != null) read(component!!, settingsInstance)
  }

  override fun reset() {
    if (component != null) write(settingsInstance, component!!)
  }

  override fun disposeUIResources() {
    val component = component
    this.component = null
    if(component != null && component is Disposable) component.dispose()
  }

  open fun build(component: C): JComponent =
    UITools.buildFormViaReflection(component, false)!!

  open fun read(component: C, settings: S) {
    UITools.readKotlinUIViaReflection(component, settings)
  }

  open fun write(settings: S, component: C) {
    UITools.writeKotlinUIViaReflection(settings, component)
  }

}