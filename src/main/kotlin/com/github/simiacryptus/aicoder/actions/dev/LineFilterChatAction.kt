package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction.Companion
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop

class LineFilterChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/codeChat"

    override fun handle(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val session = StorageInterface.newGlobalID()
        val language = ComputerLanguage.getComputerLanguage(e)?.name ?: return
        val filename = FileDocumentManager.getInstance().getFile(editor.document)?.name ?: return
        val code = editor.caretModel.primaryCaret.selectedText ?: editor.document.text
        val lines = code.split("\n").toTypedArray()
        val codelines = lines.withIndex().joinToString("\n") { (i, line) ->
            "${i.toString().padStart(3, '0')} $line"
        }
        SessionProxyServer.agents[session] = object : ChatSocketManager(
            session = session,
            model = AppSettingsState.instance.smartModel.chatModel(),
            userInterfacePrompt = """
        |# `$filename`
        |
        |```$language
        |${code.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
        |```
        """.trimMargin().trim(),
            systemPrompt = """
        |You are a helpful AI that helps people with coding.
        |
        |You will be answering questions about the following code located in `$filename`:
        |
        |```$language
        |${codelines.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
        |```
        |
        |Responses may use markdown formatting. Lines from the prompt can be included by using the line number in a response line (e.g. `\nLINE\n`).
        |
        |For example:
        |
        |```text
        |001
        |## Injected subtitle
        |
        |025
        |026
        |
        |013
        |014
        |```
        """.trimMargin(),
            api = api,
            applicationClass = ApplicationServer::class.java,
            storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome),
        ) {
            override fun canWrite(user: User?): Boolean = true
            override fun renderResponse(response: String, task: SessionTask): String {
                return renderMarkdown(response.split("\n").joinToString("\n") {
                    when {
                        // Is numeric, use line if in range
                        it.toIntOrNull()?.let { i -> lines.indices.contains(i) } == true -> lines[it.toInt()]
                        // Otherwise, use response
                        else -> it
                    }
                }
                )
            }
        }

        val server = AppServer.getServer(e.project)

        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                Desktop.getDesktop().browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.devActions

    companion object {
        private val log = LoggerFactory.getLogger(LineFilterChatAction::class.java)

    }
}
