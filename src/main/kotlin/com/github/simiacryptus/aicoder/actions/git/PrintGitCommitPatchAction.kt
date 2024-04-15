package com.github.simiacryptus.aicoder.actions.git

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.history.VcsRevisionNumber

class PrintGitCommitPatchAction : AnAction("Print Git Commit Patch") {

    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.getData(CommonDataKeys.PROJECT)
        val revision: VcsRevisionNumber? = event.getData(VcsDataKeys.VCS_REVISION_NUMBER)
        val filePaths = event.getData(VcsDataKeys.CHANGES) ?: return

        if (project == null || revision == null) {
            Messages.showErrorDialog(project, "No commit selected.", "Error")
            return
        }

        val changes = filePaths.mapNotNull { it as? Change }
        val patches = changes.mapNotNull { change ->
            val changeListManager = ChangeListManager.getInstance(project)
            changeListManager.getChangeList(change)?.changes?.firstOrNull()?.beforeRevision?.content
        }

        val patchText = patches.joinToString(separator = "\n\n", prefix = "Patch for Revision: $revision\n\n")
        Messages.showInfoMessage(project, patchText, "Git Commit Patch")
    }

    override fun update(event: AnActionEvent) {
        // Enable action only if a project is open and a revision is selected
        val project: Project? = event.getData(CommonDataKeys.PROJECT)
        val revision: VcsRevisionNumber? = event.getData(VcsDataKeys.VCS_REVISION_NUMBER)
        event.presentation.isEnabledAndVisible = project != null && revision != null
    }
}
