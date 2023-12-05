package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

open class EditorMenu : com.intellij.openapi.actionSystem.DefaultActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return AppSettingsState.instance.editorActions.edit(super.getChildren(e))
    }
}


