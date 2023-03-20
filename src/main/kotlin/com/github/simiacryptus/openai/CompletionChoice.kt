package com.github.simiacryptus.openai

class CompletionChoice {
    var text: String? = null

    @Suppress("unused")
    var index = 0

    @Suppress("unused")
    var logprobs: LogProbs? = null

    @Suppress("unused")
    var finish_reason: String? = null

    @Suppress("unused")
    constructor()

    @Suppress("unused")
    constructor(text: String?, index: Int, logprobs: LogProbs?, finish_reason: String?) {
        this.text = text
        this.index = index
        this.logprobs = logprobs
        this.finish_reason = finish_reason
    }
}