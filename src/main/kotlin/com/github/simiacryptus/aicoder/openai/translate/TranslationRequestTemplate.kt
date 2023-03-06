package com.github.simiacryptus.aicoder.openai.translate

import com.github.simiacryptus.aicoder.config.AppSettingsState

enum class TranslationRequestTemplate(private val fn: java.util.function.Function<AppSettingsState, TranslationRequest>) {
    XML({ settings: AppSettingsState? ->
        TranslationRequest_XML(
            settings!!
        )
    });

    operator fun get(config: AppSettingsState): TranslationRequest {
        return fn.apply(config)
    }
}