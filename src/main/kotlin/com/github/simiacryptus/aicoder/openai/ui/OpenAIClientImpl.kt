package com.github.simiacryptus.aicoder.openai.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.simiacryptus.openai.OpenAIClient
import org.apache.http.client.methods.HttpRequestBase

class OpenAIClientImpl(
    private val appSettingsState: AppSettingsState
) : OpenAIClient(
    key = appSettingsState.apiKey,
    apiBase = appSettingsState.apiBase,
    logLevel = appSettingsState.apiLogLevel
) {

    override fun incrementTokens(totalTokens: Int) {
        appSettingsState.tokenCounter += totalTokens
    }

    // Reenable on next JoePenai release

//    override fun authorize(request: HttpRequestBase) {
//        var apiKey: CharSequence = key
//        if (apiKey.isEmpty()) {
//            synchronized(OpenAIClient.javaClass) {
//                apiKey = key
//                if (apiKey.isEmpty()) {
//                    apiKey = UITools.queryAPIKey()!!
//                    key = apiKey.toString()
//                }
//            }
//        }
//        request.addHeader("Authorization", "Bearer $apiKey")
//    }
}