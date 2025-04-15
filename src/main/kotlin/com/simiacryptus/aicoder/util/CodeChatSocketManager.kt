package com.simiacryptus.aicoder.util

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
  parsingModel: ChatModel,
  storage: StorageInterface?,
) : ChatSocketManager(
  session = session,
  model = model,
  parsingModel = parsingModel,
  userInterfacePrompt = "# `$filename`\n\n```$language\n$codeSelection\n```",
  systemPrompt = "\nYou are a helpful AI that helps people with coding.\n\nYou will be answering questions about the following code located in `$filename`:\n\n```$language\n$codeSelection\n```\n\nResponses may use markdown formatting, including code blocks.",
  api = api,
  applicationClass = ApplicationServer::class.java,
  storage = storage,
) {
  override fun canWrite(user: User?): Boolean = true
}