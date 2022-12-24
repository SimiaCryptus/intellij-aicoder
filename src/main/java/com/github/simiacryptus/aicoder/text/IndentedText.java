package com.github.simiacryptus.aicoder.text;

import java.util.Arrays;

public class IndentedText {
    public static final String TAB_REPLACEMENT = "  ";
    public static final String DELIMITER = "\n";

    public String indent;
    public String textBlock;

    public IndentedText(String indent, String textBlock) {
        this.indent = indent;
        this.textBlock = textBlock;
    }

    @Override
    public String toString() {
        return Arrays.stream(textBlock.split("\n"))
                .map(s -> indent + s)
                .reduce((a, b) -> a + "\n" + b).get();
    }

    public static IndentedText fromString(String text) {
        text = text.replace("\t", TAB_REPLACEMENT);
        long spaces = Arrays.stream(text.split(DELIMITER)).mapToLong(l -> l.chars().takeWhile(i -> i == ((int) ' ')).count()).filter(l -> l > 0).min().orElse(0l);
        char[] chars = new char[(int) spaces];
        Arrays.fill(chars, ' ');
        String indent = String.valueOf(chars);
        String stripped = Arrays.stream(text.split("\n"))
                .map(s -> s.startsWith(indent) ? s.substring(indent.length()) : s)
                .reduce((a, b) -> a + "\n" + b).get();
        return new IndentedText(indent, stripped);
    }

    public IndentedText withIndent(String indent) {
        return new IndentedText(indent, textBlock);
    }
}
