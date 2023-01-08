package com.github.simiacryptus.aicoder.openai.translate;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseTranslationRequest<T extends BaseTranslationRequest<T>> implements TranslationRequest {

    private String inputTag;
    private String outputTag;
    private String instruction;
    @NotNull
    private Map<String, String> inputAttr = new HashMap<>();
    @NotNull
    private Map<String, String> outputAttr = new HashMap<>();
    private String originalText;
    private double temperature;
    private int maxTokens;

    @Override
    public String getInputTag() {
        return inputTag;
    }

    @Override
    public String getOutputTag() {
        return outputTag;
    }

    @Override
    public String getInstruction() {
        return instruction;
    }

    @Override
    @NotNull
    public Map<String, String> getInputAttr() {
        return Collections.unmodifiableMap(inputAttr);
    }

    @Override
    @NotNull
    public Map<String, String> getOutputAttr() {
        return Collections.unmodifiableMap(outputAttr);
    }

    @Override
    public String getOriginalText() {
        return originalText;
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
    public T setInputType(String inputTag) {
        this.inputTag = inputTag;
        return (T) this;
    }

    @Override
    public T setOutputType(String outputTag) {
        this.outputTag = outputTag;
        return (T) this;
    }

    @Override
    public T setInstruction(String instruction) {
        this.instruction = instruction;
        return (T) this;
    }

    @Override
    public T setInputText(String originalText) {
        this.originalText = originalText;
        return (T) this;
    }

    @Override
    public T setTemperature(double temperature) {
        this.temperature = temperature;
        return (T) this;
    }

    @Override
    public T setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return (T) this;
    }

    @Override
    public T setInputAttribute(String key, String value) {
        if(null == value || value.isEmpty()) {
            inputAttr.remove(key);
        } else {
            inputAttr.put(key, value);
        }
        return (T) this;
    }

    @Override
    public T setOutputAttrute(String key, String value) {
        if(null == value || value.isEmpty()) {
            outputAttr.remove(key);
        } else {
            outputAttr.put(key, value);
        }
        return (T) this;
    }

}
