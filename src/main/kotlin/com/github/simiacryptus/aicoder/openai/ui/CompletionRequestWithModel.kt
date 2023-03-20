package com.github.simiacryptus.aicoder.openai.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.core.CompletionRequest

class CompletionRequestWithModel : CompletionRequest {
    var model: String

    constructor(other: CompletionRequest, model: String) : super(other) {
        this.model = model
    }

    fun fixup(settings: AppSettingsState) {
        if (null != suffix) {
            if (suffix!!.trim { it <= ' ' }.length == 0) {
                setSuffix(null)
            } else {
                echo = false
            }
        }
        if (null != stop && stop!!.size == 0) {
            stop = null
        }
        require(prompt.length <= settings.maxPrompt) { "Prompt too long:" + prompt.length + " chars" }
    }
}