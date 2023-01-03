package com.github.simiacryptus.aicoder.openai.translate;

import com.github.simiacryptus.aicoder.config.AppSettingsState;

import java.util.function.Function;
import java.util.function.Supplier;

public enum TranslationRequestTemplate {
    XML(config -> new TranslationRequest_XML(config));
    private final Function<AppSettingsState, TranslationRequest> fn;

    TranslationRequestTemplate(Function<AppSettingsState, TranslationRequest> fn) {
        this.fn = fn;
    }

    public TranslationRequest get(AppSettingsState config) {
        return fn.apply(config);
    }
}
