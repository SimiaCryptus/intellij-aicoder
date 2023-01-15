package com.github.simiacryptus.aicoder.openai.translate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseTranslationRequest<T extends BaseTranslationRequest<T>> implements TranslationRequest {

    private CharSequence inputTag;
    private CharSequence outputTag;
    private CharSequence instruction;
    @NotNull
    private final Map<CharSequence, CharSequence> inputAttr = new HashMap<>();
    @NotNull
    private final Map<CharSequence, CharSequence> outputAttr = new HashMap<>();
    private CharSequence originalText;
    private double temperature;
    private int maxTokens;

    @Override
    public @NotNull String getInputTag() {
        return inputTag.toString();
    }

    @Override
    public @NotNull String getOutputTag() {
        return outputTag.toString();
    }

    @Override
    public @NotNull CharSequence getInstruction() {
        return instruction.toString();
    }

    @Override
    @NotNull
    public Map<CharSequence, CharSequence> getInputAttr() {
        return Collections.unmodifiableMap(inputAttr);
    }

    @Override
    @NotNull
    public Map<CharSequence, CharSequence> getOutputAttr() {
        return Collections.unmodifiableMap(outputAttr);
    }

    @Override
    public @NotNull String getOriginalText() {
        return originalText.toString();
    }

    @Override
    public double getTemperature() {
        return temperature;
    }

    @Override
    public int getMaxTokens() {
        return maxTokens;
    }

    @Override
    public @NotNull T setInputType(CharSequence inputTag) {
        this.inputTag = inputTag;
        return (T) this;
    }

    @Override
    public @NotNull T setOutputType(CharSequence outputTag) {
        this.outputTag = outputTag;
        return (T) this;
    }

    @Override
    public @NotNull T setInstruction(CharSequence instruction) {
        this.instruction = instruction;
        return (T) this;
    }

    @Override
    public @NotNull T setInputText(CharSequence originalText) {
        this.originalText = originalText;
        return (T) this;
    }

    @Override
    public @NotNull T setTemperature(double temperature) {
        this.temperature = temperature;
        return (T) this;
    }

    @Override
    public @NotNull T setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return (T) this;
    }

    @Override
    public @NotNull T setInputAttribute(CharSequence key, @Nullable CharSequence value) {
        if(null == value || value.length()==0) {
            inputAttr.remove(key);
        } else {
            inputAttr.put(key, value);
        }
        return (T) this;
    }

    @Override
    public @NotNull T setOutputAttrute(CharSequence key, @Nullable CharSequence value) {
        if(null == value || value.length()==0) {
            outputAttr.remove(key);
        } else {
            outputAttr.put(key, value);
        }
        return (T) this;
    }

}
