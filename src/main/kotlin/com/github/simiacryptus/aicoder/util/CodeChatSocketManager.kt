package com.github.simiacryptus.aicoder.util

import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.StorageInterface
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager

open class CodeChatSocketManager(
    session: Session,
    val language: String,
    val filename: String,
    val codeSelection: String,
    api: ChatClient,
    model: ChatModel,
    storage: StorageInterface?,
) : ChatSocketManager(
    session = session,
    model = model,
  userInterfacePrompt = ("""
          # `""".trimIndent() + filename + """`
          
          ```""".trimIndent() + language + """
          """.trimIndent() + codeSelection + """
          ```
          """.trimIndent()).trim(),
    systemPrompt = """
          You are a helpful AI that helps people with coding.
          
          You will be answering questions about the following code located in `""" + filename + """`:
          
          ```""".trimIndent() + language + """
          """.trimIndent() + codeSelection + """
          ```
          
          Responses may use markdown formatting, including code blocks.
          """.trimIndent(),
    api = api,
    applicationClass = ApplicationServer::class.java,
    storage = storage,
) {
    override fun canWrite(user: User?): Boolean = true
}