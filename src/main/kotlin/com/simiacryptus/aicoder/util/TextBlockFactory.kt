package com.simiacryptus.aicoder.util

interface TextBlockFactory<T : TextBlock?> {
  fun fromString(text: String?): T

  @Suppress("unused")
  fun toString(text: T): CharSequence? {
    return text.toString()
  }

  fun looksLike(text: String?): Boolean
}
