package aicoder.actions

import aicoder.actions.agent.toFile
import aicoder.actions.chat.MultiDiffChatAction.Companion.patchEditorPrompt
import aicoder.actions.find.FindResultsModificationDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.usages.Usage
import com.intellij.usages.UsageView
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.aicoder.util.psi.PsiUtil
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
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
    override val singleInput = true
    override val stickyInput = false

    override fun newSession(user: User?, session: Session): SocketManager {
      val socketManager = super.newSession(user, session)
      val ui = (socketManager as ApplicationSocketManager).applicationInterface
      val task = ui.newTask()
      val api = api.getChildClient().apply {
        val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
        createFile.second?.apply {
          logStreams += this.outputStream().buffered()
          task.verbose("API log: <a href=\"file:///$this\">$this</a>")
        }
      }
      val tabs = TabbedDisplay(task)
      usages.groupBy { it.location?.editor?.file }.entries.forEach { (file, usages) ->
        val task = ui.newTask(false)
        tabs[if (null == file) "Unknown" else file.name] = task.placeholder
        val api = api.getChildClient().apply {
          val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
          createFile.second?.apply {
            logStreams += this.outputStream().buffered()
            task.verbose("API log: <a href=\"file:///$this\">$this</a>")
          }
        }
        fun formatLine(index: Int, line: String, isFocused: Boolean) = when {
          isFocused -> "/* L$index */ $line /* <<< */"
          else -> "/* L$index */ $line"
        }
        val document = PsiDocumentManager.getInstance(project).getDocument(file?.findPsiFile(project) ?: return@forEach) ?: return@forEach
        val psiRoot: PsiFile? = file.findPsiFile(project)
        val byContainer = usages.groupBy { getSmallestContainingEntity(psiRoot, it) }.entries.sortedBy { it.key?.textRange?.startOffset }.toTypedArray()
        val lines = document.text.lines()
        val filteredLines = lines.mapIndexed { index: Int, line: String ->
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
            "..."
          }
        }.joinToString("\n").replace("(?:\\.\\.\\.\n){2,}".toRegex(), "...\n")
        val fileListingMarkdown = "## ${file.name}\n\n```${file.extension}\n$filteredLines\n```\n"
        task.add(renderMarkdown(fileListingMarkdown))
        val prompt = """
            You are a code modification assistant. You will receive code files and locations where changes are needed.
            Your task is to suggest appropriate modifications based on the replacement text provided.
            Usage locations:
            """.trimIndent() + usages.joinToString("\n") { "* `${it.presentation.plainText}`" } +
            "\n\nRequested modification: " + modificationParams.replacementText + "\n\n" + patchEditorPrompt
        ui.socketManager?.addApplyFileDiffLinks(
          root = root.toPath(),
          response = SimpleActor(
            prompt = prompt,
            model = AppSettingsState.instance.smartModel.chatModel()
          ).answer(
            listOf(
              fileListingMarkdown
            ), api
          ).replace(Regex("""/\* L\d+ \*/"""), "").replace(Regex("""/\* <<< \*/"""), ""),
          handle = { newCodeMap ->
            newCodeMap.forEach { (path, newCode) ->
              task.complete("Updated $path")
            }
          },
          ui = ui,
          api = api,
          shouldAutoApply = { modificationParams.autoApply },
          defaultFile = file.toFile.path
        )?.apply {
          task.complete(renderMarkdown(this))
        }
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