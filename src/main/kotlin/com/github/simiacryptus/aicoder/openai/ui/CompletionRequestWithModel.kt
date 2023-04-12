package com.github.simiacryptus.aicoder.openai.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.openai.CompletionRequest

class CompletionRequestWithModel(other: CompletionRequest, var model: String) : CompletionRequest(other) {

    fun fixup(settings: AppSettingsState) {
        if (null != suffix) {
            if (suffix!!.trim { it <= ' ' }.isEmpty()) {
                setSuffix(null)
            } else {
                echo = false
            }
        }
        if (null != stop && stop!!.isEmpty()) {
            stop = null
        }
        require(prompt.length <= settings.maxPrompt) { "Prompt too long:" + prompt.length + " chars" }
    }
}