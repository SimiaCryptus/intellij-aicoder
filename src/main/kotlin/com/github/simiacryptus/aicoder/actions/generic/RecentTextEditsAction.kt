package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.ArrayList

/**
 * The RecentTextEditsAction is an IntelliJ action that allows users to quickly access and apply recent text edits.
 * This action is triggered when the user has selected some text in the editor.
 * When the action is triggered, a list of recent text edits is displayed,
 * allowing the user to quickly select and apply one of the edits.
 */
class RecentTextEditsAction : ActionGroup() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val children = ArrayList<AnAction>()
        for (instruction in AppSettingsState.getInstance().editHistory) {
            val id = children.size + 1
            var text: String
            text = if (id < 10) {
                String.format("_%d: %s", id, instruction)
            } else {
                String.format("%d: %s", id, instruction)
            }
            children.add(object : AnAction(text, instruction, null) {
                override fun actionPerformed(event: AnActionEvent) {
                    val editor = event.getRequiredData(CommonDataKeys.EDITOR)
                    val caretModel = editor.caretModel
                    val primaryCaret = caretModel.primaryCaret
                    val selectionStart = primaryCaret.selectionStart
                    val selectionEnd = primaryCaret.selectionEnd
                    val selectedText = primaryCaret.selectedText
                    val settings = AppSettingsState.getInstance()
                    settings.addInstructionToHistory(instruction)
                    val request = settings.createEditRequest()
                        .setInstruction(instruction)
                        .setInput(IndentedText.fromString(selectedText).textBlock.toString())
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
            })
        }
        return children.toTypedArray()
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            return UITools.hasSelection(e)
        }
    }
}