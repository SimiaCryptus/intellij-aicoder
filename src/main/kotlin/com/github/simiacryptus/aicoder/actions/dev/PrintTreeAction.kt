package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import org.slf4j.LoggerFactory

/**
 * The PrintTreeAction class is an IntelliJ action that enables developers to print the tree structure of a PsiFile.
 * To use this action, first make sure that the "devActions" setting is enabled.
 * Then, open the file you want to print the tree structure of.
 * Finally, select the "PrintTreeAction" action from the editor context menu.
 * This will print the tree structure of the file to the log.
 */
class PrintTreeAction : BaseAction() {


    override fun handle(e: AnActionEvent) {
        log.warn(PsiUtil.printTree(PsiUtil.getLargestContainedEntity(e)!!))
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        return AppSettingsState.instance.devActions
    }

    companion object {
        private val log = LoggerFactory.getLogger(PrintTreeAction::class.java)
    }
}
