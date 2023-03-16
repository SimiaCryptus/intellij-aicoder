package com.github.simiacryptus.aicoder.openai.translate

import com.github.simiacryptus.aicoder.config.AppSettingsState

abstract class BaseTranslationRequest<T : BaseTranslationRequest<T>>(settings: AppSettingsState) : TranslationRequest {
    init {
        setTemperature(settings.temperature)
        setMaxTokens(settings.maxTokens)
    }

    override var inputTag: CharSequence? = null
    override var outputTag: CharSequence? = null
    override var instruction: CharSequence? = null
    override val inputAttr: MutableMap<CharSequence?, CharSequence> = HashMap()
    override val outputAttr: MutableMap<CharSequence?, CharSequence> = HashMap()

    override var originalText: CharSequence? = null
    override var temperature = 0.0
    override var maxTokens = 0

    override fun setInputType(inputTag: CharSequence?): T {
        this.inputTag = inputTag
        return this as T
    }

    override fun setOutputType(outputTag: CharSequence?): T {
        this.outputTag = outputTag
        return this as T
    }

    override fun setInstruction(instruction: CharSequence?): T {
        this.instruction = instruction
        return this as T
    }

    override fun setInputText(originalText: CharSequence?): T {
        this.originalText = originalText
        return this as T
    }

    override fun setTemperature(temperature: Double): T {
        this.temperature = temperature
        return this as T
    }

    override fun setMaxTokens(maxTokens: Int): T {
        this.maxTokens = maxTokens
        return this as T
    }

    override fun setInputAttribute(key: CharSequence?, value: CharSequence?): T {
        if (null == value || value.length == 0) {
            inputAttr.remove(key)
        } else {
            inputAttr[key] = value
        }
        return this as T
    }

    override fun setOutputAttrute(key: CharSequence?, value: CharSequence?): T {
        if (null == value || value.length == 0) {
            outputAttr.remove(key)
        } else {
            outputAttr[key] = value
        }
        return this as T
    }
}