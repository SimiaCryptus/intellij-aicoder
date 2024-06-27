package com.github.simiacryptus.aicoder.util

import java.io.File
import java.nio.file.Path

object CodeProcessingUtils {

    fun extractCode(code: String): String {
        return code.trim().let {
            "(?s)```[^\\n]*\n(.*)\n```".toRegex().find(it)?.groupValues?.get(1) ?: it
        }
    }

    fun codeSummary(codeFiles: Set<Path>, root: File): String {
        return codeFiles.filter {
            val name = it.fileName.toString().lowercase()
            !(name.endsWith(".png") || name.endsWith(".jpg"))
        }.joinToString("\n\n") { path ->
            "# $path\n```${path.toString().split('.').last()}\n${root.resolve(path.toFile()).readText()}\n```"
        }
    }

    fun formatCodeWithLineNumbers(code: String): String {
        return code.split("\n").withIndex().joinToString("\n") { (i, line) ->
            "${i.toString().padStart(3, '0')} $line"
        }
    }

    fun getSuffixForContext(text: String, idealLength: Int): String {
        return text.takeLast(idealLength).replace('\n', ' ')
    }

    fun getPrefixForContext(text: String, idealLength: Int): String {
        return text.take(idealLength).replace('\n', ' ')
    }
}