package com.github.simiacryptus.aicoder.openai.translate;

import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface TranslationRequest {
    @NotNull CompletionRequest buildCompletionRequest();

    String getInputTag();

    String getOutputTag();

    CharSequence getInstruction();

    @NotNull Map<CharSequence, CharSequence> getInputAttr();

    @NotNull Map<CharSequence, CharSequence> getOutputAttr();

    String getOriginalText();

    double getTemperature();

    int getMaxTokens();

    TranslationRequest setInputType(CharSequence inputTag);

    TranslationRequest setOutputType(CharSequence outputTag);

    TranslationRequest setInstruction(CharSequence instruction);

    TranslationRequest setInputAttribute(CharSequence key, CharSequence value);
    TranslationRequest setOutputAttrute(CharSequence key, CharSequence value);

    TranslationRequest setInputText(CharSequence originalText);

    TranslationRequest setTemperature(double temperature);

    TranslationRequest setMaxTokens(int maxTokens);
}
