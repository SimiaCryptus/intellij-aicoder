package com.github.simiacryptus.aicoder.openai.translate;

import com.github.simiacryptus.aicoder.config.AppSettingsState;

import java.util.function.Function;

public enum TranslationRequestTemplate {
    XML(TranslationRequest_XML::new);
    private final Function<AppSettingsState, TranslationRequest> fn;

    TranslationRequestTemplate(Function<AppSettingsState, TranslationRequest> fn) {
        this.fn = fn;
    }

    public TranslationRequest get(AppSettingsState config) {
        return fn.apply(config);
    }
}
