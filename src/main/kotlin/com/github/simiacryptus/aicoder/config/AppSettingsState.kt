package com.github.simiacryptus.aicoder.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.JsonUtil
import java.io.File

@State(name = "org.intellij.sdk.settings.AppSettingsState", storages = [Storage("SdkSettingsPlugin.xml")])
data class AppSettingsState(
  var temperature: Double = 0.1,
  var modelName: String = ChatModels.GPT35Turbo.modelName,
  var listeningPort: Int = 8081,
  var listeningEndpoint: String = "localhost",
  var humanLanguage: String = "English",
  var apiThreads: Int = 4,
  var apiBase: Map<String, String>? = mapOf("OpenAI" to "https://api.openai.com/v1"),
  var apiKey: Map<String, String>? = mapOf("OpenAI" to ""),
//    var tokenCounter: Int = 0,
  var modalTasks: Boolean = false,
  var suppressErrors: Boolean = false,
  var apiLog: Boolean = false,
  var devActions: Boolean = false,
  var editRequests: Boolean = false,
) : PersistentStateComponent<SimpleEnvelope> {

  private var onSettingsLoadedListeners = mutableListOf<() -> Unit>()
  val editorActions = ActionSettingsRegistry()
  val fileActions = ActionSettingsRegistry()
  private val recentCommands = mutableMapOf<String, MRUItems>()

  fun defaultChatModel(): ChatModels = ChatModels.values().entries.firstOrNull {
    it.value.modelName == modelName || it.key == modelName
  }?.value ?: throw IllegalArgumentException("Unknown model: $modelName")

  @JsonIgnore
  override fun getState() = SimpleEnvelope(JsonUtil.toJson(this))

  fun getRecentCommands(id: String) = recentCommands.computeIfAbsent(id) { MRUItems() }

  override fun loadState(state: SimpleEnvelope) {
    state.value ?: return
    val fromJson = try {
      JsonUtil.fromJson<AppSettingsState>(state.value!!, AppSettingsState::class.java)
    } catch (e: Exception) {
      //throw RuntimeException("Error loading settings: ${state.value}", e)
      AppSettingsState()
    }
    XmlSerializerUtil.copyBean(fromJson, this)
    recentCommands.clear();
    recentCommands.putAll(fromJson.recentCommands)
    editorActions.actionSettings.clear();
    editorActions.actionSettings.putAll(fromJson.editorActions.actionSettings)
    fileActions.actionSettings.clear();
    fileActions.actionSettings.putAll(fromJson.fileActions.actionSettings)
    notifySettingsLoaded()
  }

  fun addOnSettingsLoadedListener(listener: () -> Unit) {
    onSettingsLoadedListeners.add(listener)
  }

  private fun notifySettingsLoaded() {
    onSettingsLoadedListeners.forEach { it() }
  }

  companion object {
    var auxiliaryLog: File? = null

    @JvmStatic
    val instance: AppSettingsState by lazy {
      ApplicationManager.getApplication()?.getService(AppSettingsState::class.java) ?: AppSettingsState()
    }
  }
}
