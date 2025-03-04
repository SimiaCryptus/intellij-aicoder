package com.simiacryptus.aicoder.ui

import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.projectView.impl.ProjectViewListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.invokeLater
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.*
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.simiacryptus.jopenai.util.GPT4Tokenizer
import kotlinx.coroutines.CoroutineScope
import java.awt.event.MouseEvent
import java.io.File
import java.util.*
import java.util.concurrent.*
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.TreePath
import kotlin.io.path.isSymbolicLink

class TokenCountWidgetFactory : StatusBarWidgetFactory {
  companion object {
    val log = com.intellij.openapi.diagnostic.Logger.getInstance(TokenCountWidgetFactory::class.java)
    private val messages = ResourceBundle.getBundle("messages.TokenCountWidget")
    private fun getMessage(key: String, vararg args: Any): String =
      String.format(messages.getString(key), *args)
    
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
    
    @Volatile
    private var isCalculating: Boolean = false
    private var animationCounter: Int = 0
    
    var updateFuture: Future<*>? = null
    private fun update(statusBar: StatusBar, tokens: () -> Int) {
      // Remove any pending work (no coroutine cancellation required now)
      isCalculating = true
      statusBar.updateWidget(ID())
      workQueue.clear()
      updateFuture?.cancel(true)
      updateFuture = pool.submit {
        try {
          tokenCount = tokens()
        } catch (e: Exception) {
          e.printStackTrace()
          tokenCount = 0
        } finally {
          isCalculating = false
          statusBar.project?.let {
            invokeLater(it) {
              statusBar.updateWidget(ID())
            }
          }
        }
      }
    }
    
    override fun ID(): String {
      return "TokenCountWidget"
    }
    
    override fun getPresentation() = this
    
    fun resolve(path: Array<String>, candidates: List<VirtualFile>): VirtualFile? {
      if (candidates.isEmpty()) return null
      val pathString = path.joinToString(File.separator)
      val exactMatches = candidates.filter { candidate ->
        candidate.path.endsWith(pathString) ||
            candidate.path.contains(path.last())
      }
      if (exactMatches.isNotEmpty()) {
        return exactMatches.minByOrNull {
          stringDistance(pathString, it.path)
        }
      }
      val sortedCandidates = candidates.sortedBy {
        stringDistance(pathString, it.path)
      }
      return sortedCandidates.first()
    }
    
    fun update(statusBar: StatusBar, current: AbstractProjectViewPane) {
      try {
        val paths: Array<TreePath>? = current.selectionPaths
        update(statusBar) {
          paths?.flatMap { path ->
            val openProjects = ProjectManager.getInstance().openProjects
            val pathComponents = path.path.map { it.toString() }
            val fullPathString = pathComponents.joinToString("/")
            val matchingRoots = openProjects.flatMap { project ->
              val baseVirtualFile = listOfNotNull(
                project.guessProjectDir()
              ) + project.modules.mapNotNull { it.guessModuleDir() }
              val exactPathMatches = baseVirtualFile.flatMap { it.listChildrenRecursively { it.path.endsWith(fullPathString) } }
              if (exactPathMatches.isNotEmpty()) {
                exactPathMatches
              } else {
                val lastComponents = pathComponents.takeLast(minOf(3, pathComponents.size))
                baseVirtualFile.flatMap { root ->
                  lastComponents.indices.map { i ->
                    lastComponents.drop(i).joinToString("/")
                  }.mapNotNull { relativePath ->
                    root.findFileByRelativePath(relativePath)
                  }
                }.ifEmpty {
                  baseVirtualFile.flatMap { root ->
                    root.listChildrenRecursively { file ->
                      matches(file, path.lastPathComponent as? javax.swing.tree.TreeNode)
                    }
                  }
                }
              }
            }
            val pathArray = path.path.map { it.toString() }.toTypedArray()
            val virtualFile = if (matchingRoots.size == 1) {
              matchingRoots.first()
            } else {
              val scoredFiles = matchingRoots.map { file ->
                val filePath = file.path
                val pathScore = pathComponents.foldIndexed(0) { index, score, component ->
                  if (filePath.contains("/$component/") || filePath.endsWith("/$component")) {
                    score + (pathComponents.size - index)
                  } else {
                    score
                  }
                }
                file to pathScore
              }
              scoredFiles.maxByOrNull { it.second }?.first ?: resolve(pathArray, matchingRoots)
            }
            
            if (virtualFile?.isFile == true) {
              listOf(virtualFile.readText() to virtualFile.path)
            } else {
              virtualFile?.listChildrenRecursively { it.isFile }?.map { it.readText() to it.path } ?: emptyList()
            }
          }?.let { pairs ->
            var totalCount = 0
            val details = buildString {
              //Language=HTML
              append("<html><body style='font-family: Arial, sans-serif;'>")
              append("<table border='0' cellpadding='3' style='border-collapse: collapse; width: 100%; max-width: 800px; margin: 20px auto;'>")
              append("<tr style='font-weight: bold;'><th style='padding: 10px; text-align: left; border-bottom: 2px solid #ddd;'>File</th><th style='padding: 10px; text-align: left; border-bottom: 2px solid #ddd;'>Tokens</th></tr>")
              var displayedCount = 0
              for ((content, name) in pairs.sortedBy { it.second }) {
                if (content != null) {
                  val fileTokens = codex.estimateTokenCount(content)
                  totalCount += fileTokens
                  append(
                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 8px;'>$name</td><td style='padding: 8px;'>${
                      tokenCountToString(
                        fileTokens
                      )
                    }</td></tr>"
                  )
                  displayedCount += 1
                }
                if (displayedCount > 10) {
                  append("<tr><td colspan='2' style='text-align: center; padding: 8px; font-style: italic; color: #666;'>...</td></tr>")
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
      } catch (e: Exception) {
        log.error("Error updating token count", e)
        tokenCount = 0
      }
    }
    
    private fun matches(file: VirtualFile, node: javax.swing.tree.TreeNode?) : Boolean = when {
      file.name != node.toString() -> false
      node is javax.swing.tree.TreeNode && node.parent != null -> matches(file.parent, node.parent)
      else -> true
    }
    
    override fun install(statusBar: StatusBar) {
      val connection = statusBar.project?.messageBus?.connect()
      connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
        override fun selectionChanged(event: FileEditorManagerEvent) {
          update(statusBar) {
            val file = event.newFile ?: event.oldFile
            val count = if (file != null) {
              val text = file.readText()
              val tokenCount = codex.estimateTokenCount(text)
              val projectRelativePath = statusBar.project?.let { project ->
                val projectPath = project.basePath
                if (projectPath != null && file.path.startsWith(projectPath)) {
                  file.path.substring(projectPath.length).removePrefix("/")
                } else {
                  file.path
                }
              } ?: file.path
              updateTooltip(projectRelativePath, tokenCount)
              tokenCount
            } else {
              updateTooltip(null, 0)
              0
            }
            count
          }
          
          val editor = FileEditorManager.getInstance(statusBar.project!!).selectedTextEditor
          editor?.document?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
              update(statusBar) {
                val count = codex.estimateTokenCount(editor.document.text)
                // Use full path instead of just name for better identification
                updateTooltip(editor.virtualFile.path, count)
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
                  updateTooltip(editor.virtualFile.path, estimateTokenCount)
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
          val currentSelectionListener = TreeSelectionListener { update(statusBar, current) }
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
          val treeSelectionListener = TreeSelectionListener { update(statusBar, currentPane) }
          currentPane.tree?.addTreeSelectionListener(treeSelectionListener)
          Disposer.register(this) {
            currentPane.tree?.removeTreeSelectionListener(treeSelectionListener)
          }
        }
      }
    }
    
    private fun updateTooltip(source: String?, count: Int) {
      tooltipDetails = when {
        source == null -> "No file selected"
        source.startsWith("<html>") -> source
        else -> {
          // Improve tooltip formatting for better readability of file paths
          val displayPath = if (source.length > 60) {
            // For long paths, show the first and last parts
            val parts = source.split("/")
            if (parts.size > 4) {
              val first = parts.take(2).joinToString("/")
              val last = parts.takeLast(2).joinToString("/")
              "$first/...//$last"
            } else {
              source
            }
          } else {
            source
          }
          "<html><body>File: $displayPath<br>Tokens: $count</body></html>"
        }
      }
    }
    
    override fun getText(): String {
      return if (isCalculating) {
        val dots = ".".repeat((animationCounter % 3) + 1)
        getMessage("status.calculating", dots)
      } else {
        tokenCountToString(tokenCount)
      }
    }
    
    override fun getTooltipText(): String {
      return if (isCalculating) {
        val frames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
        val spinnerChar = frames[animationCounter % frames.size]
        animationCounter++
        if (animationCounter >= frames.size) animationCounter = 0
        getMessage("tooltip.calculating_html", spinnerChar)
      } else {
        animationCounter = 0
        tooltipDetails
      }
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
          count == 0 -> getMessage("count.zero")
          count == 1 -> getMessage("count.one")
          count >= 1_000_000 -> getMessage("count.millions", count / 1_000_000)
          count >= 10000 -> getMessage("count.thousands", count / 1000)
          count < 0 -> getMessage("count.characters", -count)
          else -> getMessage("count.normal", count)
        }
      }
    }
  }
  
  override fun getId(): String {
    return "TokenCountWidget"
  }
  
  override fun getDisplayName(): String {
    return getMessage("widget.display_name")
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
      this.toNioPath().isSymbolicLink() -> return
      else -> {
        if (filter(this)) result.add(this)
        children.forEach { it.listChildrenRecursively() }
      }
    }
  }
  listChildrenRecursively()
  return result
}