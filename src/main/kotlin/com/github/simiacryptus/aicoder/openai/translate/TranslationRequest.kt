package com.github.simiacryptus.aicoder.openai.translate

import com.github.simiacryptus.aicoder.openai.CompletionRequest
import java.util.*
import kotlin.collections.HashMap


interface TranslationRequest {
    fun buildCompletionRequest(): CompletionRequest

    @get:Suppress("unused")
    val inputTag: CharSequence?

    @get:Suppress("unused")
    val outputTag: CharSequence?

    @get:Suppress("unused")
    val instruction: CharSequence?

    @get:Suppress("unused")
    val inputAttr: Map<CharSequence?, CharSequence?>

    @get:Suppress("unused")
    val outputAttr: Map<CharSequence?, CharSequence?>

    @get:Suppress("unused")
    val originalText: CharSequence?

    @get:Suppress("unused")
    val temperature: Double

    @get:Suppress("unused")
    val maxTokens: Int

    fun setInputType(inputTag: CharSequence?): TranslationRequest
    fun setOutputType(outputTag: CharSequence?): TranslationRequest
    fun setInstruction(instruction: CharSequence?): TranslationRequest
    fun setInputAttribute(key: CharSequence?, value: CharSequence?): TranslationRequest
    fun setOutputAttrute(key: CharSequence?, value: CharSequence?): TranslationRequest
    fun addExample(exampleText: CharSequence, attributes: Map<CharSequence, CharSequence> = HashMap()): TranslationRequest

    fun setInputText(originalText: CharSequence?): TranslationRequest

    @Suppress("unused")
    fun setTemperature(temperature: Double): TranslationRequest

    @Suppress("unused")
    fun setMaxTokens(maxTokens: Int): TranslationRequest
}


