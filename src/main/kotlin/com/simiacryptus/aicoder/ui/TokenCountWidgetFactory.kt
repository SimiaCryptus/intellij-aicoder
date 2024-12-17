package com.simiacryptus.aicoder.ui

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
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.simiacryptus.diff.FileValidationUtils.Companion.isGitignore
import com.simiacryptus.diff.FileValidationUtils.Companion.isLLMIncludableFile
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
        private var tooltipDetails: String = "Current file token count"

        override fun ID(): String {
            return "StatusBarComponent"
        }

        override fun getPresentation(): StatusBarWidget.WidgetPresentation {
            return this
        }

        fun resolve(path: Array<String>, candidates: List<VirtualFile>): VirtualFile? {
            if (candidates.isEmpty()) return null
            val sortedCandidates = candidates.sortedBy {
                stringDistance(path.joinToString(File.separator), it.toNioPath().toFile().absolutePath)
            }
            return sortedCandidates.first()
        }

        fun update(statusBar: StatusBar, current: AbstractProjectViewPane) {
            val paths = current.selectionPaths
            val pathChildren = paths?.associate { path ->
                path to current.tree.model.getChildCount(path.lastPathComponent)
            } ?: emptyMap()
            update(statusBar) {
                paths?.flatMap { path ->
                    val childCount = pathChildren[path] ?: 0
                    if (0 == childCount) {
                        /* File */
                        val node = path.lastPathComponent
                        val virtualFile = ProjectManager.getInstance().openProjects.flatMap {
                            it.baseDir?.listChildrenRecursively { file ->
                                file.isFile && file.name.contains(node.toString())
                            } ?: emptyList()
                        }.let { files ->
                            resolve(
                                path?.path?.map { it.toString() }?.toTypedArray<String>() ?: emptyArray<String>(),
                                files
                            )
                        }
                        listOf(virtualFile?.readText() to node.toString())
                    } else {
                        /* Directory */
                        val node = path.lastPathComponent
                        val virtualFile = ProjectManager.getInstance().openProjects.flatMap {
                            val baseDir = it.baseDir
                            baseDir?.listChildrenRecursively { file ->
                                file.isDirectory && file.name.contains(node.toString())
                            } ?: emptyList()
                        }.let { files ->
                            resolve(
                                path?.path?.map { it.toString() }?.toTypedArray<String>() ?: emptyArray<String>(),
                                files
                            )
                        }
                        val pairs: List<Pair<String?, String>> = virtualFile?.listChildrenRecursively { file ->
                            file.isFile && isLLMIncludableFile(file.toNioPath().toFile())
                        }?.map { file ->
                            file.readText() to file.name
                        }?.toList() ?: emptyList()
                        pairs
                    }
                }?.let { pairs ->
                    val tokenCountCache = mutableMapOf<String, Int>()
                    fun getCachedTokenCount(text: String): Int = tokenCountCache.getOrPut(text) { codex.estimateTokenCount(text) }
                    val totalCount = pairs.sumBy { (content, _) -> content?.let { getCachedTokenCount(it) } ?: 0 }
                    val details = buildString {
                        //Language=HTML
                        append("<html><body style='font-family: Arial, sans-serif;'>")
                        append("<table border='0' cellpadding='3' style='border-collapse: collapse; width: 100%; max-width: 800px; margin: 20px auto;'>")
                        append("<tr style='font-weight: bold;'><th style='padding: 10px; text-align: left; border-bottom: 2px solid #ddd;'>File</th><th style='padding: 10px; text-align: left; border-bottom: 2px solid #ddd;'>Tokens</th></tr>")
                        var totalCount = 0
                        for ((content, name) in pairs.sortedBy {
                            -(it.first?.length ?: 0)
                        }) {
                            if (content != null) {
                                val fileTokens = getCachedTokenCount(content)
                                append(
                                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 8px;'>$name</td><td style='padding: 8px;'>${
                                        tokenCountToString(
                                            fileTokens
                                        )
                                    }</td></tr>"
                                )
                                totalCount += 1
                            }
                            if (totalCount > 10) {
                                append("<tr><td colspan='3' style='text-align: center; padding: 8px; font-style: italic; color: #666;'>...</td></tr>")
                                break
                            }
                        }
                        append("</table>")
                        append("</body></html>")
                    }
                    updateTooltip(details, totalCount)
                    totalCount
                } ?: 0
            }
        }

        override fun install(statusBar: StatusBar) {
            val connection = statusBar.project?.messageBus?.connect()
            connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    update(statusBar) {
                        codex.estimateTokenCount((event.newFile ?: event.oldFile)?.readText() ?: "")
                        updateTooltip(event.newFile?.name ?: event.oldFile?.name, tokenCount)
                        tokenCount
                    }

                    val editor = FileEditorManager.getInstance(statusBar.project!!).selectedTextEditor
                    editor?.document?.addDocumentListener(object : DocumentListener {
                        override fun documentChanged(event: DocumentEvent) {
                            update(statusBar) {
                                val count = codex.estimateTokenCount(editor.document.text)
                                updateTooltip(editor.virtualFile.name, count)
                                count
                            }
                        }
                    })

                    editor?.selectionModel?.addSelectionListener(object : SelectionListener {
                        override fun selectionChanged(event: SelectionEvent) {
                            update(statusBar) {
                                val newTokens = event.newRanges?.sumOf {
                                    val estimateTokenCount = codex.estimateTokenCount(
                                        event.editor.document.text.substring(
                                            it.startOffset,
                                            it.endOffset
                                        )
                                    )
                                    updateTooltip(editor.virtualFile.name, estimateTokenCount)
                                    estimateTokenCount
                                } ?: 0
                                newTokens
                            }
                        }
                    })
                }
            })
            connection?.subscribe(ProjectViewListener.TOPIC, object : ProjectViewListener {
                var previousSelectionListener: TreeSelectionListener? = null
                override fun paneShown(current: AbstractProjectViewPane, previous: AbstractProjectViewPane?) {
                    val currentSelectionListener = object : TreeSelectionListener {
                        override fun valueChanged(e: TreeSelectionEvent?) {
                            update(statusBar, current)
                        }
                    }
                    current.tree.addTreeSelectionListener(currentSelectionListener)
                    previousSelectionListener?.let { previous?.tree?.removeTreeSelectionListener(it) }
                    previousSelectionListener = currentSelectionListener
                    super.paneShown(current, previous)
                }
            })
            statusBar.project?.let { project ->
                val projectView = com.intellij.ide.projectView.impl.ProjectViewImpl.getInstance(project)
                val currentPane = projectView.currentProjectViewPane
                if (currentPane != null) {
                    val listener = object : TreeSelectionListener {
                        override fun valueChanged(e: TreeSelectionEvent?) {
                            update(statusBar, currentPane)
                        }
                    }
                    currentPane.tree.addTreeSelectionListener(listener)
                }
            }
        }

        private fun updateTooltip(source: String?, count: Int) {
            tooltipDetails = when {
                source == null -> "No file selected"
                source.startsWith("<html>") -> source
                else -> "<html><body>File: $source<br>Tokens: $count</body></html>"
            }
        }

        private fun update(statusBar: StatusBar, tokens: () -> Int) {
            workQueue.clear()
            pool.submit {
                val text = statusBar.project?.let {
                    FileEditorManager.getInstance(it).selectedTextEditor?.document?.text
                } ?: ""
                tokenCount = if (text.length > 1024 * 1024) {
                    -text.length  // Using negative value to indicate character count
                } else {
                    tokens()
                }
                statusBar.updateWidget(ID())
            }
        }

        override fun dispose() {
            //connection?.disconnect()
        }


        override fun getText() = tokenCountToString(tokenCount)

        override fun getTooltipText(): String {
            return tooltipDetails
        }

        override fun getAlignment(): Float {
            return 0.5f
        }

        override fun getClickConsumer(): com.intellij.util.Consumer<MouseEvent>? = null

        companion object {
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

            fun tokenCountToString(count: Int): String {
                return when {
                    count == 0 -> "0 Tokens"
                    count == 1 -> "1 Token"
                    count < 0 -> "${-count} Chars"  // Handle character count case
                    count >= 1000000 -> "${count / 1000000}M Tokens"
                    count >= 10000 -> "${count / 1000}K Tokens"
                    else -> "$count Tokens"
                }
            }
        }


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

private fun VirtualFile.listChildrenRecursively(filter: (VirtualFile) -> Boolean): List<VirtualFile> {
    val result = mutableListOf<VirtualFile>()
    fun VirtualFile.listChildrenRecursively() {
        when {
            isGitignore(this.toNioPath()) -> return
            name.startsWith(".") -> return
            else -> {
                if (filter(this)) result.add(this)
                children.forEach { it.listChildrenRecursively() }
            }
        }
    }
    listChildrenRecursively()
    return result
}
