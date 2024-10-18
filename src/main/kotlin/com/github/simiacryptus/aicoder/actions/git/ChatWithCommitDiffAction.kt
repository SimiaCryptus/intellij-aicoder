package com.github.simiacryptus.aicoder.actions.git

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.CodeChatSocketManager
import com.github.simiacryptus.aicoder.util.IdeaChatClient
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.changes.TextRevisionNumber
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import javax.swing.JOptionPane

class ChatWithCommitDiffAction : AnAction() {
    companion object {
        private val log = Logger.getInstance(ChatWithCommitDiffAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        log.info("Comparing selected commit with the current HEAD")
        val project = e.project ?: return
        val selectedCommit = e.getData(VcsDataKeys.VCS_REVISION_NUMBER) ?: return
        val vcsManager = ProjectLevelVcsManager.getInstance(project)
        val vcs = vcsManager.allActiveVcss.firstOrNull() ?: run {
            JOptionPane.showMessageDialog(null, "No active VCS found", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        Thread {
            try {
                val diffInfo = getChangesBetweenCommits(project, selectedCommit).ifEmpty { "No changes found" }
                openChatWithDiff(e, diffInfo)
            } catch (e: Throwable) {
                log.error("Error comparing changes", e)
                JOptionPane.showMessageDialog(null, e.message, "Error", JOptionPane.ERROR_MESSAGE)
            }
        }.start()
    }

    private fun openChatWithDiff(e: AnActionEvent, diffInfo: String) {
        val session = Session.newGlobalID()
        SessionProxyServer.agents[session] = CodeChatSocketManager(
            session = session,
            language = "diff",
            codeSelection = diffInfo,
            filename = "commit_changes.diff",
            api = IdeaChatClient.instance,
            model = AppSettingsState.instance.smartModel.chatModel(),
            storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome)
        )
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Code Chat",
            singleInput = false,
            stickyInput = true,
            loadImages = false,
            showMenubar = false
        )

        val server = AppServer.getServer(e.project)

        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    private fun getChangesBetweenCommits(project: Project, selectedCommit: VcsRevisionNumber): String {
        val commitID = (selectedCommit as TextRevisionNumber).asString()
        val changeListManager = ChangeListManager.getInstance(project)
        val changes = changeListManager.allChanges
        return changes.joinToString("\n") { change: Change ->
            buildString {
                appendLine("File: ${change.virtualFile?.path ?: "Unknown"}")
                appendLine("Type: ${change.type}")
                appendLine(getDiffForChange(project, change, selectedCommit) ?: "No diff available")
            }
        }
    }

    private fun getDiffForChange(project: Project, change: Change, selectedCommit: VcsRevisionNumber): String? {
        val file = change.virtualFile ?: return null
        val currentContent = change.afterRevision?.content ?: return null
        val selectedContent = getContentForRevision(project, file, selectedCommit) ?: return null
        return createSimpleDiff(currentContent, selectedContent)
    }

    private fun getContentForRevision(project: Project, file: VirtualFile, revisionNumber: VcsRevisionNumber): String? {
        try {
            val contentRevision = CurrentContentRevision(LocalFilePath(file.path, file.isDirectory))
            return contentRevision.content
        } catch (e: Exception) {
            log.error("Error getting content for revision", e)
            return null
        }
    }

    private fun createSimpleDiff(currentContent: String, selectedContent: String): String {
        val currentLines = currentContent.lines()
        val selectedLines = selectedContent.lines()
        val diff = StringBuilder()
        for ((index, line) in currentLines.withIndex()) {
            if (index >= selectedLines.size) {
                diff.appendLine("+ $line")
            } else if (line != selectedLines[index]) {
                diff.appendLine("- ${selectedLines[index]}")
                diff.appendLine("+ $line")
            }
        }
        if (selectedLines.size > currentLines.size) {
            for (i in currentLines.size until selectedLines.size) {
                diff.appendLine("- ${selectedLines[i]}")
            }
        }
        return diff.toString()
    }


    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null &&
                ProjectLevelVcsManager.getInstance(project).allActiveVcss.isNotEmpty()
    }

}

