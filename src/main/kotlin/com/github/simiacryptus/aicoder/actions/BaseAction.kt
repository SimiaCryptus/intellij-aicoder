package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.actions.generic.GenerateFileFromRequirementsAction
import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.openai.OpenAIClient
import javax.swing.Icon

abstract class BaseAction(
    name: String? = null,
    description: String? = null,
    icon: Icon? = null,
    ) : AnAction(name, description, icon) {

    //override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    val api: OpenAIClient
        get() = IdeaOpenAIClient.api

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isEnabled(event)
        super.update(event)
    }


    abstract fun actionPerformed2(e: AnActionEvent)


    override fun actionPerformed(e: AnActionEvent) {
        IdeaOpenAIClient.lastEvent = e
        actionPerformed2(e)
    }

    open fun isEnabled(event: AnActionEvent): Boolean = true
}