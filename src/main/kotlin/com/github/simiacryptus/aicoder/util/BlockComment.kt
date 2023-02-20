package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.util.TextBlock.Companion.DELIMITER
import com.github.simiacryptus.aicoder.util.TextBlock.Companion.TAB_REPLACEMENT
import java.util.*
import java.util.stream.Collectors

class BlockComment(
    val blockPrefix: CharSequence,
    val linePrefix: CharSequence,
    val blockSuffix: CharSequence,
    indent: CharSequence,
    vararg textBlock: CharSequence
) :
    IndentedText(indent, *textBlock) {
    class Factory(val blockPrefix: String, val linePrefix: String, val blockSuffix: String) :
        TextBlockFactory<BlockComment?> {
        override fun fromString(text: String?): BlockComment {
            var text = text!!
            text = StringTools.stripSuffix(
                StringTools.trimSuffix(text.replace("\t", TAB_REPLACEMENT.toString(), false)),
                blockSuffix.trim { it <= ' ' })
            val indent = StringTools.getWhitespacePrefix(*text.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
            return BlockComment(blockPrefix,
                linePrefix,
                blockSuffix,
                indent,
                *(Arrays.stream(text.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    .map { s: CharSequence? -> StringTools.stripPrefix(s!!, indent) }
                    .map { text: CharSequence? -> StringTools.trimPrefix(text!!) }
                    .map { s: CharSequence? -> StringTools.stripPrefix(s!!, blockPrefix.trim { it <= ' ' }) }
                    .map { s: CharSequence? -> StringTools.stripPrefix(s!!, linePrefix.trim { it <= ' ' }) }
                    .collect(Collectors.toList()).toTypedArray()))
        }

        override fun looksLike(text: String?): Boolean {
            return text!!.trim { it <= ' ' }.startsWith(blockPrefix) && text.trim { it <= ' ' }.endsWith(blockSuffix)
        }
    }

    override fun toString(): String {
        val indent = this.indent
        val delimiter: CharSequence = DELIMITER + indent
        val joined: CharSequence = Arrays.stream(rawString()).map { x: CharSequence -> "$linePrefix $x" }
            .collect(Collectors.joining(delimiter))
        return blockPrefix.toString() + delimiter + joined + delimiter + blockSuffix
    }

    override fun withIndent(indent: CharSequence?): BlockComment {
        return BlockComment(blockPrefix, linePrefix, blockSuffix, indent!!, *lines)
    }
}