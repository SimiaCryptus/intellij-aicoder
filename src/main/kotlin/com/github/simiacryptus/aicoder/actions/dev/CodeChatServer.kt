package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.body.*
import org.eclipse.jetty.util.resource.Resource

class CodeChatServer(
    val project: Project,
    val language: String,
    val codeSelection: String,
) : SkyenetBasicChat(
    applicationName = "Code Chat",
    model = AppSettingsState.instance.defaultChatModel()
) {

    private val rootOperationID = (0..5).map { ('a'..'z').random() }.joinToString("")
    private var rootMessageTrail: String = ""

    override fun newSession(sessionId: String): CodeChatSession {
        val newSession = CodeChatSession(sessionId)
rootMessageTrail =
"""$rootOperationID,<div><h3>Code:</h3><pre><code class="language-$language">${htmlEscape(codeSelection)}</code></pre></div>"""
        newSession.send(rootMessageTrail)
        return newSession
    }

    private fun htmlEscape(html: String): String {
        return html.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
            .replace("'", "&#039;")
    }

    open inner class CodeChatSession(sessionId: String) : BasicChatSession(
        parent = this@CodeChatServer,
        model = model,
        sessionId = sessionId
    ) {
        override fun run(userMessage: String) {
            var messageTrail = ChatSession.divInitializer()
            send("""$messageTrail<div>$userMessage</div><div>${SkyenetSessionServerBase.spinner}</div>""")
            messages += OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.user, userMessage)
            val response = api.chat(chatRequest, model).choices.first()?.message?.content.orEmpty()
            messages += OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.assistant, response)
            messageTrail += ChatSessionFlexmark.renderMarkdown(response)
            send(messageTrail)
        }

        override val messages = listOf(
            OpenAIClient.ChatMessage(
                OpenAIClient.ChatMessage.Role.system, """
                    |You are a helpful AI that helps people with coding.
                    |
                    |You will be answering questions about the following code:
                    |
                    |```$language
                    |$codeSelection
                    |```
                    |
                    |Responses may use markdown formatting.
                    """.trimMargin()
            )
        ).toMutableList()

        private val chatRequest: OpenAIClient.ChatRequest
            get() {
                val chatRequest = OpenAIClient.ChatRequest()
                val model = AppSettingsState.instance.defaultChatModel()
                chatRequest.model = model.modelName
                chatRequest.temperature = AppSettingsState.instance.temperature
                chatRequest.messages = messages.toTypedArray()
                return chatRequest
            }
    }

    override val baseResource: Resource
        get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase))

}