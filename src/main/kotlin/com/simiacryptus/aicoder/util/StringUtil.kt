package com.simiacryptus.aicoder.util

object StringUtil {
  fun lineWrapping(text: String, maxLineLength: Int): String {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = StringBuilder()

    for (word in words) {
      if (currentLine.length + word.length + 1 <= maxLineLength) {
        if (currentLine.isNotEmpty()) currentLine.append(" ")
        currentLine.append(word)
      } else {
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        currentLine = StringBuilder(word)
      }
    }
    if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
    return lines.joinToString("\n")
  }
}