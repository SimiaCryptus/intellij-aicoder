﻿package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.util.UITools.retry
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * The RedoLast action is an IntelliJ action that allows users to redo the last AI Coder action they performed in the editor.
 * To use this action, open the editor and select the RedoLast action from the editor context menu.
 * This will redo the last action that was performed in the editor.
 */
class RedoLast : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        retry[e.getRequiredData(CommonDataKeys.EDITOR).document]!!.run()
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        return null != retry[event.getRequiredData(CommonDataKeys.EDITOR).document]
    }

}
