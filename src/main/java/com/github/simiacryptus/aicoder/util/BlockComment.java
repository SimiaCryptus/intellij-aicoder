package com.github.simiacryptus.aicoder.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public class BlockComment extends IndentedText {
    public static class Factory implements TextBlockFactory<BlockComment> {
        public final String linePrefix;
        public final String blockPrefix;
        public final String blockSuffix;

        public Factory(String blockPrefix, String linePrefix, String blockSuffix) {
            this.linePrefix = linePrefix;
            this.blockPrefix = blockPrefix;
            this.blockSuffix = blockSuffix;
        }

        @Override
        public BlockComment fromString(String text) {
            text = StringTools.stripSuffix(StringTools.trimSuffix(text.replace("\t", TAB_REPLACEMENT)), blockSuffix.trim());
            @NotNull CharSequence indent = StringTools.getWhitespacePrefix(text.split(DELIMITER));
            return new BlockComment(blockPrefix, linePrefix, blockSuffix, indent, Arrays.stream(text.split(DELIMITER))
                    .map(s -> StringTools.stripPrefix(s, indent))
                    .map(StringTools::trimPrefix)
                    .map(s -> StringTools.stripPrefix(s, blockPrefix.trim()))
                    .map(s -> StringTools.stripPrefix(s, linePrefix.trim()))
                    .toArray(CharSequence[]::new));
        }

        @Override
        public boolean looksLike(String text) {
            return text.trim().startsWith(blockPrefix) && text.trim().endsWith(blockSuffix);
        }
    }

    public final CharSequence linePrefix;
    public final CharSequence blockPrefix;
    public final CharSequence blockSuffix;


    public BlockComment(CharSequence blockPrefix, CharSequence linePrefix, CharSequence blockSuffix, CharSequence indent, CharSequence... textBlock) {
        super(indent, textBlock);
        this.linePrefix = linePrefix;
        this.blockPrefix = blockPrefix;
        this.blockSuffix = blockSuffix;
    }

    @Override
    public @NotNull String toString() {
        CharSequence indent = getIndent();
        CharSequence delimiter = DELIMITER + indent;
        CharSequence joined = Arrays.stream(rawString()).map(x->linePrefix + " " + x).collect(Collectors.joining(delimiter));
        return blockPrefix.toString() + delimiter + joined + delimiter + blockSuffix;
    }

    public @NotNull BlockComment withIndent(CharSequence indent) {
        return new BlockComment(blockPrefix, linePrefix, blockSuffix, indent, textBlock);
    }
}
