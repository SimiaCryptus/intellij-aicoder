package com.github.simiacryptus.aicoder.openai.ui

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.async.AsyncAPI
import com.github.simiacryptus.aicoder.openai.async.AsyncAPI.Companion.map
import com.github.simiacryptus.aicoder.openai.async.AsyncAPIImpl
import com.github.simiacryptus.aicoder.openai.core.CompletionRequest
import com.github.simiacryptus.aicoder.openai.core.CompletionResponse
import com.github.simiacryptus.aicoder.openai.core.CoreAPI
import com.github.simiacryptus.aicoder.openai.core.EditRequest
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.StringTools
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.swing.JComponent
import kotlin.collections.set

object OpenAI_API {

    @JvmStatic
    private val log = Logger.getInstance(OpenAI_API::class.java)

    @JvmStatic
    val coreAPI: CoreAPI = CoreAPIImpl(settingsState!!)

    @JvmStatic
    val asyncAPI = AsyncAPIImpl(coreAPI, settingsState!!)

    private val activeModelUI = WeakHashMap<ComboBox<CharSequence?>, Any>()

    @Transient
    var settings: AppSettingsState? = null

    @Transient
    private var comboBox: ComboBox<CharSequence?>? = null
    val modelSelector: JComponent
        get() {
            if (null != comboBox) {
                val element = ComboBox(
                    (IntStream.range(0, comboBox!!.itemCount)
                        .mapToObj(comboBox!!::getItemAt)).collect(Collectors.toList()).toTypedArray()
                )
                activeModelUI[element] = Any()
                return element
            }
            val apiKey: CharSequence = settingsState!!.apiKey
            if (apiKey.toString().trim { it <= ' ' }.length > 0) {
                try {
                    comboBox = ComboBox(
                        arrayOf<CharSequence?>(
                            settingsState!!.model_completion,
                            settingsState!!.model_edit
                        )
                    )
                    activeModelUI[comboBox] = Any()
                    AsyncAPI.onSuccess(
                        engines
                    ) { engines: ObjectNode ->
                        val data = engines["data"]
                        val items =
                            arrayOfNulls<CharSequence>(data.size())
                        for (i in 0 until data.size()) {
                            items[i] = data[i]["id"].asText()
                        }
                        Arrays.sort(items)
                        activeModelUI.keys.forEach(Consumer { ui: ComboBox<CharSequence?> ->
                            Arrays.stream(items).forEach(ui::addItem)
                        })
                    }
                    return comboBox!!
                } catch (e: Throwable) {
                    log.warn(e)
                }
            }
            return JBTextField()
        }

    fun getCompletion(
        project: Project?,
        request: CompletionRequest,
        indent: CharSequence
    ): ListenableFuture<CharSequence> {
        return getCompletion(project, request, filterStringResult(indent))
    }

    fun getCompletion(
        project: Project?,
        request: CompletionRequest,
        filter: (CharSequence) -> CharSequence
    ): ListenableFuture<CharSequence> {
        return map(complete(project, request)) { it.firstChoice.map(filter).orElse("") }
    }

    fun edit(project: Project?, request: EditRequest, indent: CharSequence): ListenableFuture<CharSequence?> {
        return edit(project, request, filterStringResult(indent))
    }

    fun edit(
        project: Project?,
        request: EditRequest,
        filter: (CharSequence) -> CharSequence
    ): ListenableFuture<CharSequence?> {
        return map(edit(project, request)) { it.firstChoice.map(filter).orElse("") }
    }

    val settingsState: AppSettingsState?
        get() {
            if (null == settings) {
                settings = AppSettingsState.instance
            }
            return settings
        }
    private val engines: ListenableFuture<ObjectNode>
        get() = AsyncAPI.pool.submit<ObjectNode> {
            coreAPI.mapper.readValue(
                coreAPI.get(settingsState!!.apiBase + "/engines"),
                ObjectNode::class.java
            )
        }

    fun complete(
        project: Project?,
        completionRequest: CompletionRequest
    ): ListenableFuture<CompletionResponse> {
        val settings = settingsState
        val withModel = completionRequest.uiIntercept()
        withModel.fixup(settings!!)
        val newRequest = CompletionRequest(withModel)
        return complete(project, newRequest, settings, withModel.model)
    }

    private fun edit(project: Project?, request: EditRequest): ListenableFuture<CompletionResponse> {
        return edit(project, request, settingsState!!)
    }

    private fun edit(
        project: Project?,
        editRequest: EditRequest,
        settings: AppSettingsState
    ): ListenableFuture<CompletionResponse> = asyncAPI.edit(project, editRequest, settings)

    fun complete(
        project: Project?,
        completionRequest: CompletionRequest,
        settings: AppSettingsState,
        model: String
    ): ListenableFuture<CompletionResponse> = asyncAPI.complete(project, completionRequest, model)


    fun filterStringResult(
        indent: CharSequence = "",
        stripUnbalancedTerminators: Boolean = true
    ): (CharSequence) -> CharSequence {
        return { text ->
            var result: CharSequence = text.toString().trim { it <= ' ' }
            if (stripUnbalancedTerminators) {
                result = StringTools.stripUnbalancedTerminators(result)
            }
            result = IndentedText.fromString2(result).withIndent(indent).toString()
            indent.toString() + result
        }
    }

    fun text_to_speech(wavAudio: ByteArray, prompt: String = ""): String = coreAPI.text_to_speech(wavAudio, prompt)

}
