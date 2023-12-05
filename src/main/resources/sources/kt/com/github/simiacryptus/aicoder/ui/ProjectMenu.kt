package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

open class ProjectMenu : com.intellij.openapi.actionSystem.DefaultActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return AppSettingsState.instance.fileActions.edit(super.getChildren(e))
    }
}