package com.github.simiacryptus.aicoder.text;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

public class StringTools {

    /*
     *
     * Strips unbalanced terminators from a given input string.
     *
     * @param input The input string to strip unbalanced terminators from.
     * @return The input string with unbalanced terminators removed.
     * @throws IllegalArgumentException If the input string is unbalanced.
     */
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

    public static @NotNull String trimPrefix(@NotNull String text) {
        String prefix = getWhitespacePrefix(text);
        return stripPrefix(text, prefix);
    }

    public static @NotNull String trimSuffix(@NotNull String text) {
        String suffix = getWhitespaceSuffix(text);
        return stripSuffix(text, suffix);
    }

    public static @NotNull String stripSuffix(@NotNull String text, @NotNull String suffix) {
        boolean endsWith = text.endsWith(suffix);
        if (endsWith) {
            return text.substring(0, text.length() - suffix.length());
        } else {
            return text;
        }
    }

    public static String lineWrapping(String description, int width) {
        StringBuilder output = new StringBuilder();
        String[] lines = description.split("\n");
        int lineLength = 0;
        for (String line : lines) {
            AtomicInteger sentenceLength = new AtomicInteger(lineLength);
            String sentanceBuffer = wrapSentence(line, width, sentenceLength);
            if (lineLength + sentanceBuffer.length() > width && sentanceBuffer.length() < width) {
                output.append("\n");
                lineLength = 0;
                sentenceLength.set(lineLength);
                sentanceBuffer = wrapSentence(line, width, sentenceLength);
            } else {
                output.append(" ");
                sentenceLength.addAndGet(1);
            }
            output.append(sentanceBuffer);
            lineLength = sentenceLength.get();
        }
        return output.toString();
    }

    private static String wrapSentence(String line, int width, AtomicInteger xPointer) {
        StringBuilder sentenceBuffer = new StringBuilder();
        String[] words = line.split(" ");
        for (String word : words) {
            if (xPointer.get() + word.length() > width) {
                sentenceBuffer.append("\n");
                xPointer.set(0);
            } else {
                sentenceBuffer.append(" ");
                xPointer.addAndGet(1);
            }
            sentenceBuffer.append(word);
            xPointer.addAndGet(word.length());
        }
        return sentenceBuffer.toString();
    }

    public static String toString(int[] ints) {
        char[] chars = new char[ints.length];
        for (int i = 0; i < ints.length; i++) {
            chars[i] = (char) ints[i];
        }
        return String.valueOf(chars);
    }

    @NotNull
    public static String getWhitespacePrefix(String... lines) {
        return Arrays.stream(lines)
                .map(l -> toString(l.chars().takeWhile(i -> Character.isWhitespace(i)).toArray()))
                .filter(x->!x.isEmpty())
                .min(Comparator.comparing(x -> x.length())).orElse("");
    }

    @NotNull
    public static String getWhitespaceSuffix(String... lines) {
        return reverse(Arrays.stream(lines)
                .map(StringTools::reverse)
                .map(l -> toString(l.chars().takeWhile(i -> Character.isWhitespace(i)).toArray()))
                .max(Comparator.comparing(x -> x.length())).orElse("")).toString();
    }

    private static CharSequence reverse(String l) {
        return new StringBuffer(l).reverse().toString();
    }
}
