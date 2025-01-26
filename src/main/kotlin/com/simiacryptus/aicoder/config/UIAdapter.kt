package com.simiacryptus.aicoder.config

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.simiacryptus.aicoder.util.UITools
import org.slf4j.LoggerFactory
import javax.swing.JComponent

abstract class UIAdapter<C : Any, S : Any>(
  protected val settingsInstance: S,
  protected var component: C? = null,
) : Configurable {

  companion object {
    private val log = LoggerFactory.getLogger(UIAdapter::class.java)
  }

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
          try {
            val component = newComponent()
            this.component = component
            mainPanel = build(component)
            write(settingsInstance, component)
          } catch (e: Exception) {
            log.error("Error creating component", e)
          }
        }
      }
    }
    return mainPanel
  }

  abstract fun newComponent(): C
  abstract fun newSettings(): S
  private fun getSettings(component: C? = this.component) = try {
    when (component) {
      null -> settingsInstance
      else -> {
        val buffer = newSettings()
        read(component, buffer)
        buffer
      }
    }
  } catch (e: Exception) {
    log.error("Error reading settings", e)
    settingsInstance
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
    if (component != null && component is Disposable) component.dispose()
  }

  open fun build(component: C): JComponent =
    UITools.buildFormViaReflection(component, false)!!

  open fun read(component: C, settings: S) {
    UITools.readKotlinUIViaReflection(settings, component, Any::class)
  }

  open fun write(settings: S, component: C) {
    UITools.writeKotlinUIViaReflection(settings, component, Any::class)
  }

}