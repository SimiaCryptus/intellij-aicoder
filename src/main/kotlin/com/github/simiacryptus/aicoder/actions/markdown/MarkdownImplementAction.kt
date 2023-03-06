package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.hasSelection
import com.github.simiacryptus.aicoder.util.UITools.insertString
import com.github.simiacryptus.aicoder.util.UITools.redoableRequest
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * The MarkdownImplementAction is an IntelliJ action that allows users to quickly insert code snippets into markdown documents.
 * This action is triggered when a user selects a piece of text and then selects the action from the editor context menu.
 * The action will then generate a markdown code block and insert it into the document at the end of the selection.
 *
 * To use the MarkdownImplementAction, first select the text that you want to be included in the markdown code block.
 * Then, select the action in the context menu.
 * The action will generate a markdown code block and insert it into the document at the end of the selection.
 */
class MarkdownImplementAction(private val language: String) : AnAction(
    language
) {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val settings = AppSettingsState.getInstance()
        val caret = event.getData(CommonDataKeys.CARET)
        val selectedText = caret!!.selectedText
        val endOffset = caret.selectionEnd
        val completionRequest = settings.createCompletionRequest()
            .appendPrompt(String.format("%s\n```%s\n", selectedText, language))
            .addStops("```")
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        redoableRequest(completionRequest, "", event,
            { docString ->
                String.format(
                    "\n```%s\n%s\n```",
                    language, docString
                )
            },
            { docString ->
                insertString(
                    document, endOffset,
                    docString!!
                )
            }
        )
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if(UITools.isSanctioned()) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (ComputerLanguage.Markdown != computerLanguage) return false
            return hasSelection(e)
        }
    }
}