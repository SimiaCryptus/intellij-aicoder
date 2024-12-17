package aicoder.actions.plan

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.GPT4Tokenizer
import com.simiacryptus.skyenet.apps.general.SingleTaskApp
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import java.io.File
import java.text.SimpleDateFormat

class SingleTaskAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        val dialog = PlanConfigDialog(
            e.project,
            PlanSettings(
                defaultModel = AppSettingsState.instance.smartModel.chatModel(),
                parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                shellCmd = listOf(
                    if (System.getProperty("os.name").lowercase().contains("win")) "powershell" else "bash"
                ),
                temperature = AppSettingsState.instance.temperature.coerceIn(0.0, 1.0),
                workingDir = UITools.getRoot(e),
                env = mapOf(),
                githubToken = AppSettingsState.instance.githubToken,
                googleApiKey = AppSettingsState.instance.googleApiKey,
                googleSearchEngineId = AppSettingsState.instance.googleSearchEngineId,
            ),
            singleTaskMode = true
        )
        if (dialog.showAndGet()) {
            try {
                val planSettings = dialog.settings
                UITools.runAsync(e.project, "Initializing Single Task", true) { progress ->
                    initializeTask(e, progress, planSettings, contextData(e))
                }
            } catch (ex: Exception) {
                log.error("Failed to initialize task", ex)
                UITools.showError(e.project, "Failed to initialize task: ${ex.message}")
            }
        }
    }

    private fun initializeTask(
        e: AnActionEvent,
        progress: ProgressIndicator,
        planSettings: PlanSettings,
        contextData: List<String> = emptyList(),
    ) {
        progress.text = "Setting up session..."
        val session = Session.newGlobalID()
        val root = getProjectRoot(e)
        progress.text = "Processing files..."
        setupTaskSession(session, root, planSettings, contextData)
        progress.text = "Starting server..."
        val server = AppServer.getServer(e.project)
        openBrowser(server, session.toString())
    }

    private fun getProjectRoot(e: AnActionEvent) =
        UITools.getSelectedFolder(e)?.toFile ?: UITools.getRoot(e).let { File(it) }

    private fun setupTaskSession(
        session: Session,
        root: File,
        planSettings: PlanSettings,
        contextData: List<String> = emptyList(),
    ) {
        DataStorage.sessionPaths[session] = root
        SessionProxyServer.chats[session] = createSingleTaskApp(root, planSettings, contextData)
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Single Task",
            singleInput = true,
            stickyInput = false,
            loadImages = false,
            showMenubar = false
        )
        SessionProxyServer.metadataStorage.setSessionName(
            null,
            session,
            "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
        )
    }

    private fun createSingleTaskApp(
        root: File,
        planSettings: PlanSettings,
        contextData: List<String> = emptyList(),
    ): SingleTaskApp = object : SingleTaskApp(
        applicationName = "Single Task",
        planSettings = planSettings.copy(
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
        api2 = api2,
    ) {
        override fun contextData(): List<String> {
            return contextData
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
        fun contextData(event: AnActionEvent): List<String> {
            val selectedFiles = UITools.getSelectedFiles(event).toTypedArray().toList()
            if (selectedFiles.isEmpty()) return emptyList()
          val root = File(UITools.getRoot(event))
            return selectedFiles
                .flatMap { virtualFile ->
                    try {
                        FileValidationUtils.expandFileList(virtualFile.toFile).toList()
                    } catch (e: Exception) {
                      emptyList()
                    }
                }
                .filter { file ->
                  file.exists() && file.length() < 512 * 1024
                }.mapNotNull { file ->
                    try {
                        val relativePath = root.toPath().relativize(file.toPath())
                      val text = file.readText(Charsets.UTF_8)
                      val tokenCount = GPT4Tokenizer().estimateTokenCount(text)
                        """
                        * ${relativePath} - ${file.length()} bytes, ${tokenCount} tokens
                        """.trimIndent()
                    } catch (e: Exception) {
                        null
                    }
                }

        }
    }
}