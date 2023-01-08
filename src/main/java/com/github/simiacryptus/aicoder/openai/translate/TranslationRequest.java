package com.github.simiacryptus.aicoder.openai.translate;

import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface TranslationRequest {
    @NotNull CompletionRequest buildCompletionRequest();

    String getInputTag();

    String getOutputTag();

    String getInstruction();

    @NotNull Map<String, String> getInputAttr();

    @NotNull Map<String, String> getOutputAttr();

    String getOriginalText();

    double getTemperature();

    int getMaxTokens();

    TranslationRequest setInputType(String inputTag);

    TranslationRequest setOutputType(String outputTag);

    TranslationRequest setInstruction(String instruction);

    TranslationRequest setInputAttribute(String key, String value);
    TranslationRequest setOutputAttrute(String key, String value);

    TranslationRequest setInputText(String originalText);

    TranslationRequest setTemperature(double temperature);

    TranslationRequest setMaxTokens(int maxTokens);
}
