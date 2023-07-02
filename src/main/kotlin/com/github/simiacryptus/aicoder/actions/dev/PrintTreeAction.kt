package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.simiacryptus.openai.APIClientBase

/**
 * The PrintTreeAction class is an IntelliJ action that enables developers to print the tree structure of a PsiFile.
 * To use this action, first make sure that the "devActions" setting is enabled.
 * Then, open the file you want to print the tree structure of.
 * Finally, select the "PrintTreeAction" action from the editor context menu.
 * This will print the tree structure of the file to the log.
 */
class PrintTreeAction : BaseAction() {


    override fun handle(e1: AnActionEvent) {
        log.warn(PsiUtil.printTree(PsiUtil.getLargestContainedEntity(e1)!!))
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (APIClientBase.isSanctioned()) return false
        return AppSettingsState.instance.devActions
    }

    companion object {
        val log = Logger.getInstance(PrintTreeAction::class.java)
    }
}
