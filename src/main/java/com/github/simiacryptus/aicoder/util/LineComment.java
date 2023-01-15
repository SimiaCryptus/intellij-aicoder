package com.github.simiacryptus.aicoder.util;

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
        public @NotNull LineComment fromString(String text) {
            text = text.replace("\t", TAB_REPLACEMENT);
            CharSequence indent = StringTools.getWhitespacePrefix(text.split(DELIMITER));
            return new LineComment(commentPrefix, indent, Arrays.stream(text.split(DELIMITER))
                    .map(s -> StringTools.stripPrefix(s, indent))
                    .map(StringTools::trimPrefix)
                    .map(s -> StringTools.stripPrefix(s, commentPrefix))
                    .toArray(CharSequence[]::new));
        }

        @Override
        public boolean looksLike(@NotNull String text) {
            return Arrays.stream(text.split(DELIMITER)).allMatch(x->x.trim().startsWith(commentPrefix));
        }
    }

    private final CharSequence commentPrefix;

    public LineComment(CharSequence commentPrefix, CharSequence indent, CharSequence... textBlock) {
        super(indent, textBlock);
        this.commentPrefix = commentPrefix;
    }

    @Override
    public @NotNull String toString() {
        return commentPrefix + " " + Arrays.stream(rawString()).collect(Collectors.joining(DELIMITER + getIndent() + commentPrefix + " "));
    }

    public @NotNull LineComment withIndent(CharSequence indent) {
        return new LineComment(commentPrefix, indent, textBlock);
    }
}
