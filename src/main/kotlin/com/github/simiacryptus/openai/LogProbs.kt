package com.github.simiacryptus.openai

import com.fasterxml.jackson.databind.node.ObjectNode

class LogProbs {
    @Suppress("unused")
    var tokens: Array<CharSequence> = arrayOf()

    @Suppress("unused")
    var token_logprobs: DoubleArray = DoubleArray(0)

    @Suppress("unused")
    var top_logprobs: Array<ObjectNode> = arrayOf()

    @Suppress("unused")
    var text_offset: IntArray = IntArray(0)

    @Suppress("unused")
    constructor()

    @Suppress("unused")
    constructor(
        tokens: Array<CharSequence>,
        token_logprobs: DoubleArray,
        top_logprobs: Array<ObjectNode>,
        text_offset: IntArray
    ) {
        this.tokens = tokens
        this.token_logprobs = token_logprobs
        this.top_logprobs = top_logprobs
        this.text_offset = text_offset
    }
}