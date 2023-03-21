package com.github.simiacryptus.aicoder.openai.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.openai.OpenAIClient

class OpenAIClientImpl(
    private val appSettingsState: AppSettingsState
) : OpenAIClient(
    appSettingsState.apiBase,
    appSettingsState.apiKey,
    appSettingsState.apiLogLevel
) {

    override fun incrementTokens(totalTokens: Int) {
        appSettingsState.tokenCounter += totalTokens
    }
}