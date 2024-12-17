package com.simiacryptus.aicoder.util

import com.simiacryptus.util.StringUtil

/**
 * This class provides a way to store and manipulate indented text blocks.
 *
 * The text block is stored as a single string, with each line separated by a newline character.
 * The indentation is stored as a separate string, which is prepended to each line when the text block is converted to a string.
 *
 * The class provides a companion object method to convert a string to an IndentedText object.
 * This method replaces all tab characters with two spaces, and then finds the minimum indentation of all lines.
 * This indentation is then used as the indentation for the IndentedText object.
 *
 * The class also provides a method to create a new IndentedText object with a different indentation.
 */
open class IndentedText(var indent: CharSequence, vararg val lines: CharSequence) : TextBlock {

    override fun toString(): String {
        return rawString().joinToString(TextBlock.DELIMITER + indent)
    }

    override fun withIndent(indent: CharSequence): IndentedText {
        return IndentedText(indent, *lines)
    }

    override fun rawString(): Array<out CharSequence> {
        return lines
    }

    companion object {
        /**
         * This method is used to convert a string into an IndentedText object.
         *
         * @param text The string to be converted into an IndentedText object.
         * @return IndentedText object created from the input string.
         */
        fun fromString(text: String?): IndentedText {
            val processedText = (text ?: "").replace("\t", TextBlock.TAB_REPLACEMENT.toString())
            val lines = processedText.split(TextBlock.DELIMITER)
            val indent = StringUtil.getWhitespacePrefix(*lines.toTypedArray())
            return IndentedText(indent, *lines.map { StringUtil.stripPrefix(it, indent) }.toTypedArray())
        }
    }
}