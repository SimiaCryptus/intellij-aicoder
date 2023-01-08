package com.github.simiacryptus.aicoder;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringTools {

    public static String stripUnbalancedTerminators(String input) {
        int openCount = 0;
        boolean inQuotes = false;
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (inQuotes && c == '\\') {
                // Skip the next character
                i++;
            } else if (!inQuotes) {
                switch (c) {
                    case '{':
                    case '[':
                    case '(':
                        openCount++;
                        break;
                    case '}':
                    case ']':
                    case ')':
                        openCount--;
                        break;
                }
            }
            if (openCount >= 0) {
                output.append(c);
            } else {
                openCount++; // Dropping character
            }
        }
        if (openCount != 0) {
            throw new IllegalArgumentException("Unbalanced input");
        }
        return output.toString();
    }

    public static @NotNull String stripPrefix(@NotNull String text, @NotNull String prefix) {
        boolean startsWith = text.startsWith(prefix);
        if (startsWith) {
            return text.substring(prefix.length());
        } else {
            return text;
        }
    }

    public static @NotNull String stripSuffix(@NotNull String text, @NotNull String suffix) {
        boolean endsWith = text.endsWith(suffix);
        if (endsWith) {
            return text.substring(0, text.length()- suffix.length());
        } else {
            return text;
        }
    }

    public static String lineWrapping(String description) {
        return Arrays.stream(description.trim().split("\\.")).map(String::trim).collect(Collectors.joining("\n"));
    }
}
