package aicoder.actions.agent

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.AddApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.apps.general.renderMarkdown
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.FileSelectionUtils
import com.simiacryptus.skyenet.core.util.FileSelectionUtils.Companion.filteredWalk
import com.simiacryptus.skyenet.core.util.FileSelectionUtils.Companion.isGitignore
import com.simiacryptus.skyenet.core.util.FileSelectionUtils.Companion.isLLMIncludableFile
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.walk

class SimpleCommandAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(event: AnActionEvent) {
        val project = event.project
        try {
            val settings = getUserSettings(event) ?: run {
                log.error("Failed to retrieve user settings")
                UITools.showErrorDialog("Failed to retrieve settings", "Error")
                return
            }
            UITools.run(project, "Initializing", true) { progress ->
                progress.text = "Setting up command execution..."
                val dataContext = event.dataContext
                val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
                val folder = UITools.getSelectedFolder(event)
                val root = folder?.toFile?.toPath() ?: project?.basePath?.let { File(it).toPath() } ?: run {
                    throw IllegalStateException("Failed to determine project root")
                }

                val session = Session.newGlobalID()
                progress.text = "Creating patch application..."
                val patchApp = createPatchApp(root.toFile(), session, settings, virtualFiles)
                progress.text = "Configuring session..."
                SessionProxyServer.metadataStorage.setSessionName(
                    null,
                    session,
                    "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
                )
                SessionProxyServer.chats[session] = patchApp
                ApplicationServer.appInfoMap[session] = AppInfoData(
                    applicationName = "Code Chat",
                    singleInput = true,
                    stickyInput = false,
                    loadImages = false,
                    showMenubar = false
                )
                val server = AppServer.getServer(project)
                openBrowserWithDelay(server.server.uri.resolve("/#$session"))
            }

        } catch (e: Exception) {
            log.error("Error handling action", e)
            UITools.showErrorDialog(
                "Failed to execute command: ${e.message}",
                "Error"
            )
        }
    }

    private fun createPatchApp(
        root: File,
        session: Session,
        settings: Settings,
        virtualFiles: Array<out VirtualFile>?
    ): PatchApp = UITools.run(null, "Creating Patch Application", true) { progress ->
        progress.text = "Initializing patch application..."
        object : PatchApp(root, session, settings) {
            // Limit file size to 0.5MB for performance
            private val maxFileSize = 512 * 1024

            override fun codeFiles() = (virtualFiles?.toList<VirtualFile>()?.flatMap<VirtualFile, File> {
                FileSelectionUtils.expandFileList(it.toFile).toList()
            }?.map<File, Path> { it.toPath() }?.toSet<Path>()?.toMutableSet<Path>() ?: mutableSetOf<Path>())
                .filter { it.toFile().length() < maxFileSize }
                .map { root.toPath().relativize(it) ?: it }.toSet()
            // Add progress indication for long operations

            override fun codeSummary(paths: List<Path>) = paths
                .filter { it.toFile().exists() }
                .mapIndexed { index, path ->
                    progress.fraction = index.toDouble() / paths.size
                    progress.text = "Processing ${path.fileName}..."
                    if (progress.isCanceled) throw InterruptedException("Operation cancelled")
                    "# ${settings.workingDirectory.toPath().relativize(path)}\n$tripleTilde${
                        path.toString().split('.').lastOrNull()
                    }\n${
                        path.toFile().readText(Charsets.UTF_8)
                    }\n$tripleTilde"
                }.joinToString("\n\n")
            // Add validation for file operations

            override fun projectSummary() = codeFiles()
                .asSequence()
                .filter { settings.workingDirectory.toPath()?.resolve(it)?.toFile()?.exists() == true }
                .distinct().sorted()
                .joinToString("\n") { path ->
                    "* ${path} - ${
                        settings.workingDirectory.toPath()?.resolve(path)?.toFile()?.length() ?: "?"
                    } bytes".trim()
                }

            override fun searchFiles(searchStrings: List<String>): Set<Path> {
                require(searchStrings.isNotEmpty()) { "Search strings cannot be empty" }
                return searchStrings.flatMap { searchString ->
                    filteredWalk(settings.workingDirectory) { !isGitignore(it.toPath()) }
                        .filter { isLLMIncludableFile(it) }
                        .filter { it.readText().contains(searchString, ignoreCase = true) }
                        .map { it.toPath() }
                        .toList()
                }.toSet()
            }
        }
    }
    // Add proper resource cleanup

    private fun openBrowserWithDelay(uri: java.net.URI) {
        Thread({
            Thread.sleep(500)
            try {
                log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
                UITools.showErrorDialog("Failed to open browser: ${e.message}", "Error")
            }
        }, "BrowserOpener").apply { isDaemon = true }.start()
    }

    abstract inner class PatchApp(
        override val root: File,
        val session: Session,
        val settings: Settings,
    ) : ApplicationServer(
        applicationName = "Magic Code Genie",
        path = "/doCmd",
        showMenubar = false,
    ) {
        abstract fun codeFiles(): Set<Path>
        abstract fun codeSummary(paths: List<Path>): String
        abstract fun searchFiles(searchStrings: List<String>): Set<Path>
        override val singleInput = true
        override val stickyInput = false

        override fun userMessage(
            session: Session,
            user: User?,
            userMessage: String,
            ui: ApplicationInterface,
            api: API
        ) {
            val task = ui.newTask()
            task.echo(renderMarkdown(userMessage))
            Thread {
                run(ui, task, session, settings, userMessage)
            }.start()
        }

        abstract fun projectSummary(): String
    }

    private fun PatchApp.run(
        ui: ApplicationInterface,
        task: SessionTask,
        session: Session,
        settings: Settings,
        userMessage: String = ""
    ) {
        val planTxt = projectSummary()
        task.verbose(planTxt.renderMarkdown())
        Retryable(ui, task) {
            val task = ui.newTask(false)
            try {
                val plan = ParsedActor(
                    resultClass = ParsedTasks::class.java,
                    prompt = """
                      You are a helpful AI that helps people with coding.
                      
                      You will be answering questions about the following project:
                      
                      Project Root: """.trimIndent() + (settings.workingDirectory.absolutePath ?: "") + """
                      
                      Files:
                      """.trimIndent() + planTxt + """
                      
                      Given the request, identify one or more tasks.
                      For each task:
                         1) predict the files that need to be fixed
                         2) predict related files that may be needed to debug the issue
                      """.trimIndent(),
                    model = AppSettingsState.instance.smartModel.chatModel(),
                    parsingModel = AppSettingsState.instance.fastModel.chatModel(),
                ).answer(
                    listOf(
                        "\nExecute the following directive:\n\n$tripleTilde\n$userMessage\n$tripleTilde\n"
                    ), api = api
                )
                val progressHeader = task.header("Processing tasks", 1)
                plan.obj.errors?.forEach { planTask ->
                    Retryable(ui, task) {
                        val task = ui.newTask(false)
                        val paths =
                            ((planTask.fixFiles ?: emptyList()) + (planTask.relatedFiles ?: emptyList())).flatMap {
                                toPaths(settings.workingDirectory.toPath(), it)
                            }
                        val searchResults = searchFiles(planTask.searchStrings ?: emptyList())
                        val combinedPaths = (paths + searchResults).distinct()
                        val prunedPaths = prunePaths(combinedPaths, 50 * 1024)
                        val codeSummary =
                            codeSummary(prunedPaths.map { settings.workingDirectory.toPath().resolve(it) })
                        val response = SimpleActor(
                            prompt = """
                You are a helpful AI that helps people with coding.
                
                You will be answering questions about the following code:
                
                """.trimIndent() + codeSummary + "\n\n" + patchFormatPrompt + """
                
                If needed, new files can be created by using code blocks labeled with the filename in the same manner.
                """.trimIndent(),
                            model = AppSettingsState.instance.smartModel.chatModel()
                        ).answer(
                            listOf(
                                "We are working on executing the following directive:\n\n$tripleTilde\n$userMessage\n$tripleTilde\n\nFocus on the task at hand:\n  ${
                                    planTask.message?.replace(
                                        "\n",
                                        "\n  "
                                    ) ?: ""
                                }"
                            ), api = api
                        )
                        val markdown = AddApplyFileDiffLinks.instrumentFileDiffs(
                            ui.socketManager!!,
                            root = root.toPath(),
                            response = response,
                            handle = { newCodeMap ->
                                newCodeMap.forEach { (path, newCode) ->
                                    task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                                }
                            },
                            ui = ui,
                            api = api,
                        )
                        task.add(renderMarkdown(markdown))
                        task.placeholder
                    }
                }
                progressHeader?.clear()
                task.append(
                    AgentPatterns.displayMapInTabs(
                        mapOf(
                            "Text" to renderMarkdown(plan.text, ui = ui),
                            "JSON" to renderMarkdown(
                                "${tripleTilde}json\n${JsonUtil.toJson(plan.obj)}\n$tripleTilde",
                                ui = ui
                            )
                        )
                    ), false
                )
            } catch (e: Exception) {
                log.error("Error during task execution", e)
                task.error(ui, e)
            }
            task.placeholder
        }
    }

    private fun prunePaths(paths: List<Path>, maxSize: Int): List<Path> {
        val sortedPaths = paths.sortedByDescending { it.toFile().length() }
        var totalSize = 0
        val prunedPaths = mutableListOf<Path>()
        for (path in sortedPaths) {
            val fileSize = path.toFile().length().toInt()
            if (totalSize + fileSize > maxSize) break
            prunedPaths.add(path)
            totalSize += fileSize
        }
        return prunedPaths
    }

    data class ParsedTasks(
        val errors: List<ParsedTask>? = null
    )

    data class ParsedTask(
        @Description("The task to be performed")
        val message: String? = null,
        @Description("Files identified as needing modification and issue-related files, in order of descending relevance")
        val relatedFiles: List<String>? = null,
        @Description("Files identified as needing modification and issue-related files, in order of descending relevance")
        val fixFiles: List<String>? = null,
        @Description("Search strings to find relevant files, in order of descending relevance")
        val searchStrings: List<String>? = null
    )

    data class Settings(
        var workingDirectory: File,
    )

    private fun getUserSettings(event: AnActionEvent?): Settings? {
        val root = UITools.getSelectedFolder(event ?: return null)?.toNioPath() ?: event.project?.basePath?.let {
            File(it).toPath()
        }
        val files = UITools.getSelectedFiles(event).map { it.path.let { File(it).toPath() } }.toMutableSet()
        if (files.isEmpty()) Files.walk(root)
            .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
            .toList().filterNotNull().forEach { files.add(it) }
        return root?.toFile()?.let { Settings(it) }
    }

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(SimpleCommandAction::class.java)
        val tripleTilde = "`" + "``" // This is a workaround for the markdown parser when editing this file

        @OptIn(ExperimentalPathApi::class)
        fun toPaths(root: Path, it: String): Iterable<Path> {
            // Expand any wildcards
            return if (it.contains("*")) {
                val prefix = it.substringBefore("*")
                val suffix = it.substringAfter("*")
                val files = root.walk().toList()
                files.filter {
                    it.toString().startsWith(prefix) && it.toString().endsWith(suffix)
                }
            } else {
                listOf(Path.of(it))
            }
        }

    }
}