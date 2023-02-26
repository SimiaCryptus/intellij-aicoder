package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*
import javax.swing.JOptionPane

/**
 * The CustomEditAction class is an IntelliJ action that allows users to edit computer language code.
 * When the action is triggered, a dialog box will appear prompting the user to enter an instruction.
 * The instruction will then be used to transform the selected code.
 *
 * To use the CustomEditAction, first select the code that you want to edit.
 * Then, select the action in the context menu.
 * A dialog box will appear, prompting you to enter an instruction.
 * Enter the instruction and press OK.
 */
class CustomEditAction : AnAction() {
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
        val computerLanguage = ComputerLanguage.getComputerLanguage(e)!!.name
        val instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE) ?: return
        if(instruction.isBlank()) return
        val settings = AppSettingsState.getInstance()
        settings.addInstructionToHistory(instruction)
        val request = settings.createTranslationRequest()
            .setInputType(computerLanguage)
            .setOutputType(computerLanguage)
            .setInstruction(instruction)
            .setInputAttribute("type", "before")
            .setOutputAttrute("type", "after")
            .setInputText(IndentedText.fromString(selectedText).textBlock)
            .buildCompletionRequest()
        val caret = e.getData(CommonDataKeys.CARET)
        val indent = UITools.getIndent(caret)
        UITools.redoableRequest(
            request, indent, e
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