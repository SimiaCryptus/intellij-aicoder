package aicoder.actions.find

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageView
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.aicoder.util.psi.PsiUtil
import com.simiacryptus.diff.AddApplyFileDiffLinks

import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.session.getChildClient
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import javax.swing.Icon

class FindResultsModificationAction(
  name: String? = "Modify Find Results",
  description: String? = "Modify files based on find results",
  icon: Icon? = null
) : BaseAction(name, description, icon) {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

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
      val fileListMap = usages.groupBy { getFile(it) }
      SessionProxyServer.chats[session] = PatchApp(
        root = root.toFile(),
        modificationParams = modificationParams,
        project = event.project ?: return,
        usages = fileListMap
      )
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
          UITools.showErrorDialog(message, "Error")
        }
      }
    } catch (ex: Exception) {
      UITools.error(log, "Error modifying files", ex)
    }
  }

  private fun getFile(it: Usage) = when {
    it is UsageInfo2UsageAdapter -> {
      it.file
    }

    else -> {
      it.location?.editor?.file
    }
  }

  inner class PatchApp(
    override val root: File,
    val modificationParams: ModificationParams,
    val project: Project,
    val usages: Map<VirtualFile?, List<Usage>>,
  ) : ApplicationServer(
    applicationName = "Multi-file Patch Chat",
    path = "/patchChat",
    showMenubar = false,
  ) {
    override val singleInput = true
    override val stickyInput = false

    override fun newSession(user: User?, session: Session): SocketManager {
      val socketManager = super.newSession(user, session)
      val ui = (socketManager as ApplicationSocketManager).applicationInterface
      val task = ui.newTask()
      val api = api.getChildClient(task)
      val tabs = TabbedDisplay(task)
      usages.entries.map { (file, usages) ->
        val task = ui.newTask(false)
        tabs[file?.name ?: "Unknown"] = task.placeholder
        lateinit var fileListingMarkdown: String
        lateinit var prompt: String
        ApplicationManager.getApplication().runReadAction {
            file ?: return@runReadAction
            fileListingMarkdown =
                "## ${file.name}\n\n```${file.extension}\n${getFilteredLines(project, file, usages)}\n```\n"
            task.add(renderMarkdown(fileListingMarkdown))
            prompt = """
                    You are a code modification assistant. You will receive code files and locations where changes are needed.
                    Your task is to suggest appropriate modifications based on the replacement text provided.
                    Usage locations:
                    """.trimIndent() + usages.joinToString("\n") { "* `${it.presentation.plainText}`" } +
                    "\n\nRequested modification: " + modificationParams.replacementText + "\n\n" + patchFormatPrompt
        }
        ui.socketManager!!.pool.submit {
          val api = api.getChildClient(task)
          val response = SimpleActor(
            prompt = prompt,
            model = AppSettingsState.instance.smartModel.chatModel()
          ).answer(
            listOf(
              fileListingMarkdown
            ), api
          ).replace(Regex("""/\* L\d+ \*/"""), "")
            .replace(Regex("""/\* <<< \*/"""), "")
          AddApplyFileDiffLinks.instrumentFileDiffs(
            ui.socketManager!!,
            root = root.toPath(),
            response = response,
            handle = { newCodeMap ->
              newCodeMap.forEach { (path, newCode) ->
                task.complete("Updated $path")
              }
            },
            ui = ui,
            api = api,
            shouldAutoApply = { modificationParams.autoApply },
            defaultFile = file?.toFile?.path
          )?.apply {
            task.complete(renderMarkdown(this))
          }
        }
      }.toTypedArray().forEach { it.get() }
      return socketManager
    }

  }

  private fun getSmallestContainingEntity(psiRoot: PsiFile?, usage: Usage) =
    PsiUtil.getSmallestContainingEntity(
      element = psiRoot!!,
      selectionStart = usage.navigationOffset,
      selectionEnd = usage.presentation.plainText.length + usage.navigationOffset - 1
    )

  private fun formatLine(index: Int, line: String, isFocused: Boolean) = when {
    isFocused -> "/* L$index */ $line /* <<< */"
    else -> "/* L$index */ $line"
  }

private fun getFilteredLines(project: Project, file: VirtualFile, usages: List<Usage>): String? {
    val document =
      PsiDocumentManager.getInstance(project).getDocument(file.findPsiFile(project) ?: return null) ?: return null
    val psiRoot: PsiFile? = file.findPsiFile(project)
    val byContainer = usages.groupBy { getSmallestContainingEntity(psiRoot, it) }.entries.sortedBy { it.key?.textRange?.startOffset }.toTypedArray()
    val filteredLines = document.text.lines().mapIndexed { index: Int, line: String ->
      val lineStart = document.getLineStartOffset(index)
      val lineEnd = document.getLineEndOffset(index)
      val containers = byContainer.map { it.key }.filter { psiElement ->
        psiElement ?: return@filter false
        val textRange = psiElement.textRange
        val startOffset = textRange.startOffset
        val endOffset = textRange.endOffset
        when {
          startOffset >= lineEnd -> false
          endOffset <= lineStart -> false
          else -> true
        }
      }
      val intersectingUsages = usages.filter { usage ->
        //val plainText = usage.presentation.plainText.trim()
        val startOffset = usage.navigationOffset
        val endOffset = startOffset + 1 // (plainText.length-1)
        when {
          startOffset >= lineEnd -> false
          endOffset <= lineStart -> false
          else -> true
        }
      }
      when {
        intersectingUsages.isNotEmpty() -> formatLine(index, line, true)
        containers.isNotEmpty() -> formatLine(index, line, false)
        else -> "..."
      }
    }.joinToString("\n").replace("(?:\\.\\.\\.\n){2,}".toRegex(), "...\n")
    return filteredLines
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
        replacementText = config.replacementText ?: "",
        autoApply = config.autoApply
      )
    } else null
  }

  data class ModificationParams(
    val replacementText: String,
    val autoApply: Boolean
  )

}