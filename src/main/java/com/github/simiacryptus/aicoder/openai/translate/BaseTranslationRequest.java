package com.github.simiacryptus.aicoder.openai.translate;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseTranslationRequest<T extends BaseTranslationRequest<T>> implements TranslationRequest {

    private CharSequence inputTag;
    private CharSequence outputTag;
    private CharSequence instruction;
    @NotNull
    private Map<CharSequence, CharSequence> inputAttr = new HashMap<>();
    @NotNull
    private Map<CharSequence, CharSequence> outputAttr = new HashMap<>();
    private CharSequence originalText;
    private double temperature;
    private int maxTokens;

    @Override
    public String getInputTag() {
        return inputTag.toString();
    }

    @Override
    public String getOutputTag() {
        return outputTag.toString();
    }

    @Override
    public CharSequence getInstruction() {
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
    public String getOriginalText() {
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
    public T setInputType(CharSequence inputTag) {
        this.inputTag = inputTag;
        return (T) this;
    }

    @Override
    public T setOutputType(CharSequence outputTag) {
        this.outputTag = outputTag;
        return (T) this;
    }

    @Override
    public T setInstruction(CharSequence instruction) {
        this.instruction = instruction;
        return (T) this;
    }

    @Override
    public T setInputText(CharSequence originalText) {
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
    public T setInputAttribute(CharSequence key, CharSequence value) {
        if(null == value || value.length()==0) {
            inputAttr.remove(key);
        } else {
            inputAttr.put(key, value);
        }
        return (T) this;
    }

    @Override
    public T setOutputAttrute(CharSequence key, CharSequence value) {
        if(null == value || value.length()==0) {
            outputAttr.remove(key);
        } else {
            outputAttr.put(key, value);
        }
        return (T) this;
    }

}
