package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import com.github.simiacryptus.aicoder.util.CodeChatSocketManager
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.diff.addApplyDiffLinks
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class DiffChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/diffChat"

    override fun handle(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val session = StorageInterface.newGlobalID()
        val language = ComputerLanguage.getComputerLanguage(e)?.name ?: ""
        val document = editor.document
        val filename = FileDocumentManager.getInstance().getFile(document)?.name ?: return
        val primaryCaret = editor.caretModel.primaryCaret
        val rawText: String
        val selectionStart: Int
        var selectionEnd: Int
        val selectedText = primaryCaret.selectedText
        if (null != selectedText) {
            rawText = selectedText
            selectionStart = primaryCaret.selectionStart
            selectionEnd = primaryCaret.selectionEnd
        } else {
            rawText = document.text
            selectionStart = 0
            selectionEnd = rawText.length
        }
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Code Chat",
            singleInput = false,
            stickyInput = true,
            loadImages = false,
            showMenubar = false
        )
        SessionProxyServer.agents[session] = object : CodeChatSocketManager(
            session = session,
            language = language,
            codeSelection = rawText,
            filename = filename,
            api = api,
            model = AppSettingsState.instance.smartModel.chatModel(),
            storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome)
        ) {
            override val systemPrompt: String
                @Language("Markdown")
                get() = super.systemPrompt + """
          |Please provide code modifications in the following diff format within triple-backtick diff code blocks. Each diff block should be preceded by a header that identifies the file being modified.
          |
          |The diff format rules are as follows:
          |- Use '-' at the beginning of a line to indicate a deletion.
          |- Use '+' at the beginning of a line to indicate an addition.
          |- Include 2 lines of context before and after every change to help identify the location of the change.
          |- If a line is part of the original code and hasn't been modified, simply include it without '+' or '-'.
          |- Lines starting with "@@" or "---" or "+++" are treated as headers and are ignored.
          |
          |Example:
          |
          |Here are the patches:
          |
          |### src/utils/exampleUtils.js
          |```diff
          | // Utility functions for example feature
          | const b = 2;
          | function exampleFunction() {
          |-   return b + 1;
          |+   return b + 2;
          | }
          |```
          |
          |### tests/exampleUtils.test.js
          |```diff
          | // Unit tests for exampleUtils
          | const assert = require('assert');
          | const { exampleFunction } = require('../src/utils/exampleUtils');
          | 
          | describe('exampleFunction', () => {
          |-   it('should return 3', () => {
          |+   it('should return 4', () => {
          |     assert.equal(exampleFunction(), 3);
          |   });
          | });
          |```

        """.trimMargin()

            val ui by lazy { ApplicationInterface(this) }
            override fun renderResponse(response: String, task: SessionTask) = """<div>${
                renderMarkdown(
                    addApplyDiffLinks(
                        code = {
                            editor.document.getText(TextRange(selectionStart, selectionEnd))
                        },
                        response = response,
                        handle = { newCode: String ->
                            WriteCommandAction.runWriteCommandAction(e.project) {
                                selectionEnd = selectionStart + newCode.length
                                document.replaceString(selectionStart, selectionStart + rawText.length, newCode)
                            }
                        },
                        task = task,
                        ui = ui
                    )
                )
            }</div>"""
        }

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
        private val log = LoggerFactory.getLogger(DiffChatAction::class.java)
    }
}