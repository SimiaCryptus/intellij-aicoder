package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class RecentCodeEditsAction : ActionGroup() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return emptyArray()
        val children = mutableListOf<AnAction>()
        for ((instruction, _) in AppSettingsState.instance.getRecentCommands("customEdits").mostUsedHistory) {
            val id = children.size + 1
            val text = if (id < 10) "_$id: $instruction" else "$id: $instruction"
            val element = object : CustomEditAction() {
                override fun getConfig(project: Project?): String {
                    return instruction
                }
            }
            element.templatePresentation.text = text
            element.templatePresentation.description = instruction
            element.templatePresentation.icon = null
            children.add(element)
        }
        return children.toTypedArray()
    }

    companion object {
        fun isEnabled(e: AnActionEvent): Boolean {
            if (!UITools.hasSelection(e)) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e)
            return computerLanguage != ComputerLanguage.Text
        }
    }
}