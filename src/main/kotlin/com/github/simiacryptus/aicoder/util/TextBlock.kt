package com.github.simiacryptus.aicoder.util

import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

interface TextBlock {
    fun rawString(): Array<CharSequence>?
    val textBlock: CharSequence?
        get() = Arrays.stream(rawString()).collect(Collectors.joining(DELIMITER))

    fun withIndent(indent: CharSequence?): TextBlock
    fun stream(): Stream<CharSequence> {
        return Arrays.stream(rawString())
    }

    companion object {
        val TAB_REPLACEMENT: CharSequence = "  "
        const val DELIMITER = "\n"
    }
}