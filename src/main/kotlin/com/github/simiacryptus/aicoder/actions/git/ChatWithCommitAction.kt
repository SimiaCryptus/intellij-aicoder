package com.github.simiacryptus.aicoder.actions.git

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.CodeChatSocketManager
import com.github.simiacryptus.aicoder.util.IdeaChatClient
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.diff.IterativePatchUtil
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import java.io.File

val String.isBinary: Boolean
    get() {
        val binary = this.toByteArray().filter { it < 0x20 || it > 0x7E }
        return binary.size > this.length / 10
    }

class ChatWithCommitAction : AnAction() {
    private val logger = Logger.getInstance(ChatWithCommitAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        logger.info("Comparing selected revision with the current working copy")
        val files = expand(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY))
        val changes = e.getData(VcsDataKeys.CHANGES)
        Thread {
            try {
                val map = changes?.toList()
                    ?.associateBy { (it.beforeRevision?.file ?: it.afterRevision?.file)!!.toString() }
                val msg = map?.entries
                    ?.filter { (file, change) ->
                        val find = files?.find { it.toNioPath().toFile().absolutePath == File(file).absolutePath }
                        find != null
                    }
                    ?.joinToString("\n\n") { (file, change) ->
                        val before = change.beforeRevision?.content
                        val after = change.afterRevision?.content
                        if ((before ?: after)!!.isBinary)
                            return@joinToString "# Binary: ${change.afterRevision?.file}".replace("\n", "\n  ")
                        if (before == null) return@joinToString "# Deleted: ${change.afterRevision?.file}\n${after}".replace(
                            "\n",
                            "\n  "
                        )
                        if (after == null) return@joinToString "# Added: ${change.beforeRevision?.file}\n${before}".replace(
                            "\n",
                            "\n  "
                        )
                        val diff = IterativePatchUtil.generatePatch(before, after)
                        "# Change: ${change.beforeRevision?.file}\n$diff".replace("\n", "\n  ")
                    }

                // Open chat with the diff information
                openChatWithDiff(e, msg ?: "No changes found")
            } catch (e: Throwable) {
                logger.error("Error comparing changes", e)
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
                logger.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                logger.warn("Error opening browser", e)
            }
        }.start()
    }

    private fun expand(data: Array<VirtualFile>?): Array<VirtualFile>? {
        return data?.flatMap {
            if (it.isDirectory) {
                expand(it.children.toList().toTypedArray())?.toList() ?: listOf()
            } else {
                listOf(it)
            }
        }?.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(DataKey.create<String>("VCS")) != "Git"
    }

}