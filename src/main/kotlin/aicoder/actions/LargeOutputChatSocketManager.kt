package aicoder.actions

import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.core.actors.LargeOutputActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager

class LargeOutputChatSocketManager(
  session: Session,
  model: ChatModel,
  parsingModel: ChatModel,
  userInterfacePrompt: String,
  systemPrompt: String,
  api: ChatClient,
  storage: StorageInterface?,
  applicationClass: Class<out ApplicationServer>,
  private val largeOutputActor: LargeOutputActor
) : ChatSocketManager(
  session = session,
  model = model,
  parsingModel = parsingModel,
  userInterfacePrompt = userInterfacePrompt,
  systemPrompt = systemPrompt,
  api = api,
  storage = storage,
  applicationClass = applicationClass
) {
  override fun respond(api: ChatClient, messages: List<ApiModel.ChatMessage>) =
    largeOutputActor.respond(messages.flatMap { it.content?.mapNotNull { it.text } ?: emptyList() }.toList(), api = api)
}