package com.github.simiacryptus.aicoder.text;

public interface TextBlockFactory<T extends TextBlock> {
    T fromString(String text);
    default String toString(T text) {
        return text.toString();
    }
    boolean looksLike(String text);
}
