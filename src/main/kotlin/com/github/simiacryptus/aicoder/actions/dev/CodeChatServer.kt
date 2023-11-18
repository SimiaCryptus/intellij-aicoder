package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.chat.ChatServer
import com.simiacryptus.skyenet.chat.ChatSession
import com.simiacryptus.skyenet.util.ClasspathResource
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext

class CodeChatServer(
    val project: Project,
    val language: String,
    val codeSelection: String,
    val api: OpenAIClient,
    resourceBase: String = "codeChat",
) : ChatServer(
    resourceBase = resourceBase,
) {
    override val applicationName: String
        get() = "Code Chat"

    override fun newSession(userId: String?, sessionId: String) = object : ChatSession(
        sessionId = sessionId,
        parent = this@CodeChatServer,
        model = AppSettingsState.instance.defaultChatModel(),
        api = api,
        visiblePrompt = """
            |<div><h3>Code:</h3>
            |    <pre><code class="language-$language">${htmlEscape(codeSelection)}</code></pre>
            |</div>
            """.trimMargin().trim(),
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
        applicationClass = ApplicationBase::class.java,
    ) {
        override fun canWrite(user: String?): Boolean = true
    }

    private fun htmlEscape(html: String): String {
        return html.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
            .replace("'", "&#039;")
    }

    override val baseResource: Resource
        get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase)!!)

    override fun configure(webAppContext: WebAppContext, path: String, baseUrl: String) {
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/appInfo", AppInfoServlet()), "/appInfo")
        super.configure(webAppContext, path, baseUrl)
    }
}