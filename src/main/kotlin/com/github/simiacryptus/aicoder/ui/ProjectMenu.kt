package com.github.simiacryptus.aicoder.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

open class ProjectMenu : com.intellij.openapi.actionSystem.DefaultActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val children = super.getChildren(e)
        return children
//        return AppSettingsState.instance.fileActions.edit(children)
    }
}