package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.retry
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * The RedoLast action is an IntelliJ action that allows users to redo the last AI Coder action they performed in the editor.
 * To use this action, open the editor and select the RedoLast action from the editor context menu.
 * This will redo the last action that was performed in the editor.
 */
class RedoLast : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        retry[e.getRequiredData(CommonDataKeys.EDITOR).document]!!.run()
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (UITools.isSanctioned()) return false
            return null != retry[e.getRequiredData(CommonDataKeys.EDITOR).document]
        }
    }
}