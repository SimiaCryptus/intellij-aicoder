package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class RecentCodeEditsAction extends ActionGroup {
    void update(AnActionEvent e) {
        e.presentation.setEnabledAndVisible(isEnabled(e))
        super.update(e)
    }

    AnAction[] getChildren(AnActionEvent e) {
        if (null == e) return []
        def children = []
        for (instruction in AppSettingsState.instance.getRecentCommands("customEdits").mostUsedHistory.keySet()) {
            def id = children.size() + 1
            def text = id < 10 ? "_${id}: ${instruction}" : "${id}: ${instruction}"
            def element = new CustomEditAction() {
                String getInstruction() {
                    return instruction
                }
            }
            element.templatePresentation.text = text
            element.templatePresentation.description = instruction
            element.templatePresentation.icon = null
            children.add(element)
        }
        return children as AnAction[]
    }

    static boolean isEnabled(AnActionEvent e) {
        if (UITools.isSanctioned()) return false
        if (!UITools.hasSelection(e)) return false
        def computerLanguage = ComputerLanguage.getComputerLanguage(e)
        return computerLanguage != ComputerLanguage.Text
    }
}