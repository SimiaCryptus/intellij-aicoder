@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")

package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.openai.OpenAIClient.ChatMessage
import com.simiacryptus.openai.OpenAIClient.ChatRequest
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.body.*
import com.simiacryptus.skyenet.heart.WeakGroovyInterpreter
import org.eclipse.jetty.util.resource.Resource
import java.awt.Desktop
import java.util.function.Supplier

class CodeChatAction : BaseAction() {

    inner class CodeChatServer(
        val e: AnActionEvent,
        port: Int,
        val language: String,
        val codeSelection: String,
        baseURL: String = "http://localhost:$port",
    ) : SkyenetCodingSessionServer(
        applicationName = "Code Chat",
        model = AppSettingsState.instance.defaultChatModel(),
        apiKey = AppSettingsState.instance.apiKey,
    ) {

        val rootOperationID = (0..5).map { ('a'..'z').random() }.joinToString("")
        var rootMessageTrail: String = ""

        override fun newSession(sessionId: String): SkyenetCodingSession {
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

        open inner class CodeChatSession(sessionId: String) : SkyenetCodingSession(sessionId, this@CodeChatServer) {
            override fun run(userMessage: String) {
                var messageTrail = ChatSession.divInitializer()
                send("""$messageTrail<div>$userMessage</div><div>$spinner</div>""")
                messages += ChatMessage(ChatMessage.Role.user, userMessage)
                val response = api.chat(chatRequest, model).choices.first()?.message?.content.orEmpty()
                messages += ChatMessage(ChatMessage.Role.assistant, response)
                messageTrail += ChatSessionFlexmark.renderMarkdown(response)
                send(messageTrail)
            }

            val messages = listOf(
                ChatMessage(
                    ChatMessage.Role.system, """
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
            val chatRequest: ChatRequest
                get() {
                    val chatRequest = ChatRequest()
                    val model = AppSettingsState.instance.defaultChatModel()
                    chatRequest.model = model.modelName
                    chatRequest.max_tokens = model.maxTokens
                    chatRequest.temperature = AppSettingsState.instance.temperature
                    chatRequest.messages = messages.toTypedArray()
                    return chatRequest
                }
        }

        override val baseResource: Resource
            get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase))

        override fun hands() = java.util.HashMap<String, Object>() as java.util.Map<String, Object>

        override fun toString(e: Throwable): String {
            return e.message ?: e.toString()
        }

        override fun heart(hands: java.util.Map<String, Object>): Heart = object : WeakGroovyInterpreter(hands) {
            override fun <T : Any> wrapExecution(fn: Supplier<T?>): T? {
                return UITools.run(
                    e.project, "Running Script", false
                ) {
                    fn.get()
                }
            }
        }
    }

    override fun handle(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectedText = primaryCaret.selectedText ?: editor.document.text
        val language = ComputerLanguage.getComputerLanguage(event)?.name ?: return

        val port = (8000 + (Math.random() * 8000).toInt())
        val skyenet = CodeChatServer(event, port, language, selectedText, baseURL = "http://localhost:$port")
        val server = skyenet.start(port)

        Thread {
            try {
                UITools.run(
                    event.project, "Running CodeChat Server on $port", false
                ) {
                    while (!it.isCanceled && server.isRunning) {
                        Thread.sleep(1000)
                    }
                    if(it.isCanceled) {
                        log.info("Server cancelled")
                        server.stop()
                    } else {
                        log.info("Server stopped")
                    }
                }
            } finally {
                log.info("Stopping Server")
                server.stop()
            }
        }.start()

        Thread {
            Thread.sleep(500)
            try {
                Desktop.getDesktop().browse(server.uri.resolve("/index.html"))
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        return AppSettingsState.instance.devActions
    }

}
