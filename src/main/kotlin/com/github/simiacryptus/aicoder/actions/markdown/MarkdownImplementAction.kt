package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.hasSelection
import com.github.simiacryptus.aicoder.util.UITools.insertString
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.openai.APIClientBase
import com.simiacryptus.openai.proxy.ChatProxy

/**
 * The MarkdownImplementAction is an IntelliJ action that allows users to quickly insert code snippets into markdown documents.
 * This action is triggered when a user selects a piece of text and then selects the action from the editor context menu.
 * The action will then generate a markdown code block and insert it into the document at the end of the selection.
 *
 * To use the MarkdownImplementAction, first select the text that you want to be included in the markdown code block.
 * Then, select the action in the context menu.
 * The action will generate a markdown code block and insert it into the document at the end of the selection.
 */
class MarkdownImplementAction(private val language: String) : BaseAction(
    language
) {
    interface VirtualAPI {
        fun implement(text: String, humanLanguage: String, computerLanguage: String): ConvertedText
        data class ConvertedText(
            val code: String? = null,
            val language: String? = null,
        )
    }

    val proxy: VirtualAPI
        get() = ChatProxy(
            clazz = VirtualAPI::class.java,
            api = api,
            model = AppSettingsState.instance.defaultChatModel(),
            deserializerRetries = 5,
        ).create()


    override fun handle(event: AnActionEvent) {
        val caret = event.getData(CommonDataKeys.CARET)
        val selectedText = caret!!.selectedText ?: return
        val endOffset = caret.selectionEnd
        val document = (event.getData(CommonDataKeys.EDITOR) ?: return).document

        UITools.redoableTask(event) {
            val code = UITools.run(
                event.project, "Implement Instructions", true
            ) {
                proxy.implement(selectedText, "autodetect", language).code ?: ""
            }
            val docString = """
                |
                |```$language
                |$code
                |```
                |
                |""".trimMargin()
            UITools.writeableFn(event) {
                insertString(document, endOffset, docString)
            }
        }
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (APIClientBase.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        if (ComputerLanguage.Markdown != computerLanguage) return false
        return hasSelection(event)
    }
}
