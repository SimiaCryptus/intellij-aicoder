package com.github.simiacryptus.aicoder.openai.core

class ChatRequest @Suppress("unused") constructor() {
    var messages = arrayOf<ChatMessage>()
    var model: String? = null
    var temperature = 0.0
    var max_tokens = 0
    var stop: Array<CharSequence>? = null
}