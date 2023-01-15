package com.github.simiacryptus.aicoder.util;

import org.jetbrains.annotations.NotNull;

public interface TextBlockFactory<T extends TextBlock> {
    T fromString(String text);
    @SuppressWarnings("unused")
    default CharSequence toString(@NotNull T text) {
        return text.toString();
    }
    boolean looksLike(String text);
}
