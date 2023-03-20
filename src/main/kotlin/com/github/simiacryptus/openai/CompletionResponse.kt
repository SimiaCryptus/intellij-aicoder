package com.github.simiacryptus.openai

import java.util.*

class CompletionResponse {
    @Suppress("unused")
    var id: String? = null

    @Suppress("unused")
    var `object`: String? = null

    @Suppress("unused")
    var created = 0

    @Suppress("unused")
    var model: String? = null
    var choices: Array<CompletionChoice> = arrayOf()

    @Suppress("unused")
    var error: ApiError? = null

    @Suppress("unused")
    var usage: Usage? = null

    constructor()
    constructor(
        id: String?,
        `object`: String?,
        created: Int,
        model: String?,
        choices: Array<CompletionChoice>,
        error: ApiError?
    ) {
        this.id = id
        this.`object` = `object`
        this.created = created
        this.model = model
        this.choices = choices
        this.error = error
    }

    val firstChoice: Optional<CharSequence>
        get() = Optional.ofNullable(choices).flatMap { choices: Array<CompletionChoice>? ->
            Arrays.stream(
                choices
            ).findFirst()
        }.map { choice: CompletionChoice -> choice.text!!.trim { it <= ' ' } }
}