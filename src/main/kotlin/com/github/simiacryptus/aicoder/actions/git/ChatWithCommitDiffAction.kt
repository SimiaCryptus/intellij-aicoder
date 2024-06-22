package com.github.simiacryptus.aicoder.actions.git

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.CodeChatSocketManager
import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.TextRevisionNumber
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import git4idea.GitVcs
import git4idea.commands.Git
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.awt.Desktop

class ChatWithCommitDiffAction : AnAction() {
    companion object {
        private val log = Logger.getInstance(ChatWithCommitDiffAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        log.info("Comparing selected commit with the current HEAD")
        val project = e.project ?: return
        val gitRepository = GitRepositoryManager.getInstance(project).repositories.firstOrNull() ?: return
        val selectedCommit = e.getData(VcsDataKeys.VCS_REVISION_NUMBER) ?: return

        Thread {
            try {
                val diffInfo = getChangesBetweenCommits(gitRepository, selectedCommit).ifEmpty { "No changes found" }
                openChatWithDiff(e, diffInfo)
            } catch (e: Throwable) {
                log.error("Error comparing changes", e)
            }
        }.start()
    }

    private fun openChatWithDiff(e: AnActionEvent, diffInfo: String) {
        val session = StorageInterface.newGlobalID()
        SessionProxyServer.agents[session] = CodeChatSocketManager(
            session = session,
            language = "diff",
            codeSelection = diffInfo,
            filename = "commit_changes.diff",
            api = IdeaOpenAIClient.instance,
            model = AppSettingsState.instance.smartModel.chatModel(),
            storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome)
        )
        ApplicationServer.sessionAppInfoMap[session.toString()] = mapOf(
            "applicationName" to "Commit Chat",
            "singleInput" to false,
            "stickyInput" to true,
            "loadImages" to false,
            "showMenubar" to false,
        )

        val server = AppServer.getServer(e.project)

        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                log.info("Opening browser to $uri")
                Desktop.getDesktop().browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    private fun getChangesBetweenCommits(repository: GitRepository, selectedCommit: VcsRevisionNumber): String {
        val currentHead = repository.currentRevision ?: return ""
        val commitID = (selectedCommit as TextRevisionNumber).asString()
        val diff = Git.getInstance().diff(repository, listOf(
            "-D", "--text", "--no-color", "--no-commit-id"
        ), "$currentHead..$commitID")
        return diff.outputAsJoinedString
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(VcsDataKeys.VCS) == GitVcs.getKey()
    }

}