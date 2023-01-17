package com.github.simiacryptus.aicoder.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringTools {

    /*
     *
     * Strips unbalanced terminators from a given input string.
     *
     * @param input The input string to strip unbalanced terminators from.
     * @return The input string with unbalanced terminators removed.
     * @throws IllegalArgumentException If the input string is unbalanced.
     */
    public static @NotNull CharSequence stripUnbalancedTerminators(@NotNull CharSequence input) {
        int openCount = 0;
        boolean inQuotes = false;
        @NotNull StringBuilder output = new StringBuilder();
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

    public static @NotNull String stripPrefix(@NotNull CharSequence text, @NotNull CharSequence prefix) {
        boolean startsWith = text.toString().startsWith(prefix.toString());
        if (startsWith) {
            return text.toString().substring(prefix.length());
        } else {
            return text.toString();
        }
    }

    public static @NotNull CharSequence trimPrefix(@NotNull CharSequence text) {
        @NotNull CharSequence prefix = getWhitespacePrefix(text);
        return stripPrefix(text, prefix);
    }

    public static @NotNull String trimSuffix(@NotNull CharSequence text) {
        @NotNull String suffix = getWhitespaceSuffix(text);
        return stripSuffix(text, suffix);
    }

    public static @NotNull String stripSuffix(@NotNull CharSequence text, @NotNull CharSequence suffix) {
        boolean endsWith = text.toString().endsWith(suffix.toString());
        if (endsWith) {
            return text.toString().substring(0, text.length() - suffix.length());
        } else {
            return text.toString();
        }
    }

    public static @NotNull String lineWrapping(@NotNull CharSequence description, int width) {
        @NotNull StringBuilder output = new StringBuilder();
        String @NotNull [] lines = description.toString().split("\n");
        int lineLength = 0;
        for (@NotNull String line : lines) {
            @NotNull AtomicInteger sentenceLength = new AtomicInteger(lineLength);
            @NotNull String sentenceBuffer = wrapSentence(line, width, sentenceLength);
            if (lineLength + sentenceBuffer.length() > width && sentenceBuffer.length() < width) {
                output.append("\n");
                lineLength = 0;
                sentenceLength.set(lineLength);
                sentenceBuffer = wrapSentence(line, width, sentenceLength);
            } else {
                output.append(" ");
                sentenceLength.addAndGet(1);
            }
            output.append(sentenceBuffer);
            lineLength = sentenceLength.get();
        }
        return output.toString();
    }

    private static @NotNull String wrapSentence(@NotNull CharSequence line, int width, @NotNull AtomicInteger xPointer) {
        @NotNull StringBuilder sentenceBuffer = new StringBuilder();
        String @NotNull [] words = line.toString().split(" ");
        for (@NotNull String word : words) {
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

    public static @NotNull CharSequence toString(int @NotNull [] ints) {
        char @NotNull [] chars = new char[ints.length];
        for (int i = 0; i < ints.length; i++) {
            chars[i] = (char) ints[i];
        }
        return String.valueOf(chars);
    }

    @NotNull
    public static CharSequence getWhitespacePrefix(CharSequence @NotNull ... lines) {
        return Arrays.stream(lines)
                .map(l -> toString(l.chars().takeWhile(Character::isWhitespace).toArray()))
                .filter(x -> x.length()>0)
                .min(Comparator.comparing(CharSequence::length)).orElse("");
    }

    @NotNull
    public static String getWhitespaceSuffix(CharSequence @NotNull ... lines) {
        return reverse(Arrays.stream(lines)
                .map(StringTools::reverse)
                .map(l -> toString(l.chars().takeWhile(Character::isWhitespace).toArray()))
                .max(Comparator.comparing(CharSequence::length)).orElse("")).toString();
    }

    private static @NotNull CharSequence reverse(@NotNull CharSequence l) {
        return new StringBuffer(l).reverse().toString();
    }

    @NotNull
    public static List<CharSequence> trim(List<CharSequence> items, int max, boolean preserveHead) {
        items = new ArrayList<>(items);
        @NotNull Random random = new Random();
        while (items.size() > max) {
            int index = random.nextInt(items.size());
            if (preserveHead && index == 0) continue;
            items.remove(index);
        }
        return items;
    }

    public static @NotNull String transposeMarkdownTable(@NotNull String table, boolean inputHeader, boolean outputHeader) {
        CharSequence[] @NotNull [] cells = parseMarkdownTable(table, inputHeader);
        @NotNull StringBuilder transposedTable = new StringBuilder();
        int columns = cells[0].length;
        if (outputHeader) columns = columns + 1;
        for (int column = 0; column < columns; column++) {
            transposedTable.append("|");
            for (CharSequence[] cell : cells) {
                String cellValue;
                if (outputHeader) {
                    if (column < 1) {
                        cellValue = cell[column].toString().trim();
                    } else if (column == 1) {
                        cellValue = "---";
                    } else if ((column - 1) >= cell.length) {
                        cellValue = "";
                    } else {
                        cellValue = cell[column - 1].toString().trim();
                    }
                } else {
                    cellValue = cell[column].toString().trim();
                }
                transposedTable.append(" ").append(cellValue).append(" |");
            }
            transposedTable.append("\n");
        }
        return transposedTable.toString();
    }

    private static CharSequence[] @NotNull [] parseMarkdownTable(@NotNull String table, boolean removeHeader) {
        @NotNull ArrayList<CharSequence[]> rows = Arrays.stream(table.split("\n")).map(x -> Arrays.stream(x.split("\\|")).filter(cell -> cell.length() > 0).toArray(CharSequence[]::new)).collect(Collectors.toCollection(ArrayList::new));
        if (removeHeader) {
            rows.remove(1);
        }
        return rows.toArray(CharSequence[][]::new);
    }

    public static @NotNull CharSequence getPrefixForContext(@NotNull String text) {
        return getPrefixForContext(text, 512, ".", "\n", ",", ";");
    }

    /**
     * Get the prefix for the given context.
     *
     * @param text        The text to get the prefix from.
     * @param idealLength The ideal length of the prefix.
     * @param delimiters  The delimiters to split the text by.
     * @return The prefix for the given context.
     */
    public static @NotNull CharSequence getPrefixForContext(@NotNull String text, int idealLength, CharSequence... delimiters) {
        @NotNull List<CharSequence> candidates = Stream.of(delimiters).flatMap(d -> {
            @NotNull StringBuilder sb = new StringBuilder();
            String @NotNull [] split = text.split(Pattern.quote(d.toString()));
            for (String s : split) {
                if (Math.abs(sb.length() - idealLength) < Math.abs((sb.length() + s.length()) - idealLength)) break;
                if (sb.length() > 0) sb.append(d);
                sb.append(s);
                if (sb.length() > idealLength) break;
            }
            if (split.length == 0) return Stream.empty();
            return Stream.of(sb.toString());
        }).collect(Collectors.toList());
        return candidates.stream().min(Comparator.comparing(s -> Math.abs(s.length() - idealLength))).orElse("");
    }

    public static @NotNull CharSequence getSuffixForContext(@NotNull String text) {
        return getSuffixForContext(text, 512, ".", "\n", ",", ";");
    }

    /**
     *
     *   Get the suffix for the given context.
     *
     *   @param text The text to get the suffix from.
     *   @param idealLength The ideal length of the suffix.
     *   @param delimiters The delimiters to use when splitting the text.
     *   @return The suffix for the given context.
     */
    @NotNull
    public static CharSequence getSuffixForContext(@NotNull String text, int idealLength, CharSequence... delimiters) {
        @NotNull List<CharSequence> candidates = Stream.of(delimiters).flatMap(d -> {
            @NotNull StringBuilder sb = new StringBuilder();
            String @NotNull [] split = text.split(Pattern.quote(d.toString()));
            for (int i = split.length - 1; i >= 0; i--) {
                String s = split[i];
                if (Math.abs(sb.length() - idealLength) < Math.abs((sb.length() + s.length()) - idealLength)) break;
                if (sb.length() > 0 || text.endsWith(d.toString())) sb.insert(0, d);
                sb.insert(0, s);
                if (sb.length() > idealLength) {
                    //if (i > 0) sb.insert(0, d);
                    break;
                }
            }
            if (split.length == 0) return Stream.empty();
            return Stream.of(sb.toString());
        }).collect(Collectors.toList());
        return candidates.stream().min(Comparator.comparing(s -> Math.abs(s.length() - idealLength))).orElse("");
    }
}
