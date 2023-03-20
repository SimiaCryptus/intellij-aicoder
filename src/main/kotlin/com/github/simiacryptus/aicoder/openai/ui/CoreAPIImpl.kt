package com.github.simiacryptus.aicoder.openai.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.openai.CoreAPI

class CoreAPIImpl(
    private val appSettingsState: AppSettingsState
) : CoreAPI(
    appSettingsState.apiBase,
    appSettingsState.apiKey,
    appSettingsState.apiLogLevel
) {

    override fun incrementTokens(totalTokens: Int) {
        appSettingsState.tokenCounter += totalTokens
    }
}