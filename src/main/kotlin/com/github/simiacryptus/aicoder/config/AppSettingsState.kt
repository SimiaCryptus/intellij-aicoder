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
import java.util.Map
import java.util.stream.Collectors
import kotlin.collections.component1
import kotlin.collections.component2

class SimpleEnvelope(var value: String? = null)

@Suppress("MemberVisibilityCanBePrivate")
@State(name = "org.intellij.sdk.settings.AppSettingsState", storages = [Storage("SdkSettingsPlugin.xml")])
class AppSettingsState : PersistentStateComponent<SimpleEnvelope> {
    var apiLog: Boolean = false
    var apiBase = "https://api.openai.com/v1"
    var apiKey = ""
    var temperature = 0.1
    var useGPT4 = true
    var tokenCounter = 0
    var humanLanguage = "English"
    var devActions = false
    var editRequests = false
    var apiThreads = 4
    val editorActions = ActionSettingsRegistry()
    val fileActions = ActionSettingsRegistry()


    class MRUItems {
        val mostUsedHistory: MutableMap<String, Int> = HashMap()
        val mostRecentHistory: MutableList<String> = ArrayList()
        var historyLimit = 10
        fun addInstructionToHistory(instruction: CharSequence) {
            synchronized(mostRecentHistory) {
                mostRecentHistory.add(instruction.toString())
                while (mostRecentHistory.size > historyLimit) {
                    mostRecentHistory.removeAt(0)
                }
            }
            synchronized(mostUsedHistory) {
                mostUsedHistory.put(
                    instruction.toString(),
                    (mostUsedHistory[instruction] ?: 0) + 1
                )
            }

            if (mostUsedHistory.size > historyLimit) {
                val retain = mostUsedHistory.entries.stream()
                    .sorted(Map.Entry.comparingByValue<String, Int>().reversed())
                    .limit(historyLimit.toLong())
                    .map { (key, _) -> key }.collect(
                        Collectors.toList()
                    )
                val toRemove = HashSet<CharSequence>(mostUsedHistory.keys)
                toRemove.removeAll(retain.toSet())
                toRemove.removeAll(mostRecentHistory.toSet())
                toRemove.forEach { key: CharSequence? ->
                    mostUsedHistory.remove(
                        key
                    )
                }
            }
        }

    }

    fun createChatRequest(): ChatRequest {
        return createChatRequest(defaultChatModel())
    }

    fun defaultChatModel() = if (useGPT4) OpenAIClient.Models.GPT4 else OpenAIClient.Models.GPT35Turbo

    fun createChatRequest(model: OpenAIClient.Model): ChatRequest {
        val chatRequest = ChatRequest()
        chatRequest.model = model.modelName
        chatRequest.temperature = temperature
        chatRequest.max_tokens = model.maxTokens
        return chatRequest
    }

    @JsonIgnore
    override fun getState(): SimpleEnvelope {
        return SimpleEnvelope(JsonUtil.toJson(this))
    }

    val _recentCustomEdits = mutableMapOf<String,MRUItems>()

    fun recentCustomEdits(id:String) = _recentCustomEdits.computeIfAbsent(id) { MRUItems() }

    override fun loadState(state: SimpleEnvelope) {
        state.value ?: return
        val fromJson = JsonUtil.fromJson<AppSettingsState>(state.value!!, AppSettingsState::class.java)
        XmlSerializerUtil.copyBean(fromJson, this)

        _recentCustomEdits.clear(); _recentCustomEdits.putAll(fromJson._recentCustomEdits)
        editorActions.actionSettings.clear(); editorActions.actionSettings.putAll(fromJson.editorActions.actionSettings)
        fileActions.actionSettings.clear(); fileActions.actionSettings.putAll(fromJson.fileActions.actionSettings)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AppSettingsState
        if (that.temperature.compareTo(temperature) != 0) return false
        if (humanLanguage != that.humanLanguage) return false
        if (apiBase != that.apiBase) return false
        if (apiKey != that.apiKey) return false
        if (useGPT4 != that.useGPT4) return false
        if (apiLog != that.apiLog) return false
        if (devActions != that.devActions) return false
        if (editRequests != that.editRequests) return false
        if (editorActions != that.editorActions) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(
            apiBase,
            apiKey,
            temperature,
            useGPT4,
            apiLog,
            devActions,
            editRequests,
            editorActions
        )
    }


    companion object {
        @JvmStatic
        val instance: AppSettingsState by lazy {
            val application = ApplicationManager.getApplication()
            if (null == application) AppSettingsState() else application.getService(AppSettingsState::class.java)
        }
    }
}
