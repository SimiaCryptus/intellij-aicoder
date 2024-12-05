package com.github.simiacryptus.aicoder.actions.code

// ... keep existing imports
import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor.*

/**
 * Base class for paste actions that convert clipboard content to appropriate code format
 * Supports both text and HTML clipboard content with automatic language detection
 */
abstract class PasteActionBase(private val model: (AppSettingsState) -> ChatModel) : SelectionAction<String>(false) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun processSelection(
        event: AnActionEvent?,
        selectionState: SelectionState,
        config: String?
    ) = when {
        event != null -> processSelection(selectionState, config)
        else -> ""
    }

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
        fun scrubHtml(str: String, maxLength: Int = 100 * 1024): String {
            val document: Document = Jsoup.parse(str)
            // Remove unnecessary elements, attributes, and optimize the document
            document.apply {
                fun qTry(block: () -> Unit) {
                    return try {
                        block()
                    } catch (e: Exception) {
                        log.error("Error in scrubHtml", e)
                    }
                }
                if ((document.body()?.html()?.length ?: 0) > maxLength) return document.body()?.html()?.substring(0, maxLength) ?: ""
                select("script, style, link, meta, iframe, noscript").remove() // Remove unnecessary and potentially harmful tags
                outputSettings().prettyPrint(false) // Disable pretty printing for compact output
                if ((document.body()?.html()?.length ?: 0) > maxLength) return document.body()?.html()?.substring(0, maxLength) ?: ""
                // Remove comments
                qTry { select("*").forEach { it.childNodes().removeAll { node -> node.nodeName() == "#comment" } } }
                if (document.body().html().length > maxLength) return@apply
                // Remove data-* attributes
                qTry { select("*[data-*]").forEach { it.attributes().removeAll { attr -> attr.key.startsWith("data-") } } }
                if (document.body().html().length > maxLength) return@apply
                qTry {
                    select("*").forEach { element ->
                        val importantAttributes = setOf("href", "src", "alt", "title", "width", "height", "style", "class", "id", "name")
                        element.attributes().removeAll { it.key !in importantAttributes }
                    }
                }
                if (document.body().html().length > maxLength) return@apply
                // Remove empty elements
                qTry {
                    select("*").forEach { element ->
                        if (element.childNodes().isEmpty() && element.attributes().isEmpty()) {
                            element.remove()
                        }
                    }
                }
                if (document.body().html().length > maxLength) return@apply
                // Unwrap single-child elements with no attributes
                qTry {
                    select("*").forEach { element ->
                        if (element.childNodes().size == 1 && element.childNodes()[0].nodeName() == "#text" && element.attributes().isEmpty()) {
                            element.unwrap()
                        }
                    }
                }
                if (document.body().html().length > maxLength) return@apply
                // Convert relative URLs to absolute
                qTry {
                    select("[href],[src]").forEach { element ->
                        element.attr("href").let { href -> element.attr("href", href.makeAbsolute()) }
                        element.attr("src").let { src -> element.attr("src", src.makeAbsolute()) }
                    }
                }
                if (document.body().html().length > maxLength) return@apply
                // Remove empty attributes
                qTry {
                    select("*").forEach { element ->
                        element.attributes().removeAll { it.value.isBlank() }
                    }
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

        fun getClipboard(): Any? {
            try {
                val toolkit = Toolkit.getDefaultToolkit()
                val systemClipboard = toolkit.systemClipboard
                return systemClipboard.getContents(null)?.let { contents ->
                    return when {
                        contents.isDataFlavorSupported(selectionHtmlFlavor) -> contents.getTransferData(selectionHtmlFlavor).toString().trim().let { scrubHtml(it) }
                        contents.isDataFlavorSupported(fragmentHtmlFlavor) -> contents.getTransferData(fragmentHtmlFlavor).toString().trim().let { scrubHtml(it) }
                        contents.isDataFlavorSupported(allHtmlFlavor) -> contents.getTransferData(allHtmlFlavor).toString().trim().let { scrubHtml(it) }
                        contents.isDataFlavorSupported(stringFlavor) -> contents.getTransferData(stringFlavor)
                        contents.isDataFlavorSupported(getTextPlainUnicodeFlavor()) -> contents.getTransferData(getTextPlainUnicodeFlavor())
                        else -> null
                    }
                }
            } catch (e: Exception) {
                log.error("Failed to access clipboard", e)
                return null
            }
        }

        fun hasClipboard() = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)?.let { contents ->
            return when {
                contents.isDataFlavorSupported(stringFlavor) -> true
                contents.isDataFlavorSupported(getTextPlainUnicodeFlavor()) -> true
                else -> false
            }
        } ?: false

        fun converter(chatClient: ChatClient, chatModel: ChatModel, temp: Double) = ChatProxy(VirtualAPI::class.java, chatClient, chatModel, temp,).create()
    }

    override fun getConfig(project: Project?): String {
        return ""
    }


    override fun processSelection(state: SelectionState, config: String?): String {
        val progress: ProgressIndicator? = state.progress
        progress?.text = "Reading clipboard content..."
        val clipboardContent = getClipboard() ?: return ""
        val text = clipboardContent.toString().trim()
        progress?.text = "Converting code format..."
        val converter = converter(api, model(AppSettingsState.instance), AppSettingsState.instance.temperature)
        return converter.convert(text, "autodetect", state.language?.name ?: state.editor?.virtualFile?.extension ?: "").code ?: ""
    }

    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        return true
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!hasClipboard()) return false
        return super.isEnabled(event)
    }

}

private fun String.makeAbsolute(): String {
    return if (startsWith("http://") || startsWith("https://") || startsWith("//")) {
        this
    } else {
        "https://$this"
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