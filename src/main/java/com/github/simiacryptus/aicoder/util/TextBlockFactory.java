package com.github.simiacryptus.aicoder.util;

public interface TextBlockFactory<T extends TextBlock> {
    T fromString(String text);
    default CharSequence toString(T text) {
        return text.toString();
    }
    boolean looksLike(String text);
}
