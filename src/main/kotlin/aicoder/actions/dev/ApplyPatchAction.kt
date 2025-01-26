package aicoder.actions.dev

import aicoder.actions.BaseAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.skyenet.core.util.IterativePatchUtil
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

/**
 * Action that allows applying a patch to selected files in the IDE.
 */
class ApplyPatchAction : BaseAction(
  name = "Apply Patch",
  description = "Applies a patch to the current file"
) {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  private val log = Logger.getInstance(ApplyPatchAction::class.java)

  override fun handle(event: AnActionEvent) {
    val project = event.project ?: return
    val virtualFiles = UITools.getSelectedFiles(event) ?: return

    // Prompt user to input patch content
    val patchContent = showPatchInputDialog() ?: return
    if (patchContent.trim().isEmpty()) {
      Messages.showErrorDialog(project, "Patch content cannot be empty", "Invalid Patch")
      return
    }

    virtualFiles.forEach { virtualFile ->
      try {
        applyPatch(virtualFile, patchContent, project)
      } catch (e: Exception) {
        Messages.showErrorDialog(
          project,
          "Failed to apply patch to ${virtualFile.name}: ${e.message}",
          "Patch Application Error"
        )
      }
    }
  }

  private fun showPatchInputDialog(): String? {
    var patchContent: String? = null
    lateinit var dialogPanel: DialogPanel
    lateinit var patchContentTextArea: Cell<JBTextArea>
    dialogPanel = panel {
      row {
        textArea()
          .label("Patch Content")
          .bindText({ patchContent ?: "" }, { patchContent = it })
          .rows(10)
          .columns(50)
          .also { textArea -> patchContentTextArea = textArea }
      }
      row {
        button("Paste") {
          val clipboard = Toolkit.getDefaultToolkit().systemClipboard
          val clipboardContent = clipboard.getData(DataFlavor.stringFlavor) as? String
          log.info("Pasting clipboard content: $clipboardContent")
          patchContent = clipboardContent ?: patchContent
          patchContentTextArea.text(patchContent ?: "")
          dialogPanel.revalidate()
          dialogPanel.repaint()
          dialogPanel.apply()
        }
      }
    }
    val dialogWrapper = object : com.intellij.openapi.ui.DialogWrapper(true) {
      init {
        init()
        title = "Enter Patch Content"
      }

      override fun createCenterPanel(): DialogPanel {
        return dialogPanel
      }
    }
    dialogWrapper.showAndGet()
    return patchContent
  }

  /**
   * Applies the given patch content to a file.
   *
   * @param file The virtual file to patch
   * @param patchContent The content of the patch to apply
   * @param project The current project
   */

  private fun applyPatch(file: VirtualFile, patchContent: String, project: com.intellij.openapi.project.Project) {
    WriteCommandAction.runWriteCommandAction(project) {
      val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@runWriteCommandAction
      val newContent = IterativePatchUtil.applyPatch(psiFile.text, patchContent)
      if (newContent == psiFile.text) {
        Messages.showWarningDialog(project, "Patch made no changes to ${file.name}", "No Changes")
        return@runWriteCommandAction
      }
      psiFile.virtualFile.setBinaryContent(newContent.toByteArray())
    }
  }

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    val selectedFiles = UITools.getSelectedFiles(event)
    when {
      null == selectedFiles -> return false
      selectedFiles.size == 0 -> return false
      selectedFiles.size > 1 -> return false
      selectedFiles.first().isDirectory -> return false
      else -> return true
    }
  }

}