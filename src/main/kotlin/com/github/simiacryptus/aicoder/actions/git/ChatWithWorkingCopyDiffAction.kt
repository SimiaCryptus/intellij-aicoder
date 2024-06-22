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
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import git4idea.GitVcs
import git4idea.commands.Git
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.awt.Desktop

class ChatWithWorkingCopyDiffAction : AnAction() {
    companion object {
        private val log = Logger.getInstance(ChatWithWorkingCopyDiffAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        log.info("Comparing HEAD with the working copy")
        val project = e.project ?: return
        val gitRepository = GitRepositoryManager.getInstance(project).repositories.firstOrNull() ?: return

        Thread {
            try {
                val diffInfo = getChangesBetweenHeadAndWorkingCopy(gitRepository).ifEmpty { "No changes found" }
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
            filename = "working_copy_changes.diff",
            api = IdeaOpenAIClient.instance,
            model = AppSettingsState.instance.smartModel.chatModel(),
            storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome)
        )
        ApplicationServer.sessionAppInfoMap[session.toString()] = mapOf(
            "applicationName" to "Working Copy Diff Chat",
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

    private fun getChangesBetweenHeadAndWorkingCopy(repository: GitRepository): String {
        val changesIn = ChangeListManager.getInstance(repository.project).getChangesIn(repository.root)
        changesIn.forEach {
            log.info("Change: ${it.beforeRevision?.file} -> ${it.afterRevision?.file}")
        }
        val diff = Git.getInstance().diff(repository, listOf(
            "-D", "--text", "--no-color", "--no-commit-id"
        ), "HEAD")
        return diff.outputAsJoinedString
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(VcsDataKeys.VCS) == GitVcs.getKey()
    }
}
