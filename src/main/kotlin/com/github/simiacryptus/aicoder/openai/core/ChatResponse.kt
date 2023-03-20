package com.github.simiacryptus.aicoder.openai.core

import java.util.*

class ChatResponse {
    @Suppress("unused")
    var id: String? = null

    @Suppress("unused")
    var `object`: String? = null

    @Suppress("unused")
    var created: Long = 0

    @Suppress("unused")
    var model: String? = null
    var choices: Array<ChatChoice> = arrayOf()

    @Suppress("unused")
    var error: ApiError? = null

    @Suppress("unused")
    var usage: Usage? = null
    val response: Optional<CharSequence>
        get() = Optional.ofNullable(choices).flatMap(
            { choices: Array<ChatChoice>? ->
                Arrays.stream(
                    choices
                ).findFirst()
            }).map(
            { choice: ChatChoice -> choice.message!!.content!!.trim { it <= ' ' } })
}