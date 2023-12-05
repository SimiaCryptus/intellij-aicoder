package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.dev.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.skyenet.webui.chat.CodeChatServer
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.util.*

class CodeChatAction : BaseAction() {

    override fun handle(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectedText = primaryCaret.selectedText ?: editor.document.text
        val language = ComputerLanguage.getComputerLanguage(e)?.name ?: return
        val server = AppServer.getServer(e.project)
        val uuid = UUID.randomUUID().toString()
        server.addApp("/$uuid", CodeChatServer(language, selectedText, api = api, model = AppSettingsState.instance.defaultChatModel()))
        Thread {
            Thread.sleep(500)
            try {
                Desktop.getDesktop().browse(server.server.uri.resolve("/$uuid/index.html"))
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        return true
    }

    companion object {
        private val log = LoggerFactory.getLogger(CodeChatAction::class.java)
    }
}
