package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.openai.OpenAIClient

class AppendAction extends SelectionAction {
    @Override
    String processSelection(SelectionState state) {
        def settings = AppSettingsState.instance
        def request = settings.createChatRequest()
        request.temperature = AppSettingsState.instance.temperature
        request.messages = [
            new OpenAIClient.ChatMessage(
                OpenAIClient.ChatMessage.Role.system,
                "Append text to the end of the user's prompt"
            ),
            new OpenAIClient.ChatMessage(
                OpenAIClient.ChatMessage.Role.user,
                state.selectedText.toString()
            )
        ]
        def chatResponse = api.chat(request, AppSettingsState.instance.defaultChatModel())
        def b4 = state.selectedText ?: ""
        def str = (chatResponse.choices[0].message?.content ?: "")
        return b4 + (str.startsWith(b4) ? str.substring(b4.length) : str)
    }
}