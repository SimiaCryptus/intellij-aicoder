package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import kotlin.toString

abstract class PasteActionBase(private val model: (AppSettingsState) -> ChatModel) : SelectionAction<String>(false) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    interface VirtualAPI {
        fun convert(text: String, from_language: String, to_language: String): ConvertedText

        class ConvertedText {
            var code: String? = null
            var language: String? = null
        }
    }

    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun processSelection(state: SelectionState, config: String?): String {
        val text = getClipboard().toString().trim()
        return ChatProxy(
            VirtualAPI::class.java,
            api,
            model(AppSettingsState.instance),
            AppSettingsState.instance.temperature,
        ).create().convert(
            text,
            "autodetect",
            state.language?.name ?: ""
        ).code ?: ""
    }

    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        if (computerLanguage == null) return false
        return computerLanguage != ComputerLanguage.Text
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!hasClipboard()) return false
        return super.isEnabled(event)
    }

    private fun hasClipboard() = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)?.let { contents ->
        return when {
            contents.isDataFlavorSupported(DataFlavor.stringFlavor) -> true
            contents.isDataFlavorSupported(DataFlavor.getTextPlainUnicodeFlavor()) -> true
            else -> false
        }
    } ?: false

    private fun getClipboard(): Any? {
        val toolkit = Toolkit.getDefaultToolkit()
        val systemClipboard = toolkit.systemClipboard
        return systemClipboard.getContents(null)?.let { contents ->
            return when {
                contents.isDataFlavorSupported(DataFlavor.selectionHtmlFlavor) -> contents.getTransferData(DataFlavor.selectionHtmlFlavor).let { scrubHtml(it.toString().trim()) }
                contents.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor) -> contents.getTransferData(DataFlavor.fragmentHtmlFlavor).let { scrubHtml(it.toString().trim()) }
                contents.isDataFlavorSupported(DataFlavor.allHtmlFlavor) -> contents.getTransferData(DataFlavor.allHtmlFlavor).let { scrubHtml(it.toString().trim()) }
                contents.isDataFlavorSupported(DataFlavor.stringFlavor) -> contents.getTransferData(DataFlavor.stringFlavor)
                contents.isDataFlavorSupported(DataFlavor.getTextPlainUnicodeFlavor()) -> contents.getTransferData(
                    DataFlavor.getTextPlainUnicodeFlavor()
                )

                else -> null
            }
        }
    }

    protected open fun scrubHtml(str: String, maxLength: Int = 100 * 1024): String {
        val document: Document = Jsoup.parse(str)
        // Remove unnecessary elements, attributes, and optimize the document
        document.apply {
            if (document.body().html().length > maxLength) return@apply
            select("script, style, link, meta, iframe, noscript").remove() // Remove unnecessary and potentially harmful tags
            outputSettings().prettyPrint(false) // Disable pretty printing for compact output
            if (document.body().html().length > maxLength) return@apply
            // Remove comments
            select("*").forEach { it.childNodes().removeAll { node -> node.nodeName() == "#comment" } }
            if (document.body().html().length > maxLength) return@apply
            // Remove data-* attributes
            select("*[data-*]").forEach { it.attributes().removeAll { attr -> attr.key.startsWith("data-") } }
            if (document.body().html().length > maxLength) return@apply
            select("*").forEach { element ->
                val importantAttributes = setOf("href", "src", "alt", "title", "width", "height", "style", "class", "id", "name")
                element.attributes().removeAll { it.key !in importantAttributes }
            }
            if (document.body().html().length > maxLength) return@apply
            // Remove empty elements
            select("*").filter { it.text().isBlank() && it.attributes().isEmpty() && !it.hasAttr("img") }.forEach { remove() }
            if (document.body().html().length > maxLength) return@apply
            // Unwrap single-child elements with no attributes
            select("*").forEach { element ->
                if (element.childNodes().size == 1 && element.childNodes()[0].nodeName() == "#text" && element.attributes().isEmpty()) {
                    element.unwrap()
                }
            }
            if (document.body().html().length > maxLength) return@apply
            // Convert relative URLs to absolute
            select("[href],[src]").forEach { element ->
                element.attr("href")?.let { href -> element.attr("href", href.makeAbsolute()) }
                element.attr("src")?.let { src -> element.attr("src", src.makeAbsolute()) }
            }
            if (document.body().html().length > maxLength) return@apply
            // Remove empty attributes
            select("*").forEach { element ->
                element.attributes().removeAll { it.value.isBlank() }
            }
        }

        // Truncate if necessary
        val result = document.body().html()
        return if (result.length > maxLength) {
            result.substring(0, maxLength)
        } else {
            result
        }
    }

    private fun String.makeAbsolute(): String {
        return if (startsWith("http://") || startsWith("https://") || startsWith("//")) {
            this
        } else {
            "https://$this"
        }
    }
}

class SmartPasteAction : PasteActionBase({ it.smartModel.chatModel() })
class FastPasteAction : PasteActionBase({ it.fastModel.chatModel() })