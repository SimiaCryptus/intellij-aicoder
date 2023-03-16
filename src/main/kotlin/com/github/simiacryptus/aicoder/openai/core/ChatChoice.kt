package com.github.simiacryptus.aicoder.openai.core

class ChatChoice @Suppress("unused") constructor() {
    var message: ChatMessage? = null

    @Suppress("unused")
    var index = 0

    @Suppress("unused")
    var finish_reason: String? = null
}