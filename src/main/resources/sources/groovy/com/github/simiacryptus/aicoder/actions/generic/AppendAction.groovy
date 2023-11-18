package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.OpenAIClient
import org.jetbrains.annotations.Nullable

class AppendAction extends SelectionAction<String> {
    @Override
    java.lang.String getConfig(@Nullable Project project) {
        return ""
    }

    @Override
    String processSelection(SelectionState state, String config) {
        def settings = AppSettingsState.instance
        def request = settings.createChatRequest()
        request.temperature = AppSettingsState.instance.temperature
        request.messages = [
                new OpenAIClient.ChatMessage(
                        OpenAIClient.Role.system,
                        "Append text to the end of the user's prompt", null
                ),
                new OpenAIClient.ChatMessage(
                        OpenAIClient.Role.user,
                        state.selectedText.toString(), null
                )
        ]
        def chatResponse = api.chat(request, AppSettingsState.instance.defaultChatModel())
        def b4 = state.selectedText ?: ""
        def str = (chatResponse.choices[0].message?.content ?: "")
        return b4 + (str.startsWith(b4) ? str.substring(b4.length) : str)
    }
}