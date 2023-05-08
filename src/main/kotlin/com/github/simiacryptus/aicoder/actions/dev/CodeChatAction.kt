package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.util.containers.orNull
import com.intellij.util.ui.FormBuilder
import com.simiacryptus.openai.ChatMessage
import com.simiacryptus.openai.ChatRequest
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.body.ClasspathResource
import com.simiacryptus.skyenet.body.SkyenetSessionServer
import com.simiacryptus.skyenet.heart.WeakGroovyInterpreter
import com.simiacryptus.skyenet.util.AbbrevWhitelistYamlDescriber
import org.eclipse.jetty.util.resource.Resource
import org.jdesktop.swingx.JXButton
import java.awt.Desktop
import java.util.Map
import java.util.function.Supplier

class CodeChatAction : BaseAction() {

    inner class CodeChatServer(
        val e: AnActionEvent,
        port: Int,
        val language: String,
        val codeSelection: String,
    ) : SkyenetSessionServer(
        applicationName = "Code Chat",
        yamlDescriber = AbbrevWhitelistYamlDescriber(
            "com.simiacryptus",
        ),
        baseURL = "http://localhost:$port",
        model = AppSettingsState.instance.model_chat
    ) {

        val rootOperationID = (0..5).map { ('a'..'z').random() }.joinToString("")
        var rootMessageTrail: String = ""

        override fun newSession(sessionId: String): SessionState {
            val newSession = CodeChatSession(sessionId)
            rootMessageTrail = """$rootOperationID,<div><h3>Code:</h3><pre><code class="language-$language">$codeSelection</code></pre></div>"""
            newSession.send(rootMessageTrail)
            return newSession
        }

        open inner class CodeChatSession(sessionId: String) : SkyenetSession(sessionId) {
            override fun run(userMessage: String) {
                val operationID = (0..5).map { ('a'..'z').random() }.joinToString("")
                var messageTrail = """$operationID,<button class="cancel-button" data-id="$operationID">&times;</button>"""
                messageTrail += """<div>$userMessage</div>"""
                send("""$messageTrail<div>$spinner</div>""")
                messages += ChatMessage(ChatMessage.Role.user, userMessage)
                val response = api.chat(chatRequest).response.orNull()?.toString() ?: "???"
                messages += ChatMessage(ChatMessage.Role.assistant, response)
                messageTrail += """<div><pre>$response</pre></div>"""
                send("""$messageTrail""")
            }

            val messages = listOf(
                ChatMessage(
                    ChatMessage.Role.system, """
                        |You are a helpful AI that helps people with coding.
                        |You will be answering questions about the following code:
                        |```$language
                        |$codeSelection
                        |```
                        """.trimMargin()
                )
            ).toMutableList()
            val chatRequest: ChatRequest
                get() {
                    val chatRequest = ChatRequest()
                    chatRequest.model = AppSettingsState.instance.model_chat
                    chatRequest.max_tokens = AppSettingsState.instance.maxTokens
                    chatRequest.temperature = AppSettingsState.instance.temperature
                    chatRequest.messages = messages.toTypedArray()
                    return chatRequest
                }
        }

        override val baseResource: Resource
            get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase))

        override fun hands() = java.util.HashMap<String, Object>() as Map<String, Object>

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

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectedText = primaryCaret.selectedText ?: editor.document.text
        val language = ComputerLanguage.getComputerLanguage(event)!!.name

        val port = (8000 + (Math.random() * 1000).toInt())
        val skyenet = CodeChatServer(event, port, language, selectedText)
        val server = skyenet.start(port)

        Thread {
            try {
                log.info("Server Running on $port")
                server.join()
            } finally {
                log.info("Server Stopped")
            }
        }.start()

        Thread {
            try {
                val formBuilder = FormBuilder.createFormBuilder()
                val openButton = JXButton("Open")
                openButton.addActionListener {
                    Desktop.getDesktop().browse(server.uri.resolve("/index.html"))
                }
                formBuilder.addLabeledComponent("Server Running on $port", openButton)
                val showOptionDialog =
                    UITools.showOptionDialog(
                        formBuilder.panel,
                        "Close",
                        title = "Server Running on $port",
                        modal = true
                    )
                log.info("showOptionDialog = $showOptionDialog")
            } finally {
                log.info("Stopping Server")
                server.stop()
            }
        }.start()

        Thread {
            Thread.sleep(500)
            Desktop.getDesktop().browse(server.uri.resolve("/index.html"))
        }.start()
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        //if (!AppSettingsState.instance.devActions) return false
        return true
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(CodeChatAction::class.java)
    }

}