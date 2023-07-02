﻿package com.github.simiacryptus.aicoder.util;

import com.simiacryptus.util.StringUtil;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This class provides a way to store and manipulate indented text blocks.
 *
 * The text block is stored as a single string, with each line separated by a newline character.
 * The indentation is stored as a separate string, which is prepended to each line when the text block is converted to a string.
 *
 * The class provides a static method to convert a string to an IndentedText object.
 * This method replaces all tab characters with two spaces, and then finds the minimum indentation of all lines.
 * This indentation is then used as the indentation for the IndentedText object.
 *
 * The class also provides a method to create a new IndentedText object with a different indentation.
 */
public class IndentedText implements TextBlock {
    private CharSequence indent;
    private CharSequence[] lines;

    public IndentedText(CharSequence indent, CharSequence... lines) {
        this.setIndent(indent);
        this.setLines(lines);
    }

    @Override
    public String toString() {
        return Arrays.stream(rawString()).collect(Collectors.joining(TextBlock.DELIMITER + getIndent()));
    }

    @Override
    public IndentedText withIndent(CharSequence indent) {
        return new IndentedText(indent, getLines());
    }

    @Override
    public CharSequence[] rawString() {
        return this.getLines();
    }

    public static IndentedText fromString(String text) {
        text = text == null ? "" : text;
        text = text.replace("\t", TextBlock.TAB_REPLACEMENT.toString());
        CharSequence indent = StringUtil.getWhitespacePrefix(text.split(TextBlock.DELIMITER));
        return new IndentedText(indent, Arrays.stream(text.split(TextBlock.DELIMITER))
                .map(s -> StringUtil.stripPrefix(s, indent))
                .toArray(CharSequence[]::new));
    }

    public static IndentedText fromString2(CharSequence text) {
        text = text == null ? "" : text;
        text = text.toString().replace("\t", TextBlock.TAB_REPLACEMENT.toString());
        CharSequence indent = StringUtil.getWhitespacePrefix2(text.toString().split(TextBlock.DELIMITER));
        return new IndentedText(indent, Arrays.stream(text.toString().split(TextBlock.DELIMITER))
                .map(s -> StringUtil.stripPrefix(s, indent))
                .toArray(CharSequence[]::new));
    }

    public static IndentedText fromString3(CharSequence text) {
        text = text == null ? "" : text;
        text = text.toString().replace("\t", TextBlock.TAB_REPLACEMENT.toString());
        CharSequence indent = StringUtil.getWhitespacePrefix2(text.toString().split(TextBlock.DELIMITER));
        return new IndentedText(indent, Arrays.stream(text.toString().split(TextBlock.DELIMITER))
                .map(s -> StringUtil.stripPrefix(s, indent))
                .toArray(CharSequence[]::new));
    }

    public CharSequence getIndent() {
        return indent;
    }

    public void setIndent(CharSequence indent) {
        this.indent = indent;
    }

    public CharSequence[] getLines() {
        return lines;
    }

    public void setLines(CharSequence[] lines) {
        this.lines = lines;
    }
}