package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*

/**
 * The ToHumanLanguageAction class is an action that is used to translate code written in a computer language into a human language.
 * It is triggered when the user selects a piece of code and then clicks the action.
 * The action will then send a request to the server to translate the code into the human language specified in the AppSettingsState.
 * The translated text will then be inserted into the document at the location of the selected code.
 */
class ToHumanLanguageAction : AnAction() {
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
        val language = ComputerLanguage.getComputerLanguage(event)
        val computerLanguage = language!!.name
        val settings = AppSettingsState.instance
        val request = settings.createTranslationRequest()
            .setInstruction(UITools.getInstruction("Describe this code"))
            .setInputText(selectedText)
            .setInputType(computerLanguage)
            .setInputAttribute("type", "input")
            .setOutputType(AppSettingsState.instance.humanLanguage.lowercase(Locale.getDefault()))
            .setOutputAttrute("type", "output")
            .setOutputAttrute("style", settings.style)
            .buildCompletionRequest()
        val caret = event.getData(CommonDataKeys.CARET)
        val indent = UITools.getIndent(caret)
        val document = editor.document
        UITools.redoableRequest(
            request, indent, event
        ) { newText: CharSequence? ->
            UITools.replaceString(
                document, selectionStart, selectionEnd,
                newText!!
            )
        }
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (UITools.isSanctioned()) return false
            if (!UITools.hasSelection(e)) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (computerLanguage == ComputerLanguage.Text) return false
            return true
        }
    }
}