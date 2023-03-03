package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*
import javax.swing.JOptionPane

/**
 * The GenericEdit action is an IntelliJ action that allows users to edit text in their code.
 * When the action is triggered, a dialog box will appear prompting the user to enter an instruction.
 * The instruction will be added to the history and used to edit the selected text.
 * The action will then replace the selected text with the edited version.
 *
 * To use the GenericEdit action, first select the text you want to edit.
 * Then, select the action in the context menu.
 * A dialog box will appear prompting you to enter an instruction.
 * Enter the instruction and press OK.
 * The selected text will then be replaced with the edited version.
 */
class EditAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val settings = AppSettingsState.getInstance()
        val instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Text", JOptionPane.QUESTION_MESSAGE)
        settings.addInstructionToHistory(instruction)
        val caret = event.getData(CommonDataKeys.CARET)
        val selectedText: CharSequence? = caret!!.selectedText
        val editRequest = settings.createEditRequest()
            .setInput(selectedText.toString())
            .setInstruction(instruction)
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        val selectionEnd = caret.selectionEnd
        val selectionStart = caret.selectionStart
        UITools.redoableRequest(
            editRequest, "", event
        ) { newText: CharSequence? ->
            UITools.replaceString(
                document, selectionStart, selectionEnd,
                newText!!
            )
        }
    }

    companion object {
        @Suppress("unused")
        private fun isEnabled(e: AnActionEvent): Boolean {
            return UITools.hasSelection(e)
        }
    }
}