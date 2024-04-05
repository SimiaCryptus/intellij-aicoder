package com.github.simiacryptus.aicoder.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.simiacryptus.aicoder.ui.EditorMenu
import com.github.simiacryptus.aicoder.util.IdeaKotlinInterpreter
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import java.io.File

class ActionSettingsRegistry(
  val configDir: File
) {

   // Removed in-memory actionSettings map
  private val version = 2.0080  // Increment this value to force a reload of all actions

   private val objectMapper = jacksonObjectMapper()

  private fun updateActionConfig(action: AnAction, code: String, language: String) {
     var actionConfig = this.getActionConfig(action)
    log.info("Updating action config for action: ${action.javaClass.name}, language: $language")
    log.debug("Action Config before update: $actionConfig")
    actionConfig.language = language
    actionConfig.isDynamic = false
    with(action) {
      templatePresentation.text = actionConfig.displayText
      templatePresentation.description = actionConfig.displayText
    }
    log.debug("Action Config after basic update: $actionConfig")
    if (!actionConfig.enabled) {
      log.info("Action ${action.javaClass.name} is disabled, skipping further updates")
      return
    } else if (!actionConfig.file.exists()
      || actionConfig.file.readText().isBlank()
      || (actionConfig.version ?: 0.0) < version
    ) {
      log.info("Writing new code to action file for ${action.javaClass.name}")
      actionConfig.file.writeText(code)
      actionConfig.version = version
       saveActionConfig(actionConfig) // Save updated config to JSON file
    } else {
      handleDynamicActionConfig(action, actionConfig, code)
    }
  }

   private fun saveActionConfig(actionConfig: ActionSettings) {
     val configFile = File(configDir, "${actionConfig.packageName.replace('.', '/')}/${actionConfig.className}.json")
     configFile.parentFile.mkdirs()
     objectMapper.writeValue(configFile, actionConfig)
     log.info("Action config saved to ${configFile.path}")
   }

  private fun handleDynamicActionConfig(action: AnAction, actionConfig: ActionSettings, code: String) {
    log.debug("Entering handleDynamicActionConfig for ${action.javaClass.name}")
    if (actionConfig.isDynamic || (actionConfig.version ?: 0.0) >= version) {
      val localCode = actionConfig.file.readText().dropWhile { !it.isLetter() }
      log.debug("Local code read from file for ${action.javaClass.name}")
      if (!localCode.equals(code)) {
        try {
          log.info("Handling dynamic action config for action: ${action.javaClass.name}")
          log.debug("Dynamic code differs from existing, updating for ${action.javaClass.name}")
          val element = actionConfig.buildAction(localCode)
          actionConfig.version = version
          actionConfig.file.writeText(code)
          throw ReplaceActionException()
        } catch (e: Throwable) {
          log.info("Error loading dynamic ${action.javaClass}", e)
          log.error("Exception during dynamic action config handling for ${action.javaClass.name}", e)
        }
      }
    }
    val canLoad = try {
      ActionSettingsRegistry::class.java.classLoader.loadClass(actionConfig.id)
      log.debug("Successfully loaded class for action: ${actionConfig.id}")
      true
    } catch (e: Throwable) {
      log.error("Failed to load class for action: ${actionConfig.id}", e)
      false
    }
    if (canLoad) {
      log.info("Can load class for action: ${actionConfig.id}, updating code.")
      actionConfig.file.writeText(code)
      actionConfig.version = version
    } else {
      log.info("Cannot load class for action: ${actionConfig.id}, removing action.")
      throw RemoveActionException()
    }
  }

  fun edit(superChildren: Array<out AnAction>): Array<AnAction> {
    log.info("Starting edit process for actions")
    val children = superChildren.toList().toMutableList()
    children.toTypedArray().forEach { action ->
      val language = "kt"
      log.info("Editing action: ${action.javaClass.name}, language: $language")
      log.debug("Attempting to load code for action: ${action.javaClass.name}")
      val code: String? = load(action.javaClass, language)
      if (null != code) {
        try {
          updateActionConfig(action, code, language)
        } catch (e: RemoveActionException) {
          children.remove(action)
          log.info("Action removed due to RemoveActionException: ${action.javaClass.name}")
          if (e.newAction != null) children.add(e.newAction)
        } catch (e: Throwable) {
          UITools.error(log, "Error loading ${action.javaClass}", e)
          log.error("Exception caught during action editing: ${action.javaClass.name}", e)
        }
      }
    }
    this.getDynamicActions().forEach {
      try {
        if (!it.file.exists()) return@forEach
        log.info("Adding dynamic action: ${it.id}")
        if (!it.enabled) return@forEach
        log.debug("Building dynamic action: ${it.id}")
        val element = it.buildAction(it.file.readText())
        children.add(element)
      } catch (e: Throwable) {
        UITools.error(log, "Error loading dynamic action", e)
        log.error("Exception caught during dynamic action addition: ${it.id}", e)
      }
    }
    log.info("Edit process completed. Total actions: ${children.size}")
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
    val configDir: File,
    val id: String, // Static property
    var enabled: Boolean = true, // User settable
    // Adding logging within the buildAction method to log the action building process
    var displayText: String? = null, // User settable
    var version: Double? = null, // System property
    var isDynamic: Boolean = false, // Static property
    var language: String? = null, // Static property
    val packageName: String = id.substringBeforeLast('.'),
    val className: String = id.substringAfterLast('.'),
    val file: File = File(configDir, "${packageName.replace('.', '/')}/$className.${language ?: "kt"}").apply {
      parentFile.mkdirs()
    },
  ) {

    fun buildAction(
      code: String
    ): AnAction = try {
      val newClassName = this.className + "_" + Integer.toHexString(code.hashCode())
      with(
        actionCache.getOrPut("$packageName.$newClassName") {
          log.info("Compiling code for new action: $newClassName")
          val code1 = code.replace(
            ("""(?<![\w\d])$className(?![\w\d])""").toRegex(),
            newClassName
          ) + "\n" + newClassName + "::class.java"
          val anAction = compile(code1).getDeclaredConstructor().newInstance() as AnAction
          anAction
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
        log.info("Compiling Kotlin code for action: ${this.id}")
        val eval = scriptEngine.eval(code)
        return eval as Class<*>
      } catch (e: Throwable) {
        throw DynamicActionException(e, "Error in Action " + displayText, file, this)
      }
    }


    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as ActionSettings

      if (id != other.id) return false
      if (isDynamic != other.isDynamic) return false
      return language == other.language
    }


    override fun hashCode(): Int {
      var result = id.hashCode()
      result = 31 * result + isDynamic.hashCode()
      result = 31 * result + (language?.hashCode() ?: 0)
      return result
    }

  }

  private fun getActionConfig(action: AnAction): ActionSettings {
     val configFile = File(configDir, "${action.javaClass.`package`.name.replace('.', '/')}/${action.javaClass.simpleName}.json")
     return if (configFile.exists()) {
       objectMapper.readValue<ActionSettings>(configFile)
     } else {
      val actionConfig = ActionSettings(configDir, action.javaClass.name)
      log.info("Creating new action config for action: ${action.javaClass.name}")
      actionConfig.displayText = action.templatePresentation.text
       saveActionConfig(actionConfig) // Save new config to JSON file
      actionConfig
    }
  }

  @JsonIgnore
  fun getDynamicActions(): List<ActionSettings> {
     val dynamicActionConfigs = configDir.walk().filter { it.extension == "json" }.map { objectMapper.readValue<ActionSettings>(it) }
     return dynamicActionConfigs.filter { it.isDynamic && it.enabled }.toList()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
     // Removed comparison based on in-memory map
     return true
  }

  override fun hashCode(): Int {
     // Removed hash code based on in-memory map
     return super.hashCode()
  }

  val actionSettings: Map<String, ActionSettings>
    get() {
      val allActionSettings = mutableMapOf<String, ActionSettings>()
      configDir.walk().filter { it.extension == "json" }.forEach { file ->
        val actionSetting = objectMapper.readValue<ActionSettings>(file)
        allActionSettings[actionSetting.id] = actionSetting
      }
      return allActionSettings.toMap()
    }

  companion object {

    private val log = org.slf4j.LoggerFactory.getLogger(ActionSettingsRegistry::class.java)

     private val actionCache = HashMap<String, AnAction>()
    private fun load(actionPackage: String, actionName: String, language: String) =
      load("/sources/${language}/$actionPackage/$actionName.$language")

    private fun load(path: String): String? {
      val bytes = EditorMenu::class.java.getResourceAsStream(path)?.readAllBytes()
      return bytes?.toString(Charsets.UTF_8)?.dropWhile { !it.isLetter() }
    }

    fun load(clazz: Class<AnAction>, language: String) =
      load(clazz.`package`.name.replace('.', '/'), clazz.simpleName, language)


  }

}
