package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger

/**
 * The PrintTreeAction class is an IntelliJ action that enables developers to print the tree structure of a PsiFile.
 * To use this action, first make sure that the "devActions" setting is enabled.
 * Then, open the file you want to print the tree structure of.
 * Finally, select the "PrintTreeAction" action from the editor context menu.
 * This will print the tree structure of the file to the log.
 */
class PrintTreeAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(e1: AnActionEvent) {
        log.warn(PsiUtil.printTree(PsiUtil.getLargestContainedEntity(e1)!!))
    }

    companion object {
        val log = Logger.getInstance(PrintTreeAction::class.java)
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (UITools.isSanctioned()) return false
            return if (!AppSettingsState.instance.devActions) false else null != PsiUtil.getLargestContainedEntity(
                e
            )
        }
    }
}