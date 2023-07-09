package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.openai.OpenAIClient
import org.slf4j.LoggerFactory
import javax.swing.Icon

abstract class BaseAction(
    name: String? = null,
    description: String? = null,
    icon: Icon? = null,
) : AnAction(name, description, icon) {

    val log by lazy { LoggerFactory.getLogger(javaClass) }
    //override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    val api: OpenAIClient
        get() = IdeaOpenAIClient.api

    final override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isEnabled(event)
        super.update(event)
    }

    abstract fun handle(e: AnActionEvent)


    final override fun actionPerformed(e: AnActionEvent) {
        IdeaOpenAIClient.lastEvent = e
        handle(e)
    }

    open fun isEnabled(event: AnActionEvent): Boolean = true
}
