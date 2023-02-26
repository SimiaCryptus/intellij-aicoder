package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*

/**
 * The CommentsAction class is an IntelliJ action that allows users to add detailed comments to their code.
 * To use the CommentsAction, first select a block of code in the editor.
 * Then, select the action in the context menu.
 * The action will then generate a new version of the code with comments added.
 */
class CommentsAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectionStart = primaryCaret.selectionStart
        val selectionEnd = primaryCaret.selectionEnd
        val selectedText = primaryCaret.selectedText
        val outputHumanLanguage = AppSettingsState.getInstance().humanLanguage
        val language = ComputerLanguage.getComputerLanguage(e)
        val settings = AppSettingsState.getInstance()
        val request = settings.createTranslationRequest()
                .setInputType(Objects.requireNonNull(language)!!.name)
                .setOutputType(language!!.name)
                .setInstruction(UITools.getInstruction("Rewrite to include detailed $outputHumanLanguage code comments for every line"))
                .setInputAttribute("type", "commented")
                .setOutputAttrute("type", "uncommented")
                .setOutputAttrute("style", settings.style)
                .setInputText(selectedText)
                .buildCompletionRequest()
        val caret = e.getData(CommonDataKeys.CARET)
        val indent = UITools.getIndent(caret)
        UITools.redoableRequest(request, indent, e
        ) { newText: CharSequence? -> UITools.replaceString(editor.document, selectionStart, selectionEnd, newText!!) }
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