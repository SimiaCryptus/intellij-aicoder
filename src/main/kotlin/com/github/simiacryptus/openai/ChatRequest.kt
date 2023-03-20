package com.github.simiacryptus.openai

import com.github.simiacryptus.aicoder.config.AppSettingsState

class ChatRequest @Suppress("unused") constructor() {
    fun uiIntercept(): ChatRequest {
        return this
    }

    constructor(settingsState: AppSettingsState) : this() {
        model = (settingsState.model_chat)
        temperature = (settingsState.temperature)
    }

    constructor(request: ChatRequest) : this() {
        model = (request.model)
        temperature = (request.temperature)
        max_tokens = (request.max_tokens)
        stop = (request.stop)
        messages = (request.messages)
    }

    var messages = arrayOf<ChatMessage>()
    var model: String? = null
    var temperature = 0.0
    var max_tokens = 0
    var stop: Array<CharSequence>? = null
}