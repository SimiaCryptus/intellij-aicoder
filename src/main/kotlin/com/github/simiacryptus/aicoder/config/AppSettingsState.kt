package com.github.simiacryptus.aicoder.config

import com.simiacryptus.openai.OpenAIClient.ChatRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.simiacryptus.openai.OpenAIClient
import org.slf4j.event.Level
import java.util.*
import java.util.Map
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.remove

@State(name = "org.intellij.sdk.settings.AppSettingsState", storages = [Storage("SdkSettingsPlugin.xml")])
class AppSettingsState : PersistentStateComponent<AppSettingsState?> {
    var apiBase = "https://api.openai.com/v1"
    var apiKey = ""
    var temperature = 0.1
    var style = ""
    var useGPT4 = false

    @Suppress("unused")
    var tokenCounter = 0
    private val mostUsedHistory: MutableMap<String, Int> = HashMap()
    private val mostRecentHistory: MutableList<String> = ArrayList()
    var historyLimit = 10
    var humanLanguage = "English"
    var apiLogLevel = Level.DEBUG
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
        if (apiLogLevel != that.apiLogLevel) return false
        if (devActions != that.devActions) return false
        return style == that.style
    }

    override fun hashCode(): Int {
        return Objects.hash(
            apiBase,
            apiKey,
            temperature,
            useGPT4,
            apiLogLevel,
            devActions,
            style
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

        // If the instruction history is bigger than the history limit,
        // We'll make a set of strings to retain,
        // We'll sort the instruction history by its value,
        // And limit it to the history limit,
        // Then we'll map the entry key and collect it in a set,
        // Then we'll make a new hash set of the instruction history keys,
        // And remove all the ones we want to retain,
        // Then we'll remove all the ones we don't want to keep,
        // And that's how we'll make sure the instruction history is neat!
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