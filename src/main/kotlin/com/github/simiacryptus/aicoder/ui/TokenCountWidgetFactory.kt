package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.util.findRecursively
import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.projectView.impl.ProjectViewListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.simiacryptus.jopenai.util.GPT4Tokenizer
import kotlinx.coroutines.CoroutineScope
import java.awt.event.MouseEvent
import java.io.File
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener

class TokenCountWidgetFactory : StatusBarWidgetFactory {
    companion object {
        val workQueue = LinkedBlockingDeque<Runnable>()
        val pool = ThreadPoolExecutor(
            /* corePoolSize = */ 1, /* maximumPoolSize = */ 1,
            /* keepAliveTime = */ 60L, /* unit = */ TimeUnit.SECONDS,
            /* workQueue = */ workQueue
        )
    }

    class TokenCountWidget : StatusBarWidget, StatusBarWidget.TextPresentation {

        private var tokenCount: Int = 0
        val codex = GPT4Tokenizer(false)
        private var projectViewListener: ProjectViewListener? = null

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
                                val newTokens = event.newRanges?.map {
                                    codex.estimateTokenCount(
                                        event.editor.document.text.substring(
                                            it.startOffset,
                                            it.endOffset
                                        )
                                    )
                                }?.sum() ?: 0
                                newTokens
                            }
                        }
                    })
                }
            })
            fun stringDistance(s1: String, s2: String): Int {
                val m = s1.length
                val n = s2.length
                val dp = Array(m + 1) { IntArray(n + 1) }
                for (i in 0..m) {
                    for (j in 0..n) {
                        when {
                            i == 0 -> dp[i][j] = j
                            j == 0 -> dp[i][j] = i
                            s1[i - 1] == s2[j - 1] -> dp[i][j] = dp[i - 1][j - 1]
                            else -> dp[i][j] = 1 + minOf(dp[i - 1][j - 1], dp[i - 1][j], dp[i][j - 1])
                        }
                    }
                }
                return dp[m][n]
            }

            fun update(current: AbstractProjectViewPane, e: TreeSelectionEvent?) {
                update(statusBar) {
                    val paths = current.selectionPaths
                    val text = paths?.mapNotNull { path ->
                        val node = path.lastPathComponent
                        fun resolve(path: Array<String>, files: List<VirtualFile>): VirtualFile? {
                            if (files.isEmpty()) return null
                            if (files.size == 1) return files.first()
                            val sortedBy = files.sortedBy { stringDistance(path.joinToString(File.separator), it.toNioPath().toFile().absolutePath) }
                            return sortedBy.first()
                        }
                        ProjectManager.getInstance().openProjects.flatMap {
                            it.baseDir?.findRecursively { file -> file.name.contains(node.toString()) } ?: emptyList()
                        }.let { files ->
                            val resolve = resolve(path?.path?.map { it.toString() }?.toTypedArray() ?: emptyArray(), files)
                            if ((resolve?.length ?: 0) > 1024 * 1024) return@mapNotNull null
                            resolve?.readText()
                        }
                    }?.joinToString("\n") ?: ""
                    VirtualFileManager.getInstance()
                    codex.estimateTokenCount(text)
                }
            }
            connection?.subscribe(ProjectViewListener.TOPIC, object : ProjectViewListener {
                var previousSelectionListener: TreeSelectionListener? = null
                override fun paneShown(current: AbstractProjectViewPane, previous: AbstractProjectViewPane?) {
                    val currentSelectionListener = object : TreeSelectionListener {
                        override fun valueChanged(e: TreeSelectionEvent?) {
                            update(current, e)
                        }
                    }
                    current.tree.addTreeSelectionListener(currentSelectionListener)
                    previousSelectionListener?.let { previous?.tree?.removeTreeSelectionListener(it) }
                    previousSelectionListener = currentSelectionListener
                    super.paneShown(current, previous)
                }
            })
            // Initialize ProjectView listener on startup
            statusBar.project?.let { project ->
                val projectView = com.intellij.ide.projectView.impl.ProjectViewImpl.getInstance(project)
                val currentPane = projectView.currentProjectViewPane
                if (currentPane != null) {
                    val listener = object : TreeSelectionListener {
                        override fun valueChanged(e: TreeSelectionEvent?) {
                            update(currentPane, e)
                        }
                    }
                    currentPane.tree.addTreeSelectionListener(listener)
                }
            }
        }

        private fun update(statusBar: StatusBar, tokens: () -> Int) {
            workQueue.clear()
            pool.submit {
                tokenCount = tokens()
                statusBar.updateWidget(ID())
            }
        }

        override fun dispose() {
            //connection?.disconnect()
        }


        override fun getText(): String {
            return when {
                tokenCount == 0 -> "0 Tokens"
                tokenCount == 1 -> "1 Token"
                tokenCount >= 1000000 -> "${tokenCount / 1000000}M Tokens"
                tokenCount >= 10000 -> "${tokenCount / 1000}K Tokens"
                else -> "$tokenCount Tokens"
            }
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