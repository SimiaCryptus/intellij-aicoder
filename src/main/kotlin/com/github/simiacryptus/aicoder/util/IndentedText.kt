package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.util.StringTools.getWhitespacePrefix
import com.github.simiacryptus.util.StringTools.getWhitespacePrefix2
import com.github.simiacryptus.util.StringTools.stripPrefix
import java.util.*
import java.util.stream.Collectors

/**
 * This class provides a way to store and manipulate indented text blocks.
 *
 *
 * The text block is stored as a single string, with each line separated by a newline character.
 * The indentation is stored as a separate string, which is prepended to each line when the text block is converted to a string.
 *
 *
 * The class provides a static method to convert a string to an IndentedText object.
 * This method replaces all tab characters with two spaces, and then finds the minimum indentation of all lines.
 * This indentation is then used as the indentation for the IndentedText object.
 *
 *
 * The class also provides a method to create a new IndentedText object with a different indentation.
 */
open class IndentedText(
    var indent: CharSequence,
    vararg val lines: CharSequence
) :
    TextBlock {
    @Suppress("unused")
    class Factory : TextBlockFactory<IndentedText?> {
        override fun fromString(text: String?): IndentedText {
            return Companion.fromString(text)
        }

        @Suppress("unused")
        override fun looksLike(text: String?): Boolean {
            return true
        }
    }

    override fun toString(): String {
        return Arrays.stream(rawString()).collect(Collectors.joining(TextBlock.DELIMITER + indent))
    }

    override fun withIndent(indent: CharSequence?): IndentedText {
        return IndentedText(indent!!, *lines)
    }

    override fun rawString(): Array<CharSequence> {
        return this.lines as Array<CharSequence>
    }

    companion object {
        fun fromString(text: CharSequence?): IndentedText {
            var text = text
            text = text.toString().replace(Regex("\t"), TextBlock.TAB_REPLACEMENT.toString())
            val indent =
                getWhitespacePrefix(*text.toString().split(TextBlock.DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
            return IndentedText(indent,
                *Arrays.stream(text.toString().split(TextBlock.DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
                    .map { s: String? ->
                        stripPrefix(
                            s!!, indent
                        )
                    }
                    .collect(Collectors.toList()).toTypedArray())
        }

        fun fromString2(text: CharSequence): IndentedText {
            var text = text
            text = text.toString().replace(Regex("\t"), TextBlock.TAB_REPLACEMENT.toString())
            val indent = getWhitespacePrefix2(
                *text.toString().split(TextBlock.DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
            return IndentedText(indent,
                *Arrays.stream(text.toString().split(TextBlock.DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
                    .map { s: String? ->
                        stripPrefix(
                            s!!, indent
                        )
                    }
                    .collect(Collectors.toList()).toTypedArray())
        }
    }
}