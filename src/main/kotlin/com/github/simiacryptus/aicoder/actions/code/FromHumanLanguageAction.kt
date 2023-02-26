package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*

/**
 * The FromHumanLanguageAction class is an action that is used to convert a human language specification into a computer language.
 * It is triggered when the user selects a text in the editor and selects the action.
 * The action will then replace the selected text with the GPT-translated version.
 */
class FromHumanLanguageAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectionStart = primaryCaret.selectionStart
        val selectionEnd = primaryCaret.selectionEnd
        val selectedText = primaryCaret.selectedText
        val request = AppSettingsState.getInstance().createTranslationRequest()
            .setInputType(AppSettingsState.getInstance().humanLanguage.lowercase(Locale.getDefault()))
            .setOutputType(ComputerLanguage.getComputerLanguage(event)!!.name)
            .setInstruction("Implement this specification")
            .setInputAttribute("type", "input")
            .setOutputAttrute("type", "output")
            .setInputText(selectedText)
            .buildCompletionRequest()
        val caret = event.getData(CommonDataKeys.CARET)
        val indent = UITools.getIndent(caret)
        UITools.redoableRequest(
            request, indent, event
        ) { newText: CharSequence? ->
            UITools.replaceString(
                editor.document, selectionStart, selectionEnd,
                newText!!
            )
        }
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (!UITools.hasSelection(e)) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if(computerLanguage == ComputerLanguage.Text) return false
            return true
        }
    }
}