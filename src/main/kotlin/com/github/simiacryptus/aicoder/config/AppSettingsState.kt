package com.github.simiacryptus.aicoder.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.OpenAIClient.ChatRequest
import com.simiacryptus.util.JsonUtil
import java.util.*

class SimpleEnvelope(var value: String? = null)

@State(name = "org.intellij.sdk.settings.AppSettingsState", storages = [Storage("SdkSettingsPlugin.xml")])
class AppSettingsState : PersistentStateComponent<SimpleEnvelope> {
    val listeningPort: Int = 8081
    val listeningEndpoint: String = "localhost"
    val modalTasks: Boolean = false
    var suppressErrors: Boolean = false
    var apiLog: Boolean = false
    var apiBase = "https://api.openai.com/v1"
    var apiKey = ""
    var temperature = 0.1
    var modelName : String = OpenAIClient.Models.GPT35Turbo.modelName
    var tokenCounter = 0
    var humanLanguage = "English"
    var devActions = false
    var editRequests = false
    var apiThreads = 4
    val editorActions = ActionSettingsRegistry()
    val fileActions = ActionSettingsRegistry()

    private val recentCommands = mutableMapOf<String,MRUItems>()

    fun createChatRequest(): ChatRequest {
        return createChatRequest(defaultChatModel())
    }

    fun defaultChatModel() = OpenAIClient.Models.entries.first { it.modelName == modelName }

    private fun createChatRequest(model: OpenAIClient.Model): ChatRequest {
        val chatRequest = ChatRequest()
        chatRequest.model = model.modelName
        chatRequest.temperature = temperature
        return chatRequest
    }

    @JsonIgnore
    override fun getState(): SimpleEnvelope {
        return SimpleEnvelope(JsonUtil.toJson(this))
    }

    fun getRecentCommands(id:String) = recentCommands.computeIfAbsent(id) { MRUItems() }

    override fun loadState(state: SimpleEnvelope) {
        state.value ?: return
        val fromJson = JsonUtil.fromJson<AppSettingsState>(state.value!!, AppSettingsState::class.java)
        XmlSerializerUtil.copyBean(fromJson, this)

        recentCommands.clear(); recentCommands.putAll(fromJson.recentCommands)
        editorActions.actionSettings.clear(); editorActions.actionSettings.putAll(fromJson.editorActions.actionSettings)
        fileActions.actionSettings.clear(); fileActions.actionSettings.putAll(fromJson.fileActions.actionSettings)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppSettingsState

        if (listeningPort != other.listeningPort) return false
        if (listeningEndpoint != other.listeningEndpoint) return false
        if (modalTasks != other.modalTasks) return false
        if (suppressErrors != other.suppressErrors) return false
        if (apiLog != other.apiLog) return false
        if (apiBase != other.apiBase) return false
        if (apiKey != other.apiKey) return false
        if (temperature != other.temperature) return false
        if (modelName != other.modelName) return false
        if (tokenCounter != other.tokenCounter) return false
        if (humanLanguage != other.humanLanguage) return false
        if (devActions != other.devActions) return false
        if (editRequests != other.editRequests) return false
        if (apiThreads != other.apiThreads) return false

        return true
    }

    override fun hashCode(): Int {
        var result = listeningPort
        result = 31 * result + listeningEndpoint.hashCode()
        result = 31 * result + modalTasks.hashCode()
        result = 31 * result + suppressErrors.hashCode()
        result = 31 * result + apiLog.hashCode()
        result = 31 * result + apiBase.hashCode()
        result = 31 * result + apiKey.hashCode()
        result = 31 * result + temperature.hashCode()
        result = 31 * result + modelName.hashCode()
        result = 31 * result + tokenCounter
        result = 31 * result + humanLanguage.hashCode()
        result = 31 * result + devActions.hashCode()
        result = 31 * result + editRequests.hashCode()
        result = 31 * result + apiThreads
        return result
    }

    companion object {
        @JvmStatic
        val instance: AppSettingsState by lazy {
            val application = ApplicationManager.getApplication()
            if (null == application) AppSettingsState() else application.getService(AppSettingsState::class.java)
        }
    }
}
