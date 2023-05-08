package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * The RecentCodeEditsAction is an IntelliJ action that allows users to quickly access and apply recent code edits.
 * This action is triggered when a user selects a piece of code and then right-clicks to bring up the context menu.
 * The RecentCodeEditsAction will then display a list of recent code edits that the user can select from and apply to the selected code.
 * When the user selects a code edit, the action will generate a new version of the selected code with the code edit applied.
 * Finally, the new version of the code will be inserted into the document, replacing the original code.
 */
class RecentCodeEditsAction : ActionGroup() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if(null == e) return arrayOf()
        val children = ArrayList<AnAction>()
        for (instruction in AppSettingsState.instance.editHistory) {
            val id = children.size + 1
            var text: String = if (id < 10) {
                String.format("_%d: %s", id, instruction)
            } else {
                String.format("%d: %s", id, instruction)
            }
            children.add(object : CustomEditAction(text, instruction, null) {
                override fun getInstruction(): String? {
                    return instruction
                }
            })
        }
        return children.toTypedArray()
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (UITools.isSanctioned()) return false
            if (!UITools.hasSelection(e)) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            return computerLanguage != ComputerLanguage.Text
        }
    }
}