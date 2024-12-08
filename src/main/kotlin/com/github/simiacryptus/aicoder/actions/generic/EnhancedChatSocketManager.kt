package com.github.simiacryptus.aicoder.actions.generic

import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.core.actors.LargeOutputActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask

class EnhancedChatSocketManager(
    session: Session,
    model: ChatModel,
    userInterfacePrompt: String,
    systemPrompt: String,
    api: ChatClient,
    storage: StorageInterface?,
    applicationClass: Class<out ApplicationServer>,
    private val largeOutputActor: LargeOutputActor
) : ChatSocketManager(
    session = session,
    model = model,
    userInterfacePrompt = userInterfacePrompt,
    systemPrompt = systemPrompt,
    api = api,
    storage = storage,
    applicationClass = applicationClass
) {
    override fun respond(api: ChatClient, messages: List<ApiModel.ChatMessage>): String {
        return largeOutputActor.response(*messages.toTypedArray(), api=api).choices.first().message?.content
            ?: throw RuntimeException("No response from LLM")
    }
}