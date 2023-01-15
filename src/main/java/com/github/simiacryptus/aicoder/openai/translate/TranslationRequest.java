package com.github.simiacryptus.aicoder.openai.translate;

import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface TranslationRequest {
    @NotNull CompletionRequest buildCompletionRequest();

    @SuppressWarnings("unused")
    String getInputTag();

    @SuppressWarnings("unused")
    String getOutputTag();

    @SuppressWarnings("unused")
    CharSequence getInstruction();

    @SuppressWarnings("unused")
    @NotNull Map<CharSequence, CharSequence> getInputAttr();

    @SuppressWarnings("unused")
    @NotNull Map<CharSequence, CharSequence> getOutputAttr();

    @SuppressWarnings("unused")
    String getOriginalText();

    @SuppressWarnings("unused")
    double getTemperature();

    @SuppressWarnings("unused")
    int getMaxTokens();

    TranslationRequest setInputType(CharSequence inputTag);

    TranslationRequest setOutputType(CharSequence outputTag);

    TranslationRequest setInstruction(CharSequence instruction);

    TranslationRequest setInputAttribute(CharSequence key, CharSequence value);
    TranslationRequest setOutputAttrute(CharSequence key, CharSequence value);

    TranslationRequest setInputText(CharSequence originalText);

    @SuppressWarnings("unused")
    TranslationRequest setTemperature(double temperature);

    @SuppressWarnings("unused")
    TranslationRequest setMaxTokens(int maxTokens);
}
