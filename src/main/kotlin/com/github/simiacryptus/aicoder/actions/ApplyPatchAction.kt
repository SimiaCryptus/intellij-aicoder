package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.util.UITools
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

    override fun handle(event: AnActionEvent) {
        val project = event.project ?: return
        val virtualFile = UITools.getSelectedFile(event) ?: return

        // Prompt user to input patch content
        val patchContent = Messages.showMultilineInputDialog(
            project,
            "Enter the patch content:",
            "Input Patch",
            "",
            null,
            null
        ) ?: return

        applyPatch(virtualFile, patchContent, project)
    }

    private fun applyPatch(file: VirtualFile, patchContent: String, project: com.intellij.openapi.project.Project) {
        WriteCommandAction.runWriteCommandAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@runWriteCommandAction
            val newContent = IterativePatchUtil.applyPatch(psiFile.text, patchContent)
            psiFile.virtualFile.setBinaryContent(newContent.toByteArray())
        }
    }
}