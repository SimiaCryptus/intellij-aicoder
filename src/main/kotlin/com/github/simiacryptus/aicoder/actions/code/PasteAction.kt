package com.github.simiacryptus.aicoder.actions.code

// ... keep existing imports
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

/**
 * Base class for paste actions that convert clipboard content to appropriate code format
 * Supports both text and HTML clipboard content with automatic language detection
 */
abstract class PasteActionBase(private val model: (AppSettingsState) -> ChatModel) : SelectionAction<String>(false) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    protected fun processSelection(event: AnActionEvent, config: String?): String {
        val state = SelectionState(
            language = event.getData(CommonDataKeys.VIRTUAL_FILE)?.extension?.let { ComputerLanguage.findByExtension(it) },
            selectedText = getSelectedText(event.getData(CommonDataKeys.EDITOR)),
            progress = ProgressManager.getInstance().progressIndicator
        )
        return processSelection(state, config)
    }

    private fun getSelectedText(editor: Editor?): String? {
        if (editor == null) return null
        val caret = editor.caretModel.primaryCaret
        return if (caret.hasSelection()) {
            editor.document.getText(TextRange(caret.selectionStart, caret.selectionEnd))
        } else null
    }

    data class SelectionState(
        val language: ComputerLanguage?,
        val selectedText: String?,
        val progress: ProgressIndicator?
    )

    /**
     * API interface for code conversion
     */
    interface VirtualAPI {
        fun convert(text: String, from_language: String, to_language: String): ConvertedText

        /**
         * Response class containing converted code
         */

        class ConvertedText {
            var code: String? = null
            var language: String? = null
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PasteActionBase::class.java)
    }

    /**
     * Smart paste action using more capable but slower model
     */

    override fun getConfig(project: Project?): String {
        return ""
    }


    protected fun processSelection(state: SelectionState, config: String?): String {
        val progress: ProgressIndicator? = state.progress
        progress?.text = "Reading clipboard content..."
        val text = getClipboard().toString().trim()
        progress?.text = "Converting code format..."
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
        try {
        val toolkit = Toolkit.getDefaultToolkit()
        val systemClipboard = toolkit.systemClipboard
        return systemClipboard.getContents(null)?.let { contents ->
            return when {
                contents.isDataFlavorSupported(DataFlavor.selectionHtmlFlavor) -> contents.getTransferData(DataFlavor.selectionHtmlFlavor).toString().trim()
                    ?.let { scrubHtml(it) }

                contents.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor) -> contents.getTransferData(DataFlavor.fragmentHtmlFlavor).toString().trim()
                    ?.let { scrubHtml(it) }

                contents.isDataFlavorSupported(DataFlavor.allHtmlFlavor) -> contents.getTransferData(DataFlavor.allHtmlFlavor).toString().trim()
                    ?.let { scrubHtml(it) }
                contents.isDataFlavorSupported(DataFlavor.stringFlavor) -> contents.getTransferData(DataFlavor.stringFlavor)
                contents.isDataFlavorSupported(DataFlavor.getTextPlainUnicodeFlavor()) -> contents.getTransferData(
                    DataFlavor.getTextPlainUnicodeFlavor()
                )

                else -> null
            }
        }
        } catch (e: Exception) {
            log.error("Failed to access clipboard", e)
            return null
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

/**
 * Fast paste action using faster but simpler model
 */
class FastPasteAction : PasteActionBase({ it.fastModel.chatModel() }) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(FastPasteAction::class.java)
    }

    protected var progress: ProgressIndicator? = null
}