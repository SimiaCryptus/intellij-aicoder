package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ChatProxy
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import kotlin.toString

abstract class PasteActionBase(private val model: (AppSettingsState) -> ChatModels) : SelectionAction<String>(false) {
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

    protected open fun scrubHtml(str: String): String {
        val document: Document = Jsoup.parse(str)
        document.select("script, style").remove() // Remove script and style tags
        document.select("*").forEach { element ->
            val importantAttributes = listOf("href", "src", "alt", "title", "width", "height", "style", "class", "id")
            element.attributes().filter { it.key !in importantAttributes }.forEach { element.removeAttr(it.key) }
        } // Remove all non-important attributes
        document.select("*").forEach { element ->
            if (element.text().isNullOrEmpty()) {
                element.remove()
            }
        } // Remove elements with empty text
        val text = document.toString()
        return text // Return the plain text content
    }
}

class SmartPasteAction : PasteActionBase({ it.smartModel.chatModel() })
class FastPasteAction : PasteActionBase({ it.fastModel.chatModel() })