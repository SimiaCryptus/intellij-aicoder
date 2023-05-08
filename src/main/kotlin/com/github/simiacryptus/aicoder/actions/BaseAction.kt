package com.github.simiacryptus.aicoder.actions

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

    val api: OpenAIClient
        get() = UITools.api

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    open fun isEnabled(e: AnActionEvent): Boolean = true
}