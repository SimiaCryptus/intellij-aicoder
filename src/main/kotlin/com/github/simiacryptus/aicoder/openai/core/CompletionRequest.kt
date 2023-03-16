package com.github.simiacryptus.aicoder.openai.core

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.ui.CompletionRequestWithModel
import com.github.simiacryptus.aicoder.openai.ui.InteractiveCompletionRequest
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.util.ui.FormBuilder
import java.util.*

open class CompletionRequest {
    constructor(config: AppSettingsState) : this("", config.temperature, config.maxTokens, null)

    fun uiIntercept(): CompletionRequestWithModel {
        val withModel: CompletionRequestWithModel
        withModel = if (this !is CompletionRequestWithModel) {
            val settingsState = AppSettingsState.instance
            if (!settingsState.devActions) {
                CompletionRequestWithModel(this, settingsState.model_completion)
            } else {
                showModelEditDialog()
            }
        } else {
            this
        }
        return withModel
    }

    var prompt: String = ""
    var suffix: String? = null

    @Suppress("unused")
    var temperature = 0.0

    @Suppress("unused")
    var max_tokens = 0
    var stop: Array<CharSequence>? = null

    @Suppress("unused")
    var logprobs: Int? = null

    @Suppress("unused")
    var echo = false

    @Suppress("unused")
    constructor()
    constructor(prompt: String, temperature: Double, max_tokens: Int, logprobs: Int?, vararg stop: CharSequence) {
        this.prompt = prompt
        this.temperature = temperature
        this.max_tokens = max_tokens
        this.stop = stop.map { it }.toTypedArray()
        this.logprobs = logprobs
        echo = false
    }

    constructor(other: CompletionRequest) {
        prompt = other.prompt
        temperature = other.temperature
        max_tokens = other.max_tokens
        stop = other.stop
        logprobs = other.logprobs
        echo = other.echo
    }

    fun appendPrompt(prompt: CharSequence): CompletionRequest {
        this.prompt = this.prompt + prompt
        return this
    }

    fun addStops(vararg newStops: CharSequence): CompletionRequest {
        val stops = ArrayList<CharSequence>()
        for (x in newStops) {
            if (x.length > 0) {
                stops.add(x)
            }
        }
        if (!stops.isEmpty()) {
            if (null != stop) Arrays.stream(stop).forEach { e: CharSequence ->
                stops.add(
                    e
                )
            }
            stop = stops.stream().distinct().toArray { size: Int -> arrayOfNulls<CharSequence>(size) }
        }
        return this
    }

    fun setSuffix(suffix: CharSequence?): CompletionRequest {
        this.suffix = suffix?.toString()
        return this
    }

    fun showModelEditDialog(): CompletionRequestWithModel {
        val formBuilder = FormBuilder.createFormBuilder()
        val instance = AppSettingsState.instance
        val withModel = CompletionRequestWithModel(this, instance.model_completion)
        val ui = InteractiveCompletionRequest(withModel)
        UITools.addKotlinFields<Any>(ui, formBuilder)
        UITools.writeKotlinUI(ui, withModel)
        val mainPanel = formBuilder.panel
        return if (UITools.showOptionDialog(mainPanel, arrayOf<Any>("OK"), title = "Completion Request") == 0) {
            UITools.readKotlinUI(ui, withModel)
            withModel
        } else {
            withModel
        }
    }
}