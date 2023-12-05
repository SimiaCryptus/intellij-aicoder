 package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.ApiModel.ChatMessage
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.ClientUtil.toContentList

 class AppendAction : SelectionAction<String>() {
    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun processSelection(state: SelectionState, config: String?): String {
        val settings = AppSettingsState.instance
        val request = settings.createChatRequest().copy(
            temperature = settings.temperature,
            messages = listOf(
                ChatMessage(Role.system, "Append text to the end of the user's prompt".toContentList(), null),
                ChatMessage(Role.user, state.selectedText.toString().toContentList(), null)
            ),
        )
        val chatResponse = api.chat(request, settings.defaultChatModel())
        val b4 = state.selectedText ?: ""
        val str = chatResponse.choices[0].message?.content ?: ""
        return b4 + if (str.startsWith(b4)) str.substring(b4.length) else str
    }
}