package com.github.simiacryptus.aicoder.ui

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.simiacryptus.openai.GPT4Tokenizer
import kotlinx.coroutines.CoroutineScope
import java.awt.event.MouseEvent
import java.util.concurrent.Executors

class TokenCountWidgetFactory : StatusBarWidgetFactory {
    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(TokenCountWidgetFactory::class.java)
        val pool = Executors.newCachedThreadPool()
    }

    class TokenCountWidget : StatusBarWidget, StatusBarWidget.TextPresentation {

        private var tokenCount: Int = 0
        val codex = GPT4Tokenizer(false)

        override fun ID(): String {
            return "StatusBarComponent"
        }

        override fun getPresentation(): StatusBarWidget.WidgetPresentation {
            return this
        }

        override fun install(statusBar: StatusBar) {
            val connection = statusBar.project?.messageBus?.connect()
            connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    update(statusBar) {
                        codex.estimateTokenCount((event.newFile ?: event.oldFile)?.readText() ?: "")
                    }

                    val editor = FileEditorManager.getInstance(statusBar.project!!).selectedTextEditor
                    editor?.document?.addDocumentListener(object : DocumentListener {
                        override fun documentChanged(event: DocumentEvent) {
                            update(statusBar) { codex.estimateTokenCount(editor.document.text) }
                        }
                    })

                    editor?.selectionModel?.addSelectionListener(object : SelectionListener {
                        override fun selectionChanged(event: SelectionEvent) {
                            update(statusBar) {
                                event.newRanges?.map {
                                    codex.estimateTokenCount(
                                        event.editor.document.text.substring(
                                            it.startOffset,
                                            it.endOffset
                                        )
                                    )
                                }?.sum() ?: 0
                            }
                        }
                    })
                }
            })
        }

        private fun update(statusBar: StatusBar, tokens: () -> Int) {
            pool.submit {
                tokenCount = tokens()
                statusBar.updateWidget(ID())
            }
        }

        override fun dispose() {
            //connection?.disconnect()
        }


        override fun getText(): String {
            return "$tokenCount Tokens"
        }

        override fun getTooltipText(): String {
            return "Current file token count"
        }

        override fun getAlignment(): Float {
            return 0.5f
        }

        override fun getClickConsumer(): com.intellij.util.Consumer<MouseEvent>? = null


    }

    override fun getId(): String {
        return "StatusBarComponent"
    }

    override fun getDisplayName(): String {
        return "Token Counter"
    }

    override fun createWidget(project: Project, scope: CoroutineScope): StatusBarWidget {
        return TokenCountWidget()
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return TokenCountWidget()
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}
