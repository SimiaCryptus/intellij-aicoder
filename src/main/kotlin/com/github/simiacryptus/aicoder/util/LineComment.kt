package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.util.StringTools.getWhitespacePrefix
import com.github.simiacryptus.aicoder.util.StringTools.stripPrefix
import com.github.simiacryptus.aicoder.util.StringTools.trimPrefix
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

class LineComment(private val commentPrefix: CharSequence, indent: CharSequence?, vararg lines: CharSequence) :
    IndentedText(indent!!, *lines) {
    class Factory(val commentPrefix: String) : TextBlockFactory<LineComment?> {
        override fun fromString(text: String?): LineComment {
            var text = text
            text = text!!.replace(Regex("\t"), TextBlock.TAB_REPLACEMENT.toString())
            val indent = getWhitespacePrefix(*text.split(TextBlock.DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
            return LineComment(
                commentPrefix,
                indent,
                *Arrays.stream(text.split(TextBlock.DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
                    .map { s: String? ->
                        stripPrefix(
                            s!!, indent
                        )
                    }
                    .map(Function<CharSequence, CharSequence> { obj: CharSequence -> trimPrefix(obj) })
                    .map { s: CharSequence? ->
                        stripPrefix(
                            s!!,
                            commentPrefix
                        )
                    }
                    .collect(Collectors.toList()).toTypedArray())
        }

        override fun looksLike(text: String?): Boolean {
            return Arrays.stream(text!!.split(TextBlock.DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()).allMatch { x: String ->
                x.trim { it <= ' ' }.startsWith(
                    commentPrefix
                )
            }
        }
    }

    override fun toString(): String {
        return commentPrefix.toString() + " " + Arrays.stream(rawString())
            .collect(Collectors.joining(TextBlock.DELIMITER + indent + commentPrefix + " "))
    }

    override fun withIndent(indent: CharSequence?): LineComment {
        return LineComment(commentPrefix, indent, *lines)
    }
}