package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.find.FindResultsModificationDialog
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.actions.generic.toFile
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.usages.Usage
import com.intellij.usages.UsageView
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import javax.swing.Icon

class FindResultsModificationAction(
  name: String? = "Modify Find Results",
  description: String? = "Modify files based on find results",
  icon: Icon? = null
) : BaseAction(name, description, icon) {

  override fun handle(event: AnActionEvent) {
    val folder = UITools.getSelectedFolder(event)
    val root: Path = if (null != folder) {
      folder.toFile.toPath()
    } else {
      getModuleRootForFile(
        UITools.getSelectedFile(event)?.parent?.toFile
          ?: throw RuntimeException("No file or folder selected")
      ).toPath()
    }


    val project = event.project ?: return
    val usageView = event.getData(UsageView.USAGE_VIEW_KEY) ?: return
    val usages = usageView.usages.toTypedArray()
    if (usages.isEmpty()) {
      UITools.showWarning(project, "No find results selected for modification")
      return
    }
    val modificationParams = showModificationDialog(project, *usages) ?: return
    try {
      val session = Session.newGlobalID()
      SessionProxyServer.metadataStorage.setSessionName(
        null,
        session,
        "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
      )
      SessionProxyServer.chats[session] = PatchApp(root.toFile(), modificationParams, event.project ?: return, usages)
      ApplicationServer.appInfoMap[session] = AppInfoData(
        applicationName = "Code Chat",
        singleInput = true,
        stickyInput = false,
        loadImages = false,
        showMenubar = false
      )
      val server = AppServer.getServer(event.project)
      UITools.runAsync(event.project, "Opening Browser", true) { progress ->
        Thread.sleep(500)
        try {
          val uri = server.server.uri.resolve("/#$session")
          log.info("Opening browser to $uri")
          browse(uri)
        } catch (e: Throwable) {
          val message = "Failed to open browser: ${e.message}"
          log.error(message, e)
          UITools.showErrorDialog(event.project, message, "Error")
        }
      }
    } catch (ex: Exception) {
      UITools.error(log, "Error modifying files", ex)
    }
  }

  inner class PatchApp(
    override val root: File,
    val modificationParams: ModificationParams,
    val project: Project,
    val usages: Array<Usage>,
  ) : ApplicationServer(
    applicationName = "Multi-file Patch Chat",
    path = "/patchChat",
    showMenubar = false,
  ) {
    override fun newSession(user: User?, session: Session): SocketManager {
      val socketManager = super.newSession(user, session)
      val ui = (socketManager as ApplicationSocketManager).applicationInterface
      val task = ui.newTask()
      val tabs = TabbedDisplay(task)
      usages.groupBy { it.location?.editor?.file }.entries.forEach { (file, usages) ->
        val task = ui.newTask(false)
        tabs[if (null == file) "Unknown" else file.name] = task.placeholder
        fun formatLine(index: Int, line: String, isFocused: Boolean) = when {
          isFocused -> "/* L$index */ $line /* <<< */"
          else -> "/* L$index */ $line"
        }
        val document = getDocument(project, file ?: return@forEach) ?: return@forEach
        val psiRoot: PsiFile? = file.findPsiFile(project)
        val byContainer = usages.groupBy { getSmallestContainingEntity(psiRoot, it) }.entries.sortedBy { it.key?.textRange?.startOffset }.toTypedArray()
        val lines = document.text.lines()
        val filteredLines = lines.mapIndexedNotNull { index: Int, line: String ->
          val lineStart = lines.subList(0, index).joinToString("\n").length
          val lineEnd = lineStart + line.length
          val containers = byContainer.map { it.key }.filter {
            val textRange = it?.textRange ?: return@filter false
            textRange.startOffset <= lineEnd && textRange.endOffset >= lineStart
          }
          val intersectingUsages = usages.filter {
            val startOffset = it.navigationOffset ?: return@filter false
            val endOffset = startOffset + it.presentation.plainText.length
            startOffset <= lineEnd && endOffset >= lineStart
          }
          if (intersectingUsages.isNotEmpty()) {
            formatLine(index, line, true)
          } else if (containers.isNotEmpty()) {
            formatLine(index, line, false)
          } else {
            null
          }
        }.joinToString("\n")
        task.add(MarkdownUtil.renderMarkdown("## ${file.name}\n\n```${file.extension}\n$filteredLines\n```\n"))
        // TODO: Ask simpleagent for change, and instrument diffs
      }
      return socketManager
    }

    private fun getSmallestContainingEntity(psiRoot: PsiFile?, usage: Usage) =
      PsiUtil.getSmallestContainingEntity(
        element = psiRoot!!,
        selectionStart = usage.navigationOffset,
        selectionEnd = usage.presentation.plainText.length + usage.navigationOffset - 1
      )
  }

  private fun getDocument(project: Project, file: VirtualFile): Document? {
    return PsiDocumentManager.getInstance(project).getDocument(file.findPsiFile(project) ?: return null)
  }

  override fun isEnabled(event: AnActionEvent): Boolean {
    val usageView = event.getData(UsageView.USAGE_VIEW_KEY)
    return usageView != null && usageView.usages.isNotEmpty()
  }

  private fun showModificationDialog(project: Project, vararg usages: Usage): ModificationParams? {
    val dialog = FindResultsModificationDialog(project, usages.size)
    val config = dialog.showAndGetConfig()
    return if (config != null) {
      ModificationParams(
        config.replacementText ?: ""
      )
    } else null
  }

  data class ModificationParams(
    val replacementText: String
  )

}