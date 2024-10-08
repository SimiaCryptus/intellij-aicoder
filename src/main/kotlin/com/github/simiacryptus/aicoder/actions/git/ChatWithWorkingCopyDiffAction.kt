package com.github.simiacryptus.aicoder.actions.git

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import com.github.simiacryptus.aicoder.util.CodeChatSocketManager
import com.github.simiacryptus.aicoder.util.IdeaChatClient
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vcs.VcsDataKeys
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import git4idea.GitVcs
import git4idea.commands.Git
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import javax.swing.JOptionPane

class ChatWithWorkingCopyDiffAction : AnAction() {
    companion object {
        private val log = Logger.getInstance(ChatWithWorkingCopyDiffAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        log.info("Comparing HEAD with the working copy")
        val project = e.project ?: return
        val files = e.getData(VcsDataKeys.VIRTUAL_FILES)?.firstOrNull()
        val repositories = GitRepositoryManager.getInstance(project).repositories
        val gitRepository = repositories.find { it.root == files } ?: return

        Thread {
            try {
                val diffInfo = getChangesBetweenHeadAndWorkingCopy(gitRepository).ifEmpty { "No changes found" }
                openChatWithDiff(e, diffInfo)
            } catch (e: Throwable) {
                log.error("Error comparing changes", e)
                JOptionPane.showMessageDialog(null, e.message, "Error", JOptionPane.ERROR_MESSAGE)
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

    private fun getChangesBetweenHeadAndWorkingCopy(repository: GitRepository): String {
        val diff = Git.getInstance().diff(repository, listOf(
            "-D", "--text", "--no-color", "--no-commit-id"
        ), "HEAD")
        if (0 != diff.exitCode) {
            throw RuntimeException("Error running git diff command: ${diff.errorOutput}")
        }
        return diff.outputAsJoinedString
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val vcs = e.getData(VcsDataKeys.VCS)
        val gitVcs = GitVcs.getInstance(project)
        e.presentation.isEnabledAndVisible = project != null && (vcs!!.name == gitVcs.name)
    }
}