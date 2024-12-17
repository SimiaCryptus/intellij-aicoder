package aicoder.actions.plan

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.SimpleCommandAction.Companion.tripleTilde
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.general.AutoPlanChatApp
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat

class AutoPlanChatAction : BaseAction() {
    // Maximum file size to process (512KB)
    private companion object {
        private const val MAX_FILE_SIZE = 512 * 1024
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        val dialog = PlanConfigDialog(
            e.project, PlanSettings(
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
            )
        )
        if (dialog.showAndGet()) {
            try {
                val planSettings = dialog.settings
                UITools.runAsync(e.project, "Initializing Auto Plan Chat", true) { progress ->
                    initializeChat(e, progress, planSettings)
                }
            } catch (ex: Exception) {
                log.error("Failed to initialize chat", ex)
                UITools.showError(e.project, "Failed to initialize chat: ${ex.message}")
            }
        }
    }

    private fun initializeChat(e: AnActionEvent, progress: ProgressIndicator, planSettings: PlanSettings) {
        progress.text = "Setting up session..."
        val session = Session.newGlobalID()
        val root = getProjectRoot(e) ?: throw RuntimeException("Could not determine project root")
        progress.text = "Processing files..."
        setupChatSession(session, root, e, planSettings)
        progress.text = "Starting server..."
        val server = AppServer.getServer(e.project)
        openBrowser(server, session.toString())
    }

    private fun getProjectRoot(e: AnActionEvent): File? {
        val folder = UITools.getSelectedFolder(e)
        return folder?.toFile ?: UITools.getSelectedFile(e)?.parent?.toFile?.let { file ->
            getModuleRootForFile(file)
        }
    }

    private fun setupChatSession(session: Session, root: File, e: AnActionEvent, planSettings: PlanSettings) {
        DataStorage.sessionPaths[session] = root
        SessionProxyServer.chats[session] = createChatApp(root, e, planSettings)
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Auto Plan Chat",
            singleInput = false,
            stickyInput = true,
            loadImages = false,
            showMenubar = false
        )
        SessionProxyServer.metadataStorage.setSessionName(null, session, "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}")
    }

    private fun createChatApp(root: File, e: AnActionEvent, planSettings: PlanSettings): AutoPlanChatApp = object : AutoPlanChatApp(
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
        private fun codeFiles() = (UITools.getSelectedFiles(e).toTypedArray().toList().flatMap<VirtualFile, File> {
            FileValidationUtils.expandFileList(it.toFile).toList<File>()
        }.map<File, Path> { it.toPath() }.toSet<Path>()?.toMutableSet<Path>() ?: mutableSetOf<Path>())
            .filter { it.toFile().exists() }
            .filter { it.toFile().length() < MAX_FILE_SIZE }
            .map { root.toPath().relativize(it) ?: it }.toSet()

        private fun codeSummary() = codeFiles()
            .joinToString("\n\n") { path ->
              "# ${path}\n$tripleTilde${path.toString().split('.').lastOrNull()}\n${root.resolve(path.toFile()).readText(Charsets.UTF_8)}\n$tripleTilde"
            }

        private fun projectSummary() = codeFiles()
            .asSequence().distinct().sorted()
            .joinToString("\n") { path ->
                "* ${path} - ${root.resolve(path.toFile()).length()} bytes"
            }

        override fun contextData(): List<String> =
            try {
                listOf(
                    if (codeFiles().size < 4) {
                        "Files:\n" + codeSummary()
                    } else {
                        "Files:\n" + projectSummary()
                    }
                )
            } catch (e: Exception) {
                log.error("Error generating context data", e)
                emptyList()
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

}