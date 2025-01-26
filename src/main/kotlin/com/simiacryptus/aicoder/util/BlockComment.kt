@file:Suppress("NAME_SHADOWING")

package com.simiacryptus.aicoder.util

import com.simiacryptus.aicoder.util.TextBlock.Companion.DELIMITER
import com.simiacryptus.aicoder.util.TextBlock.Companion.TAB_REPLACEMENT
import com.simiacryptus.util.StringUtil
import java.util.*
import java.util.stream.Collectors

class BlockComment(
  private val blockPrefix: CharSequence,
  private val linePrefix: CharSequence,
  private val blockSuffix: CharSequence,
  indent: CharSequence,
  vararg textBlock: CharSequence
) :
  IndentedText(indent, *textBlock) {
  class Factory(private val blockPrefix: String, private val linePrefix: String, private val blockSuffix: String) :
    TextBlockFactory<BlockComment?> {
    override fun fromString(text: String?): BlockComment {
      var text = text!!
      text = StringUtil.stripSuffix(
        StringUtil.trimSuffix(text.replace("\t", TAB_REPLACEMENT.toString(), false)),
        blockSuffix.trim { it <= ' ' })
      val indent = StringUtil.getWhitespacePrefix(*text.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray())
      return BlockComment(
        blockPrefix,
        linePrefix,
        blockSuffix,
        indent,
        *(Arrays.stream(text.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
          .map { s: CharSequence? -> StringUtil.stripPrefix(s!!, indent) }
          .map { text: CharSequence? -> StringUtil.trimPrefix(text!!) }
          .map { s: CharSequence? -> StringUtil.stripPrefix(s!!, blockPrefix.trim { it <= ' ' }) }
          .map { s: CharSequence? -> StringUtil.stripPrefix(s!!, linePrefix.trim { it <= ' ' }) }
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

  override fun withIndent(indent: CharSequence): IndentedText {
    return BlockComment(blockPrefix, linePrefix, blockSuffix, indent, *lines)
  }
}
