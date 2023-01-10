package com.github.simiacryptus.aicoder.text;

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
            String indent = StringTools.getWhitespacePrefix(text.split(DELIMITER));
            return new BlockComment(blockPrefix, linePrefix, blockSuffix, indent, Arrays.stream(text.split(DELIMITER))
                    .map(s -> StringTools.stripPrefix(s, indent))
                    .map(StringTools::trimPrefix)
                    .map(s -> StringTools.stripPrefix(s, blockPrefix.trim()))
                    .map(s -> StringTools.stripPrefix(s, linePrefix.trim()))
                    .toArray(String[]::new));
        }

        @Override
        public boolean looksLike(String text) {
            return text.trim().startsWith(blockPrefix) && text.trim().endsWith(blockSuffix);
        }
    }

    public final String linePrefix;
    public final String blockPrefix;
    public final String blockSuffix;


    public BlockComment(String blockPrefix, String linePrefix, String blockSuffix, String indent, String... textBlock) {
        super(indent, textBlock);
        this.linePrefix = linePrefix;
        this.blockPrefix = blockPrefix;
        this.blockSuffix = blockSuffix;
    }

    @Override
    public @NotNull String toString() {
        String indent = getIndent();
        String delimiter = DELIMITER + indent;
        String joined = Arrays.stream(rawString()).map(x->linePrefix + " " + x).collect(Collectors.joining(delimiter));
        return blockPrefix + delimiter + joined + delimiter + blockSuffix;
    }

    public @NotNull IndentedText withIndent(String indent) {
        return new BlockComment(blockPrefix, linePrefix, blockSuffix, indent, textBlock);
    }
}
