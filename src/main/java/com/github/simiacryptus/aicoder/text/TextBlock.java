package com.github.simiacryptus.aicoder.text;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface TextBlock {

    public static final String TAB_REPLACEMENT = "  ";
    public static final String DELIMITER = "\n";

    String[] rawString();

    default String getTextBlock() {
        return Arrays.stream(rawString()).collect(Collectors.joining(DELIMITER));
    }

    @NotNull TextBlock withIndent(String indent);

    default Stream<String> stream() {
        return Arrays.stream(rawString());
    }
}
