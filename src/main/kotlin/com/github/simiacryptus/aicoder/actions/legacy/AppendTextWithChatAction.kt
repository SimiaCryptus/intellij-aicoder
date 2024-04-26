package com.github.simiacryptus.aicoder.actions.legacy

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.ApiModel.*
import com.simiacryptus.jopenai.util.ClientUtil.toContentList

class AppendTextWithChatAction : SelectionAction<String>() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun processSelection(state: SelectionState, config: String?): String {
        val settings = AppSettingsState.instance
        val request = ChatRequest(
            model = settings.smartModel.chatModel().modelName,
            temperature = settings.temperature
        ).copy(
            temperature = settings.temperature,
            messages = listOf(
                ChatMessage(Role.system, "Append text to the end of the user's prompt".toContentList(), null),
                ChatMessage(Role.user, state.selectedText.toString().toContentList(), null)
            ),
        )
        val chatResponse = api.chat(request, settings.smartModel.chatModel())
        val b4 = state.selectedText ?: ""
        val str = chatResponse.choices[0].message?.content ?: ""
        return b4 + if (str.startsWith(b4)) str.substring(b4.length) else str
    }
}