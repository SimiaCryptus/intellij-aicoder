package com.github.simiacryptus.openai

class ChatMessage {
    enum class Role {
        assistant, user, system
    }

    var role: Role? = null
    var content: String? = null

    @Suppress("unused")
    constructor()
    constructor(role: Role?, content: String?) {
        this.role = role
        this.content = content
    }
}