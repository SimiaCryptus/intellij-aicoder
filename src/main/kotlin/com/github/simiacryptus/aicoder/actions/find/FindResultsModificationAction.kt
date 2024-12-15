package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.actions.find.FindResultsModificationDialog
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfoAdapter
import com.intellij.usages.UsageView
import javax.swing.Icon

class FindResultsModificationAction(
  name: String? = "Modify Find Results",
  description: String? = "Modify files based on find results",
  icon: Icon? = null
) : BaseAction(name, description, icon) {

  override fun handle(e: AnActionEvent) {
    val project = e.project ?: return
    val usageView = e.getData(UsageView.USAGE_VIEW_KEY) ?: return
    val usages = usageView.usages.toTypedArray()
    if (usages.isEmpty()) {
      UITools.showWarning(project, "No find results selected for modification")
      return
    }
    val modificationParams = showModificationDialog(project, *usages) ?: return
    WriteCommandAction.runWriteCommandAction(project) {
      try {
        modifyUsages(project, usages, modificationParams)
      } catch (ex: Exception) {
        UITools.error(log, "Error modifying files", ex)
      }
    }
    UITools.showInfoMessage(
      project,
      "Modification complete",
      "File Modification Complete"
    )
  }

  private fun modifyUsages(project: Project, usages: Array<Usage>, params: ModificationParams) {
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    val map = usages.groupBy {
      it.location?.editor?.file
    }
    map.entries.forEach { (file, usages) ->
      for (usage in usages) {
        val file = usage.location?.editor?.file ?: return@forEach
        val document = getDocument(project, file) ?: return@forEach
        var startOffset = usage.navigationOffset
        var endOffset = usage.presentation.plainText.length + startOffset
        val prevText = document.getText(TextRange(startOffset, endOffset))
        val newText = prevText // This is where the modification might happen
        if (newText != prevText) {
          document.replaceString(startOffset, endOffset, newText)
          psiDocumentManager.commitDocument(document)
        }
      }
    }
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

  private data class ModificationParams(
    val replacementText: String
  )

}