package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.simiacryptus.diff.IterativePatchUtil

class ApplyPatchAction : BaseAction(
    name = "Apply Patch",
    description = "Applies a patch to the current file"
) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(event: AnActionEvent) {
        val project = event.project ?: return
        val virtualFiles = UITools.getSelectedFiles(event) ?: return

        // Prompt user to input patch content
        val patchContent = Messages.showMultilineInputDialog(
            project,
            "Enter the patch content:",
            "Input Patch",
            "",
            null,
            null
        ) ?: return
        virtualFiles.forEach { virtualFile ->
            applyPatch(virtualFile, patchContent, project)
        }
    }

    private fun applyPatch(file: VirtualFile, patchContent: String, project: com.intellij.openapi.project.Project) {
        WriteCommandAction.runWriteCommandAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@runWriteCommandAction
            val newContent = IterativePatchUtil.applyPatch(psiFile.text, patchContent)
            psiFile.virtualFile.setBinaryContent(newContent.toByteArray())
        }
    }

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    UITools.getSelectedFiles(event).let {
      when (it.size) {
        0 -> null
        1 -> it
        else -> null
      }
    } ?: return false
    return true
  }

}