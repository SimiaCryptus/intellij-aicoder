package com.simiacryptus.aicoder.util

import java.util.stream.Stream
import kotlin.streams.asStream

interface TextBlock {
  companion object {
    val TAB_REPLACEMENT: CharSequence = "  "
    const val DELIMITER: String = "\n"
  }

  fun rawString(): Array<out CharSequence>

  val textBlock: CharSequence
    get() = rawString().joinToString(DELIMITER)

  fun withIndent(indent: CharSequence): TextBlock

  fun stream(): Stream<CharSequence> = rawString().asSequence().asStream()
}