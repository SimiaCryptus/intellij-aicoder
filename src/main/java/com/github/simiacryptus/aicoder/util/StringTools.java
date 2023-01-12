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
    public static CharSequence stripUnbalancedTerminators(CharSequence input) {
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
        String suffix = getWhitespaceSuffix(text);
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

    public static String lineWrapping(CharSequence description, int width) {
        StringBuilder output = new StringBuilder();
        String[] lines = description.toString().split("\n");
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

    private static String wrapSentence(CharSequence line, int width, AtomicInteger xPointer) {
        StringBuilder sentenceBuffer = new StringBuilder();
        String[] words = line.toString().split(" ");
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

    public static CharSequence toString(int[] ints) {
        char[] chars = new char[ints.length];
        for (int i = 0; i < ints.length; i++) {
            chars[i] = (char) ints[i];
        }
        return String.valueOf(chars);
    }

    @NotNull
    public static CharSequence getWhitespacePrefix(CharSequence... lines) {
        return Arrays.stream(lines)
                .map(l -> toString(l.chars().takeWhile(i -> Character.isWhitespace(i)).toArray()))
                .filter(x -> x.length()>0)
                .min(Comparator.comparing(x -> x.length())).orElse("");
    }

    @NotNull
    public static String getWhitespaceSuffix(CharSequence... lines) {
        return reverse(Arrays.stream(lines)
                .map(StringTools::reverse)
                .map(l -> toString(l.chars().takeWhile(i -> Character.isWhitespace(i)).toArray()))
                .max(Comparator.comparing(x -> x.length())).orElse("")).toString();
    }

    private static CharSequence reverse(CharSequence l) {
        return new StringBuffer(l).reverse().toString();
    }

    @NotNull
    public static List<CharSequence> trim(List<CharSequence> items, int max, boolean preserveHead) {
        items = new ArrayList<>(items);
        Random random = new Random();
        while (items.size() > max) {
            int index = random.nextInt(items.size());
            if (preserveHead && index == 0) continue;
            items.remove(index);
        }
        return items;
    }

    public static String transposeMarkdownTable(String table, boolean inputHeader, boolean outputHeader) {
        String[][] cells = parseMarkdownTable(table, inputHeader);
        StringBuilder transposedTable = new StringBuilder();
        int columns = cells[0].length;
        int rows = cells.length;
        if (outputHeader) columns = columns + 1;
        for (int column = 0; column < columns; column++) {
            transposedTable.append("|");
            for (int row = 0; row < rows; row++) {
                String cellValue;
                String[] rowCells = cells[row];
                if (outputHeader) {
                    if (column < 1) {
                        cellValue = rowCells[column].trim();
                    } else if (column == 1) {
                        cellValue = "---";
                    } else if ((column - 1) >= rowCells.length) {
                        cellValue = "";
                    } else {
                        cellValue = rowCells[column - 1].trim();
                    }
                } else {
                    cellValue = rowCells[column].trim();
                }
                transposedTable.append(" ").append(cellValue).append(" |");
            }
            transposedTable.append("\n");
        }
        return transposedTable.toString();
    }

    private static String[][] parseMarkdownTable(String table, boolean removeHeader) {
        ArrayList<CharSequence[]> rows = new ArrayList(Arrays.stream(table.split("\n")).map(x -> Arrays.stream(x.split("\\|")).filter(cell -> cell.length() > 0).toArray(CharSequence[]::new)).collect(Collectors.toList()));
        if (removeHeader) {
            rows.remove(1);
        }
        return rows.stream()
                //.filter(x -> x.length == rows.get(0).length)
                .toArray(String[][]::new);
    }

    public static CharSequence getPrefixForContext(String text) {
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
    public static CharSequence getPrefixForContext(String text, int idealLength, CharSequence... delimiters) {
        List<CharSequence> candidates = Stream.of(delimiters).flatMap(d -> {
            StringBuilder sb = new StringBuilder();
            String[] split = text.split(Pattern.quote(d.toString()));
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                if (Math.abs(sb.length() - idealLength) < Math.abs((sb.length() + s.length()) - idealLength)) break;
                if (sb.length() > 0) sb.append(d);
                sb.append(s);
                if (sb.length() > idealLength) break;
            }
            if (split.length == 0) return Stream.empty();
            return Stream.of(sb.toString());
        }).collect(Collectors.toList());
        Optional<CharSequence> winner = candidates.stream().min(Comparator.comparing(s -> Math.abs(s.length() - idealLength)));
        return winner.get();
    }

    public static CharSequence getSuffixForContext(String text) {
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
    public static CharSequence getSuffixForContext(String text, int idealLength, CharSequence... delimiters) {
        List<CharSequence> candidates = Stream.of(delimiters).flatMap(d -> {
            StringBuilder sb = new StringBuilder();
            String[] split = text.split(Pattern.quote(d.toString()));
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
        Optional<CharSequence> winner = candidates.stream().min(Comparator.comparing(s -> Math.abs(s.length() - idealLength)));
        return winner.get();
    }
}
