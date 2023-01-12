package com.github.simiacryptus.aicoder.openai.translate;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class TranslationRequest_XML extends BaseTranslationRequest<TranslationRequest_XML> {

    public TranslationRequest_XML(AppSettingsState settings) {
        setTemperature(settings.temperature);
        setMaxTokens(settings.maxTokens);
    }

    @Override
    @NotNull
    public CompletionRequest buildCompletionRequest() {
        CharSequence inputAttrStr = getInputAttr().isEmpty() ? "" : (" " + getInputAttr().entrySet().stream().map(t -> String.format("%s=\"%s\"", t.getKey(), t.getValue())).collect(Collectors.joining(" ")));
        CharSequence outputAttrStr = getOutputAttr().isEmpty() ? "" : (" " + getOutputAttr().entrySet().stream().map(t1 -> String.format("%s=\"%s\"", t1.getKey(), t1.getValue())).collect(Collectors.joining(" ")));
        String text = String.format("<!-- %s -->\n<%s%s>%s</%s>\n<%s%s>",
                getInstruction(),
                getInputTag().toLowerCase(),
                inputAttrStr,
                getOriginalText().trim(),
                getInputTag().toLowerCase(),
                getOutputTag().toLowerCase(),
                outputAttrStr
        ).trim();
        return new CompletionRequest(
                text,
                getTemperature(),
                getMaxTokens(),
                null,
                true,
                String.format("</%s>", getOutputTag().toLowerCase())
        );
    }

}
