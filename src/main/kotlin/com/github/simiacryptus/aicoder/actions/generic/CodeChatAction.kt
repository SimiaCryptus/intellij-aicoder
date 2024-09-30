package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.CodeChatSocketManager
import com.github.simiacryptus.aicoder.util.LanguageUtils
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory

class CodeChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val session = StorageInterface.newGlobalID()
        val language = LanguageUtils.getComputerLanguage(e)?.name ?: ""
        val filename = FileDocumentManager.getInstance().getFile(editor.document)?.name ?: return

        SessionProxyServer.agents[session] = CodeChatSocketManager(
            session = session,
            language = language,
            codeSelection = editor.caretModel.primaryCaret.selectedText ?: editor.document.text,
            filename = filename,
            api = api,
            model = AppSettingsState.instance.smartModel.chatModel(),
            storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome)
        )
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Code Chat",
            singleInput = false,
            stickyInput = true,
            loadImages = false,
            showMenubar = false
        )

        val server = AppServer.getServer(e.project)

        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(CodeChatAction::class.java)
    }
}