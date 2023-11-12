package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.sessions.*
import com.simiacryptus.skyenet.util.ClasspathResource
import com.simiacryptus.skyenet.util.MarkdownUtil
import org.eclipse.jetty.util.resource.Resource
import org.intellij.lang.annotations.Language

class CodeChatServer(
    val project: Project,
    val language: String,
    val codeSelection: String,
    val api: OpenAIClient,
) : ChatApplicationBase(
    applicationName = "Code Chat",
) {

    private val rootOperationID = (0..5).map { ('a'..'z').random() }.joinToString("")
    private var rootMessageTrail: String = ""

    override fun newSession(sessionId: String): ChatSession {
        val newSession = ChatSession(
            sessionId = sessionId,
            parent = this@CodeChatServer,
            model = AppSettingsState.instance.defaultChatModel(),
            api = api,
            visiblePrompt = """
            <div><h3>Code:</h3>
                <pre><code class="language-$language">${htmlEscape(codeSelection)}</code></pre>
            </div>
        """.trimIndent().trim(),
            hiddenPrompt = "",
            systemPrompt = """
                    |You are a helpful AI that helps people with coding.
                    |
                    |You will be answering questions about the following code:
                    |
                    |```$language
                    |$codeSelection
                    |```
                    |
                    |Responses may use markdown formatting.
                    """.trimMargin(),
        )
        @Language("HTML") val html = """
            <div><h3>Code:</h3>
                <pre><code class="language-$language">${htmlEscape(codeSelection)}</code></pre>
            </div>
            """.trimIndent().trim()
        rootMessageTrail = """$rootOperationID,$html"""
        newSession.send(rootMessageTrail)
        return newSession
    }

    override fun processMessage(
        sessionId: String,
        userMessage: String,
        session: PersistentSessionBase,
        sessionDiv: SessionDiv,
        socket: MessageWebSocket
    ) {
        TODO("Not yet implemented")
    }

    private fun htmlEscape(html: String): String {
        return html.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
            .replace("'", "&#039;")
    }

    override val baseResource: Resource
        get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase)!!)

}