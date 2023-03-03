package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.showRadioButtonDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.TextRange
import java.util.*

class ReplaceOptionsAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val caret = event.getData(CommonDataKeys.CARET)
        val document = caret!!.editor.document
        val selectedText = caret.selectedText
        val idealLength = Math.pow(2.0, 2 + Math.ceil(Math.log(selectedText!!.length.toDouble()) / Math.log(2.0))).toInt()
        val newlines = "\n".toRegex()
        val selectionStart = caret.selectionStart
        val allBefore = document.getText(TextRange(0, selectionStart))
        val selectionEnd = caret.selectionEnd
        val allAfter = document.getText(TextRange(selectionEnd, document.textLength))
        val before = StringTools.getSuffixForContext(allBefore, idealLength).replace(newlines, " ")
        val after = StringTools.getPrefixForContext(allAfter, idealLength).replace(newlines, " ")
        val settings = AppSettingsState.getInstance()
        val completionRequest = settings.createCompletionRequest()
            .appendPrompt(
                """
                Give several options to fill in the blank:

                ${before}_______${after}

                1. $selectedText
                2.""".trimIndent()
            )
        UITools.redoableRequest(
            completionRequest, "", event,
            { newText: CharSequence? ->
                val options = newText!!.split("\n")
                    .map { it.trim().replace("^\\d+\\. ".toRegex(), "").trim() }.toTypedArray()
                showRadioButtonDialog("Select an option to fill in the blank:", *options)
            }, { newText: CharSequence? ->
                UITools.replaceString(document, selectionStart, selectionEnd, newText!!)
            })
    }

    @Suppress("unused")
    private fun isEnabled(e: AnActionEvent): Boolean {
        return UITools.hasSelection(e)
    }
}