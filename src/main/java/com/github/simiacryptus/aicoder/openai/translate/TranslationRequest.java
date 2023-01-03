package com.github.simiacryptus.aicoder.openai.translate;

import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface TranslationRequest {
    @NotNull CompletionRequest buildRequest();

    String getInputTag();

    String getOutputTag();

    String getInstruction();

    @NotNull Map<String, String> getInputAttr();

    @NotNull Map<String, String> getOutputAttr();

    String getOriginalText();

    double getTemperature();

    int getMaxTokens();

    TranslationRequest setInputTag(String inputTag);

    TranslationRequest setOutputTag(String outputTag);

    TranslationRequest setInstruction(String instruction);

    TranslationRequest setInputAttr(String key, String value);
    TranslationRequest setOutputAttr(String key, String value);

    TranslationRequest setOriginalText(String originalText);

    TranslationRequest setTemperature(double temperature);

    TranslationRequest setMaxTokens(int maxTokens);
}
