package com.github.simiacryptus.aicoder.util;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface TextBlock {
    CharSequence[] rawString();
    default CharSequence getTextBlock() {
        return Arrays.stream(rawString()).collect(Collectors.joining(DELIMITER));
    }

    TextBlock withIndent(CharSequence indent);
    default Stream<CharSequence> stream() {
        return Arrays.stream(rawString());
    }

    CharSequence TAB_REPLACEMENT = "  ";
    String DELIMITER = "\n";
}
