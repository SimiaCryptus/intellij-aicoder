package com.github.simiacryptus.aicoder.text;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

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
 */
public class IndentedText implements TextBlock {

    public String getIndent() {
        return indent;
    }

    public static class Factory implements TextBlockFactory<IndentedText> {
        @Override
        public IndentedText fromString(String text) {
            return IndentedText.fromString(text);
        }

        @Override
        public boolean looksLike(String text) {
            return true;
        }
    }

    protected String indent;
    protected String textBlock[];

    public IndentedText(String indent, String... textBlock) {
        this.indent = indent;
        this.textBlock = textBlock;
    }

    public static @NotNull IndentedText fromString(String text) {
        text = text.replace("\t", TAB_REPLACEMENT);
        String indent = StringTools.getWhitespacePrefix(text.split(DELIMITER));
        return new IndentedText(indent,
                Arrays.stream(text.split(DELIMITER))
                .map(s -> StringTools.stripPrefix(s, indent))
                .toArray(String[]::new));
    }

    @Override
    public @NotNull String toString() {
        return Arrays.stream(rawString()).collect(Collectors.joining(DELIMITER + getIndent()));
    }

    public @NotNull IndentedText withIndent(String indent) {
        return new IndentedText(indent, textBlock);
    }

    @Override
    public String[] rawString() {
        return this.textBlock;
    }
}
