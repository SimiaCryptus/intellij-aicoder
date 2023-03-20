package com.github.simiacryptus.aicoder

import org.apache.commons.io.FileUtils
import org.junit.Test

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

/**
 * See Also:
 *  https://github.com/JetBrains/intellij-ui-test-robot
 *  https://joel-costigliola.github.io/assertj/swing/api/org/assertj/swing/core/Robot.html
 */
class DocGen {

    @Test
    fun plugin_xml() {
        val base = File("C:\\Users\\andre\\code\\aicoder\\intellij-aicoder")
        if(!base.exists()) return
        val input =
            File(base,"src\\main\\resources\\META-INF\\plugin.xml")
        val output = File(base,"actions.md")
        val actions = loadActions(input)

        // Output a markdown table containing the action data
        val writer = PrintWriter(FileOutputStream(output))

        writer.println(
            """
            
            # Plaintext Actions
            
            Plaintext actions provide text processing features for any language. The following actions are available for plaintext in the AI Coder plugin:
            
            """.trimIndent()
        )
        printTable2(
            writer,
            actions.filter { it["id"].toString().startsWith("com.github.simiacryptus.aicoder.actions.generic") }
                .toTypedArray())

        writer.println(
            """
            
            # Code Actions
            
            The following actions are available for coding in the AI Coder plugin:
            
            """.trimIndent()
        )
        printTable2(
            writer,
            actions.filter { it["id"].toString().startsWith("com.github.simiacryptus.aicoder.actions.code") }
                .toTypedArray())

        writer.println(
            """
            
            # Markdown Actions
            
            Markdown Actions allow you to quickly and easily add list items, table columns, and more to your Markdown documents.
            
            """.trimIndent()
        )
        printTable2(
            writer,
            actions.filter { it["id"].toString().startsWith("com.github.simiacryptus.aicoder.actions.markdown") }
                .toTypedArray())

        writer.println(
            """
            
            # Developer-Mode Actions
            
            Some actions are only available when the plugin is running in developer mode. These may be useful for debugging or development, but also contain experimental features that may not be fully functional.
            
            """.trimIndent()
        )
        printTable2(
            writer,
            actions.filter { it["id"].toString().startsWith("com.github.simiacryptus.aicoder.actions.dev") }
                .toTypedArray())

        writer.close()
    }

    private fun printTable(
        writer: PrintWriter,
        actions: Array<Map<String, String>>
    ) {
        writer.println("| Text | Description | Shortcut | Full Description |")
        writer.println("| --- | --- | --- | --- |")
        actions.forEach { action ->
            val file = "src/main/kotlin/${action["id"].toString().replace(".", "/")}.kt"
            val uiText = action["text"].toString().replace("_", "")
            writer.println("| [$uiText]($file) | ${action["description"]} | ${action["shortcut"]} | ${action["long_description"]} |")
        }
    }

    private fun printTable2(
        writer: PrintWriter,
        actions: Array<Map<String, String>>
    ) {
        writer.println("| Text | Description |")
        writer.println("| --- | --- |")
        actions.forEach { action ->
            val uiText = action["text"].toString().replace("_", "")
            writer.println("| $uiText | ${action["long_description"]} |")
        }
    }

    private fun formatShortcut(shortcut: String): String {
        return shortcut.trim()
            .split(" ")
            .map { it.substringAfter("=").replace("\"", "").trim() }
            .map { "<kbd>${it.capitalize()}</kbd>" }
            .joinToString(" + ")
    }

    private fun loadActions(input: File): Array<Map<String, String>> {
        val pluginXml = FileUtils.readFileToString(input, "UTF-8");
        return (pluginXml.split("<action").map { it.substringBefore("</action>") }
                + pluginXml.split("<group").map { it.substringBefore("</group>") })
            .filter { it.contains("<!--DOC") }
            .map { action -> parseAction(action) }
            .sortedBy { it["id"] }
            .toTypedArray()
    }

    private fun parseAction(action: String) = mapOf(
        "id" to action.substringAfter("class=\"").substringBefore("\""),
        "text" to action.substringAfter("text=\"").substringBefore("\""),
        "description" to action.substringAfter("description=\"").substringBefore("\""),
        "long_description" to action.substringAfter("<!--DOC").substringBefore("-->").split("\n").map { it.trim() }
            .joinToString(" "),
        "shortcut" to if (action.contains("keyboard-shortcut")) {
            formatShortcut(action.substringAfter("keymap=\"\$default\"").substringBefore("/>"))
        } else {
            ""
        }
    )

}