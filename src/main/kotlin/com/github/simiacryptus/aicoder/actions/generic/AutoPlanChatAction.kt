package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.SimpleCommandAction.Companion.getFiles
import com.github.simiacryptus.aicoder.actions.generic.SimpleCommandAction.Companion.tripleTilde
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.general.AutoPlanChatApp
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory

class AutoPlanChatAction : BaseAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        val dialog = PlanAheadConfigDialog(
            e.project, PlanSettings(
                defaultModel = AppSettingsState.instance.smartModel.chatModel(),
                parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                command = listOf(
                    if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                ),
                temperature = AppSettingsState.instance.temperature,
                workingDir = UITools.getSelectedFolder(e)?.toFile?.absolutePath ?: "",
                env = mapOf()
            )
        )
        if (dialog.showAndGet()) {
            // Settings are applied only if the user clicks OK
            val session = Session.newGlobalID()
            val folder = UITools.getSelectedFolder(e)
            val root = folder?.toFile ?: getModuleRootForFile(
                UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException("")
            )
            DataStorage.sessionPaths[session] = root
            SessionProxyServer.chats[session] = object : AutoPlanChatApp(
                planSettings = dialog.settings.copy(
                    env = mapOf(),
                    workingDir = root.absolutePath,
                    language = if (isWindows) "powershell" else "bash",
                    command = listOf(
                        if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                    ),
                    parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                ),
                model = AppSettingsState.instance.smartModel.chatModel(),
                parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                showMenubar = false,
                api = api,
            ) {
                fun codeFiles() = getFiles(UITools.getSelectedFiles(e).toTypedArray())
                    .filter { it.toFile().exists() }
                    .filter { it.toFile().length() < 1024 * 1024 / 2 }
                    .map { root.toPath().relativize(it) ?: it }.toSet()

                fun codeSummary() = codeFiles()
                    .joinToString("\n\n") { path ->
                        """
                        |# ${path}
                        |$tripleTilde${path.toString().split('.').lastOrNull()}
                        |${root.resolve(path.toFile()).readText(Charsets.UTF_8)}
                        |$tripleTilde
                    """.trimMargin()
                    }

                fun projectSummary() = codeFiles()
                    .asSequence().distinct().sorted()
                    .joinToString("\n") { path ->
                        "* ${path} - ${root.resolve(path.toFile()).length()} bytes"
                    }

                override fun initialPrompt(userMessage: String) = super.initialPrompt(userMessage) + listOf(
                    if (codeFiles().size < 4) {
                        "Files:\n" + codeSummary()
                    } else {
                        "Files:\n" + projectSummary()
                    },
                )
            }
            ApplicationServer.appInfoMap[session] = AppInfoData(
                applicationName = "Auto Plan Chat",
                singleInput = true,
                stickyInput = false,
                loadImages = false,
                showMenubar = false
            )
            val server = AppServer.getServer(e.project)
            openBrowser(server, session.toString())
        }
    }

    private fun openBrowser(server: AppServer, session: String) {
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

    companion object {
        private val log = LoggerFactory.getLogger(AutoPlanChatAction::class.java)
    }
}