package aicoder.actions.dev

import aicoder.actions.BaseAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.IterativePatchUtil

/**
 * Action that allows applying a patch to selected files in the IDE.
 */

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
        return selectedFiles != null && selectedFiles.size == 1
    }

}