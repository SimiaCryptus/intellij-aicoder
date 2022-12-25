package com.github.simiacryptus.aicoder.text;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * This class provides a way to store and manipulate indented text blocks.
 * <p>
 * The text block is stored as a single string, with each line separated by a newline character.
 * The indentation is stored as a separate string, which is prepended to each line when the text block is converted to a string.
 * <p>
 * The class provides a static method to convert a string to an IndentedText object.
 * This method replaces all tab characters with two spaces, and then finds the minimum indentation of all lines.
 * This indentation is then used as the indentation for the IndentedText object.
 * <p>
 * The class also provides a method to create a new IndentedText object with a different indentation.
 *
 */
public class IndentedText {
    public static final String TAB_REPLACEMENT = "  ";
    public static final String DELIMITER = "\n";

    public String indent;
    public String textBlock;

    public IndentedText(String indent, String textBlock) {
        this.indent = indent;
        this.textBlock = textBlock;
    }

    public static @NotNull IndentedText fromString(String text) {
        text = text.replace("\t", TAB_REPLACEMENT);
        long spaces = Arrays.stream(text.split(DELIMITER)).mapToLong(l -> l.chars().takeWhile(i -> i == ((int) ' ')).count()).filter(l -> l > 0).min().orElse(0L);
        char[] chars = new char[(int) spaces];
        Arrays.fill(chars, ' ');
        String indent = String.valueOf(chars);
        String stripped = Arrays.stream(text.split("\n"))
                .map(s -> s.startsWith(indent) ? s.substring(indent.length()) : s)
                .reduce((a, b) -> a + "\n" + b).get();
        return new IndentedText(indent, stripped);
    }

    @Override
    public @NotNull String toString() {
        return Arrays.stream(textBlock.split("\n"))
                .map(s -> indent + s)
                .reduce((a, b) -> a + "\n" + b).get();
    }

    public @NotNull IndentedText withIndent(String indent) {
        return new IndentedText(indent, textBlock);
    }
}
