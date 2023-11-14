@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")

package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
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
        server.addApp("/$uuid", CodeChatServer(e.project!!, language, selectedText, api = api))
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
        if (UITools.isSanctioned()) return false
        return true
    }

}
