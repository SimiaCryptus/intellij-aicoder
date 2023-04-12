package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.openai.translate.TranslationRequest
import com.github.simiacryptus.aicoder.openai.translate.TranslationRequestTemplate
import com.simiacryptus.openai.ChatRequest
import com.simiacryptus.openai.CompletionRequest
import com.simiacryptus.openai.EditRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
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
import kotlin.collections.getOrDefault
import kotlin.collections.remove

@State(name = "org.intellij.sdk.settings.AppSettingsState", storages = [Storage("SdkSettingsPlugin.xml")])
class AppSettingsState : PersistentStateComponent<AppSettingsState?> {
    var apiBase = "https://api.openai.com/v1"
    var apiKey = ""
    var model_completion = "text-davinci-003"
    var model_edit = "text-davinci-edit-001"
    var model_chat = "gpt-3.5-turbo-0301"
    var maxTokens = 1000
    var temperature = 0.1
    var style = ""

    @Suppress("unused")
    var tokenCounter = 0
    private val mostUsedHistory: MutableMap<String, Int> = HashMap()
    private val mostRecentHistory: MutableList<String> = ArrayList()
    var historyLimit = 10
    var humanLanguage = "English"
    var maxPrompt = 5000
    var translationRequestTemplate = TranslationRequestTemplate.XML
    var apiLogLevel = Level.DEBUG
    var devActions = false
    var suppressProgress = false
    var apiThreads = 4
    fun createTranslationRequest(): TranslationRequest {
        return translationRequestTemplate[this]
    }

    fun createCompletionRequest(): CompletionRequest {
        val completionRequest = CompletionRequest()
        completionRequest.temperature = temperature
        completionRequest.max_tokens = maxTokens
        return completionRequest
    }

    fun createEditRequest(): EditRequest {
        val editRequest = EditRequest()
        editRequest.setModel(model_edit)
        editRequest.setTemperature(temperature)
        return editRequest
    }

    fun createChatRequest(): ChatRequest {
        val chatRequest = ChatRequest()
        chatRequest.model = model_chat
        chatRequest.temperature = temperature
        chatRequest.max_tokens = maxTokens
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
        if (maxTokens != that.maxTokens) return false
        if (maxPrompt != that.maxPrompt) return false
        if (that.temperature.compareTo(temperature) != 0) return false
        if (humanLanguage != that.humanLanguage) return false
        if (apiBase != that.apiBase) return false
        if (apiKey != that.apiKey) return false
        if (model_completion != that.model_completion) return false
        if (model_edit != that.model_edit) return false
        if (model_chat != that.model_chat) return false
        if (translationRequestTemplate != that.translationRequestTemplate) return false
        if (apiLogLevel != that.apiLogLevel) return false
        if (devActions != that.devActions) return false
        return if (suppressProgress != that.suppressProgress) false else style == that.style
    }

    override fun hashCode(): Int {
        return Objects.hash(
            apiBase,
            apiKey,
            model_completion,
            model_edit,
            model_chat,
            maxTokens,
            temperature,
            translationRequestTemplate,
            apiLogLevel,
            devActions,
            suppressProgress,
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
                mostUsedHistory.getOrDefault(instruction, 0) + 1
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