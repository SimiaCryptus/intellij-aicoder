package com.github.simiacryptus.aicoder.text;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public class LineComment extends IndentedText {
    public static class Factory implements TextBlockFactory<LineComment> {
        public final String commentPrefix;

        public Factory(String commentPrefix) {
            this.commentPrefix = commentPrefix;
        }

        @Override
        public LineComment fromString(String text) {
            text = text.replace("\t", TAB_REPLACEMENT);
            String indent = StringTools.getWhitespacePrefix(text.split(DELIMITER));
            return new LineComment(commentPrefix, indent, Arrays.stream(text.split(DELIMITER))
                    .map(s -> StringTools.stripPrefix(s, indent))
                    .map(StringTools::trimPrefix)
                    .map(s -> StringTools.stripPrefix(s, commentPrefix))
                    .toArray(String[]::new));
        }

        @Override
        public boolean looksLike(String text) {
            return Arrays.stream(text.split(DELIMITER)).allMatch(x->x.trim().startsWith(commentPrefix));
        }
    }

    private final String commentPrefix;

    public LineComment(String commentPrefix, String indent, String... textBlock) {
        super(indent, textBlock);
        this.commentPrefix = commentPrefix;
    }

    @Override
    public @NotNull String toString() {
        return commentPrefix + " " + Arrays.stream(rawString()).collect(Collectors.joining(DELIMITER + getIndent() + commentPrefix + " "));
    }

    public @NotNull IndentedText withIndent(String indent) {
        return new LineComment(commentPrefix, indent, textBlock);
    }
}
