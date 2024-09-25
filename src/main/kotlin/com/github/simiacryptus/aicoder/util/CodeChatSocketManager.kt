package com.github.simiacryptus.aicoder.util

import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager

open class CodeChatSocketManager(
    session: Session,
    val language: String,
    val filename: String,
    val codeSelection: String,
    api: ChatClient,
    model: ChatModels,
    storage: StorageInterface?,
) : ChatSocketManager(
    session = session,
    model = model,
    userInterfacePrompt = """
        |# `$filename`
        |
        |```$language
        |$codeSelection
        |```
        """.trimMargin().trim(),
    systemPrompt = """
        |You are a helpful AI that helps people with coding.
        |
        |You will be answering questions about the following code located in `$filename`:
        |
        |```$language
        |$codeSelection
        |```
        |
        |Responses may use markdown formatting, including code blocks.
        """.trimMargin(),
    api = api,
    applicationClass = ApplicationServer::class.java,
    storage = storage,
) {
    override fun canWrite(user: User?): Boolean = true
}