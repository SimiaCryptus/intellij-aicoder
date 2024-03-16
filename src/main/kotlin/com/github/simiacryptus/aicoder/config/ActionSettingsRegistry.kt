package com.github.simiacryptus.aicoder.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.simiacryptus.aicoder.ui.EditorMenu
import com.github.simiacryptus.aicoder.util.IdeaKotlinInterpreter
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import java.io.File
import java.util.stream.Collectors

class ActionSettingsRegistry {

  val actionSettings: MutableMap<String, ActionSettings> = HashMap()
  private val version = 2.0074

  private fun updateActionConfig(action: AnAction, code: String, language: String) {
    val actionConfig = this.getActionConfig(action)
    actionConfig.language = language
    actionConfig.isDynamic = false
    with(action) {
      templatePresentation.text = actionConfig.displayText
      templatePresentation.description = actionConfig.displayText
    }
    if (!actionConfig.enabled) {
      return
    } else if (!actionConfig.file.exists()
      || actionConfig.file.readText().isBlank()
      || (actionConfig.version ?: 0.0) < version
    ) {
      actionConfig.file.writeText(code)
      actionConfig.version = version
    } else {
      handleDynamicActionConfig(action, actionConfig, code)
    }
  }

  private fun handleDynamicActionConfig(action: AnAction, actionConfig: ActionSettings, code: String) {
    if (actionConfig.isDynamic || (actionConfig.version ?: 0.0) >= version) {
      val localCode = actionConfig.file.readText().dropWhile { !it.isLetter() }
      if (!localCode.equals(code)) {
        try {
          val element = actionConfig.buildAction(localCode)
          actionConfig.version = version
          actionConfig.file.writeText(code)
          throw ReplaceActionException()
        } catch (e: Throwable) {
          log.info("Error loading dynamic ${action.javaClass}", e)
        }
      }
    }
    val canLoad = try {
      ActionSettingsRegistry::class.java.classLoader.loadClass(actionConfig.id)
      true
    } catch (e: Throwable) {
      false
    }
    if (canLoad) {
      actionConfig.file.writeText(code)
      actionConfig.version = version
    } else {
      throw RemoveActionException()
    }
  }

  fun edit(superChildren: Array<out AnAction>): Array<AnAction> {
    val children = superChildren.toList().toMutableList()
    children.toTypedArray().forEach { action ->
      val language = "kt"
      val code: String? = load(action.javaClass, language)
      if (null != code) {
        try {
          updateActionConfig(action, code, language)
        } catch (e: RemoveActionException) {
          children.remove(action)
          if (e.newAction != null) children.add(e.newAction)
        } catch (e: Throwable) {
          UITools.error(log, "Error loading ${action.javaClass}", e)
        }
      }
    }
    this.getDynamicActions().forEach {
      try {
        if (!it.file.exists()) return@forEach
        if (!it.enabled) return@forEach
        val element = it.buildAction(it.file.readText())
        children.add(element)
      } catch (e: Throwable) {
        UITools.error(log, "Error loading dynamic action", e)
      }
    }
    return children.toTypedArray()
  }

  class ReplaceActionException : Exception()
  class RemoveActionException(val newAction: AnAction? = null) : Exception()

  class DynamicActionException(
    cause: Throwable,
    msg: String,
    val file: File,
    val actionSetting: ActionSettings
  ) : Exception(msg, cause)

  data class ActionSettings(
    val id: String, // Static property
    var enabled: Boolean = true, // User settable
    var displayText: String? = null, // User settable
    var version: Double? = null, // System property
    var isDynamic: Boolean = false, // Static property
    var language: String? = null, // Static property
  ) {

    fun buildAction(
      code: String
    ): AnAction = try {
      val newClassName = this.className + "_" + Integer.toHexString(code.hashCode())
      with(
        actionCache.getOrPut("$packageName.$newClassName") {
          (compile(
            code.replace(
              ("""(?<![\w\d])$className(?![\w\d])""").toRegex(),
              newClassName
            ) + "\n" + newClassName + "::class.java"
          ).getDeclaredConstructor().newInstance() as AnAction)
        }
      ) {
        templatePresentation.text = displayText
        templatePresentation.description = displayText
        this
      }
    } catch (e: Throwable) {
      throw DynamicActionException(e, "Error in Action " + displayText, file, this)
    }

    private fun compile(code: String): Class<*> {
      try {
        val kotlinInterpreter = IdeaKotlinInterpreter(mapOf())
        val scriptEngine = kotlinInterpreter.scriptEngine
        val eval = scriptEngine.eval(code)
        return eval as Class<*>
      } catch (e: Throwable) {
        throw DynamicActionException(e, "Error in Action " + displayText, file, this)
      }
    }

    private val packageName: String get() = id.substringBeforeLast('.')
    val className: String get() = id.substringAfterLast('.')

    val file: File
      get() {
        val file = File(configDir(), "aicoder/actions/${packageName.replace('.', '/')}/$className.$language")
        file.parentFile.mkdirs()
        return file
      }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as ActionSettings

      if (id != other.id) return false
      if (enabled != other.enabled) return false
      if (displayText != other.displayText) return false
      if (isDynamic != other.isDynamic) return false
      return language == other.language
    }

    override fun hashCode(): Int {
      var result = id.hashCode()
      result = 31 * result + enabled.hashCode()
      result = 31 * result + (displayText?.hashCode() ?: 0)
      result = 31 * result + isDynamic.hashCode()
      result = 31 * result + (language?.hashCode() ?: 0)
      return result
    }

  }

  private fun getActionConfig(action: AnAction): ActionSettings {
    return actionSettings.getOrPut(action.javaClass.name) {
      val actionConfig = ActionSettings(action.javaClass.name)
      actionConfig.displayText = action.templatePresentation.text
      actionConfig
    }
  }

  @JsonIgnore
  fun getDynamicActions(): List<ActionSettings> {
    return actionSettings.entries.stream().filter { it.value.isDynamic && it.value.enabled }.map { it.value }
      .collect(Collectors.toList())
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as ActionSettingsRegistry
    return actionSettings == other.actionSettings
  }

  override fun hashCode(): Int {
    return actionSettings.hashCode()
  }

  companion object {

    private val log = org.slf4j.LoggerFactory.getLogger(ActionSettingsRegistry::class.java)

    val actionCache = HashMap<String, AnAction>()
    private fun load(actionPackage: String, actionName: String, language: String) =
      load("/sources/${language}/$actionPackage/$actionName.$language")

    private fun load(path: String): String? {
      val bytes = EditorMenu::class.java.getResourceAsStream(path)?.readAllBytes()
      return bytes?.toString(Charsets.UTF_8)?.dropWhile { !it.isLetter() }
    }

    fun load(clazz: Class<AnAction>, language: String) =
      load(clazz.`package`.name.replace('.', '/'), clazz.simpleName, language)

    fun configDir(): File {
      var baseDir = System.getProperty("idea.config.path")
      if (baseDir == null) baseDir = System.getProperty("user.home")
      return File(baseDir)
    }
  }

}
