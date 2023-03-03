package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.TextRange
import java.util.*

class InsertAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val caret = event.getData(CommonDataKeys.CARET)
        val document = caret!!.editor.document
        val caretPosition = caret.offset
        val before = StringTools.getSuffixForContext(document.getText(TextRange(0, caretPosition)), 32)
        val after = StringTools.getPrefixForContext(document.getText(TextRange(caretPosition, document.textLength)), 32)
        val settings = AppSettingsState.getInstance()
        val completionRequest = settings.createCompletionRequest()
            .appendPrompt(before)
            .setSuffix(after)
        UITools.redoableRequest(
            completionRequest, "", event
        ) { newText: CharSequence? ->
            UITools.insertString(
                document, caretPosition,
                newText!!
            )
        }
    }

    companion object {
        @Suppress("unused")
        private fun isEnabled(e: AnActionEvent): Boolean {
            return !UITools.hasSelection(e)
        }
    }
}