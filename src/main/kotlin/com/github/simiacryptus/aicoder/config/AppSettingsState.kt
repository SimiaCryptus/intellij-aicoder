package com.github.simiacryptus.aicoder.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.OpenAIClient.ChatRequest
import java.util.*
import java.util.Map
import java.util.stream.Collectors
import kotlin.collections.component1
import kotlin.collections.component2

@Suppress("MemberVisibilityCanBePrivate")
@State(name = "org.intellij.sdk.settings.AppSettingsState", storages = [Storage("SdkSettingsPlugin.xml")])
class AppSettingsState : PersistentStateComponent<AppSettingsState?> {
    var apiLog: Boolean = false
    var apiBase = "https://api.openai.com/v1"
    var apiKey = ""
    var temperature = 0.1
    var useGPT4 = true
    var tokenCounter = 0
    private val mostUsedHistory: MutableMap<String, Int> = HashMap()
    private val mostRecentHistory: MutableList<String> = ArrayList()
    var historyLimit = 10
    var humanLanguage = "English"
    var devActions = false
    var apiThreads = 4

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

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
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
        )
    }

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
                (mostUsedHistory[instruction] ?:0) + 1
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

    val editHistory: Set<String>
        get() = mostUsedHistory.keys

    companion object {
        @JvmStatic
        val instance: AppSettingsState
            get() {
                val application = ApplicationManager.getApplication()
                return if (null == application) AppSettingsState() else application.getService(AppSettingsState::class.java)
            }
    }
}