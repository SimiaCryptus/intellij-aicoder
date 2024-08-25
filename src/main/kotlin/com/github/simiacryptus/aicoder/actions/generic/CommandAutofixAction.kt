package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.CommandAutofixAction.Companion.htmlEscape
import com.github.simiacryptus.aicoder.actions.generic.CommandAutofixAction.Companion.truncate
import com.github.simiacryptus.aicoder.actions.generic.CommandAutofixAction.OutputResult
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.FileSystemUtils.isGitignore
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.diff.FileValidationUtils.Companion.filteredWalk
import com.simiacryptus.diff.FileValidationUtils.Companion.isGitignore
import com.simiacryptus.diff.FileValidationUtils.Companion.isLLMIncludable
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javax.swing.*
import kotlin.collections.set

class CommandAutofixAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(event: AnActionEvent) {
        val settings = getUserSettings(event) ?: return
        val dataContext = event.dataContext
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        val folder = UITools.getSelectedFolder(event)
        val root = if (null != folder) {
            folder.toFile.toPath()
        } else {
            event.project?.basePath?.let { File(it).toPath() }
        }!!
        val session = StorageInterface.newGlobalID()
        val patchApp = CmdPatchApp(root, session, settings, api, virtualFiles)
        SessionProxyServer.chats[session] = patchApp
        val server = AppServer.getServer(event.project)
        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                Desktop.getDesktop().browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    data class OutputResult(val exitCode: Int, val output: String)

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(CommandAutofixAction::class.java)
        const val tripleTilde = "`" + "``" // This is a workaround for the markdown parser when editing this file

        val String.htmlEscape: String
            get() = this.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")

        fun truncate(output: String, kb: Int = 32): String {
            var returnVal = output
            if (returnVal.length > 1024 * 2 * kb) {
                returnVal = returnVal.substring(0, 1024 * kb) +
                        "\n\n... Output truncated ...\n\n" +
                        returnVal.substring(returnVal.length - 1024 * kb)
            }
            return returnVal
        }


        private fun getUserSettings(event: AnActionEvent?): PatchApp.Settings? {
            val root = UITools.getSelectedFolder(event ?: return null)?.toNioPath() ?: event.project?.basePath?.let {
                File(it).toPath()
            }
            val files = UITools.getSelectedFiles(event).map { it.path.let { File(it).toPath() } }.toMutableSet()
            if (files.isEmpty()) Files.walk(root)
                .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
                .toList().filterNotNull().forEach { files.add(it) }
            val settingsUI = SettingsUI(root!!.toFile())
            val dialog = CommandSettingsDialog(event.project, settingsUI)
            dialog.show()
            return if (dialog.isOK) {
                val executable = File(settingsUI.commandField.selectedItem?.toString() ?: return null)
                AppSettingsState.instance.executables += executable.absolutePath
                val argument = settingsUI.argumentsField.selectedItem?.toString() ?: ""
                AppSettingsState.instance.recentArguments.remove(argument)
                AppSettingsState.instance.recentArguments.add(0, argument)
                AppSettingsState.instance.recentArguments =
                    AppSettingsState.instance.recentArguments.take(10).toMutableList()
                PatchApp.Settings(
                    executable = executable,
                    arguments = argument,
                    workingDirectory = File(settingsUI.workingDirectoryField.text),
                    exitCodeOption = if (settingsUI.exitCodeZero.isSelected) "0" else if (settingsUI.exitCodeAny.isSelected) "any" else "nonzero",
                    additionalInstructions = settingsUI.additionalInstructionsField.text,
                    autoFix = settingsUI.autoFixCheckBox.isSelected
                )
            } else {
                null
            }
        }

        class SettingsUI(root: File) {
            val argumentsField = ComboBox<String>().apply {
                isEditable = true
                AppSettingsState.instance.recentArguments.forEach { addItem(it) }
                if (AppSettingsState.instance.recentArguments.isEmpty()) {
                    addItem("run build")
                }
            }
            val commandField = ComboBox(AppSettingsState.instance.executables.toTypedArray()).apply {
                isEditable = true
            }
            val commandButton = JButton("...").apply {
                addActionListener {
                    val fileChooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.FILES_ONLY
                        isMultiSelectionEnabled = false
                    }
                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        commandField.selectedItem = fileChooser.selectedFile.absolutePath
                    }
                }
            }
            val workingDirectoryField = JTextField(root.absolutePath).apply {
                isEditable = true
            }
            val workingDirectoryButton = JButton("...").apply {
                addActionListener {
                    val fileChooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        isMultiSelectionEnabled = false
                        this.selectedFile = File(workingDirectoryField.text)
                    }
                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        workingDirectoryField.text = fileChooser.selectedFile.absolutePath
                    }
                }
            }
            val exitCodeOptions = ButtonGroup()
            val exitCodeNonZero = JRadioButton("Patch nonzero exit code", true)
            val exitCodeZero = JRadioButton("Patch 0 exit code")
            val exitCodeAny = JRadioButton("Patch any exit code")
            val additionalInstructionsField = JTextArea().apply {
                rows = 3
                lineWrap = true
                wrapStyleWord = true
            }
            val autoFixCheckBox = JCheckBox("Auto-apply fixes").apply {
                isSelected = false
            }
        }

        class CommandSettingsDialog(project: Project?, private val settingsUI: SettingsUI) : DialogWrapper(project) {
            init {
                settingsUI.exitCodeOptions.add(settingsUI.exitCodeNonZero)
                settingsUI.exitCodeOptions.add(settingsUI.exitCodeZero)
                settingsUI.exitCodeOptions.add(settingsUI.exitCodeAny)
                title = "Command Autofix Settings"
                init()
            }

            override fun createCenterPanel(): JComponent {
                val panel = JPanel(BorderLayout()).apply {

                    val optionsPanel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        add(JLabel("Executable"))
                        add(JPanel(BorderLayout()).apply {
                            add(settingsUI.commandField, BorderLayout.CENTER)
                            add(settingsUI.commandButton, BorderLayout.EAST)
                        })
                        add(JLabel("Arguments"))
                        add(settingsUI.argumentsField)
                        add(JLabel("Working Directory"))
                        add(JPanel(BorderLayout()).apply {
                            add(settingsUI.workingDirectoryField, BorderLayout.CENTER)
                            add(settingsUI.workingDirectoryButton, BorderLayout.EAST)
                        })
                        add(JLabel("Exit Code Options"))
                        add(settingsUI.exitCodeNonZero)
                        add(settingsUI.exitCodeAny)
                        add(settingsUI.exitCodeZero)
                        add(JLabel("Additional Instructions"))
                        add(JScrollPane(settingsUI.additionalInstructionsField))
                        add(settingsUI.autoFixCheckBox)
                    }
                    add(optionsPanel, BorderLayout.SOUTH)
                }
                return panel
            }
        }

    }
}

class CmdPatchApp(
    root: Path,
    session: Session,
    settings: Settings,
    api: OpenAIClient,
    val virtualFiles: Array<out VirtualFile>?
) : PatchApp(root.toFile(), session, settings, api) {
    companion object {
        private val log = LoggerFactory.getLogger(CmdPatchApp::class.java)
    }

    private fun getFiles(
        virtualFiles: Array<out VirtualFile>?
    ): MutableSet<Path> {
        val codeFiles = mutableSetOf<Path>()    // Set to avoid duplicates
        virtualFiles?.forEach { file ->
            if (file.isDirectory) {
                if (file.name.startsWith(".")) return@forEach
                if (isGitignore(file)) return@forEach
                codeFiles.addAll(getFiles(file.children))
            } else {
                codeFiles.add((file.toNioPath()))
            }
        }
        return codeFiles
    }

    override fun codeFiles() = getFiles(virtualFiles)
        .filter { it.toFile().length() < 1024 * 1024 / 2 } // Limit to 0.5MB
        .map { root.toPath().relativize(it) ?: it }.toSet()

    override fun codeSummary(paths: List<Path>): String = paths
        .filter {
            val file = settings.workingDirectory?.resolve(it.toFile())
            file?.exists() == true && !file.isDirectory && file.length() < (256 * 1024)
        }
        .joinToString("\n\n") { path ->
            try {
                """
                        |# ${path}
                        |${CommandAutofixAction.tripleTilde}${path.toString().split('.').lastOrNull()}
                        |${settings.workingDirectory?.resolve(path.toFile())?.readText(Charsets.UTF_8)}
                        |${CommandAutofixAction.tripleTilde}
                        """.trimMargin()
            } catch (e: Exception) {
                log.warn("Error reading file", e)
                "Error reading file `${path}` - ${e.message}"
            }
        }

    override fun projectSummary(): String {
        val codeFiles = codeFiles()
        val str = codeFiles
            .asSequence()
            .filter { settings.workingDirectory?.toPath()?.resolve(it)?.toFile()?.exists() == true }
            .distinct().sorted()
            .joinToString("\n") { path ->
                "* ${path} - ${
                    settings.workingDirectory?.toPath()?.resolve(path)?.toFile()?.length() ?: "?"
                } bytes".trim()
            }
        return str
    }

    override fun output(task: SessionTask): OutputResult = run {
        val command = listOf(settings.executable.absolutePath) + settings.arguments.split(" ")
        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(settings.workingDirectory)
        val buffer = StringBuilder()
        val taskOutput = task.add("")
        val process = processBuilder.start()
        Thread {
            var lastUpdate = 0L
            process.errorStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    buffer.append(line).append("\n")
                    if (lastUpdate + TimeUnit.SECONDS.toMillis(15) < System.currentTimeMillis()) {
                        taskOutput?.set("<pre>\n${truncate(buffer.toString()).htmlEscape}\n</pre>")
                        task.append("", true)
                        lastUpdate = System.currentTimeMillis()
                    }
                }
                task.append("", true)
            }
        }.start()
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            var lastUpdate = 0L
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line).append("\n")
                if (lastUpdate + TimeUnit.SECONDS.toMillis(15) < System.currentTimeMillis()) {
                    taskOutput?.set("<pre>\n${outputString(buffer).htmlEscape}\n</pre>")
                    task.append("", true)
                    lastUpdate = System.currentTimeMillis()
                }
            }
            task.append("", true)
        }
        task.append("", false)
        val exitCode = process.waitFor()
        var output = outputString(buffer)
        taskOutput?.clear()
        OutputResult(exitCode, output)
    }

    private fun outputString(buffer: StringBuilder): String {
        var output = buffer.toString()
        output = output.replace(Regex("\\x1B\\[[0-?]*[ -/]*[@-~]"), "") // Remove terminal escape codes
        output = truncate(output)
        return output
    }

    override fun searchFiles(searchStrings: List<String>): Set<Path> {
        return searchStrings.flatMap { searchString ->
            filteredWalk(settings.workingDirectory!!) { !isGitignore(it.toPath()) }
                .filter { isLLMIncludable(it) }
                .filter { it.readText().contains(searchString, ignoreCase = true) }
                .map { it.toPath() }
                .toList()
        }.toSet()
    }
}

abstract class PatchApp(
    override val root: File,
    val session: Session,
    val settings: Settings,
    val api: OpenAIClient,
) : ApplicationServer(
    applicationName = "Magic Code Fixer",
    path = "/fixCmd",
    showMenubar = false,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PatchApp::class.java)
    }

    abstract fun codeFiles(): Set<Path>
    abstract fun codeSummary(paths: List<Path>): String
    abstract fun output(task: SessionTask): OutputResult
    abstract fun searchFiles(searchStrings: List<String>): Set<Path>
    override val singleInput = true
    override val stickyInput = false
    override fun newSession(user: User?, session: Session): SocketManager {
        val socketManager = super.newSession(user, session)
        val ui = (socketManager as ApplicationSocketManager).applicationInterface
        val task = ui.newTask()
        Retryable(
            ui = ui,
            task = task,
            process = { content ->
                val newTask = ui.newTask(false)
                newTask.add("Running Command")
                Thread {
                    run(ui, newTask, settings, api)
                }.start()
                newTask.placeholder
            }
        )
        return socketManager
    }

    abstract fun projectSummary(): String

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

    data class ParsedErrors(
        val errors: List<ParsedError>? = null
    )

    data class ParsedError(
        @Description("The error message")
        val message: String? = null,
        @Description("Files identified as needing modification and issue-related files")
        val relatedFiles: List<String>? = null,
        @Description("Files identified as needing modification and issue-related files")
        val fixFiles: List<String>? = null,
        @Description("Search strings to find relevant files")
        val searchStrings: List<String>? = null
    )

    data class Settings(
        var executable: File,
        var arguments: String = "",
        var workingDirectory: File? = null,
        var exitCodeOption: String = "0",
        var additionalInstructions: String = "",
        val autoFix: Boolean,
    )

    fun run(
        ui: ApplicationInterface,
        task: SessionTask,
        settings: Settings,
        api: OpenAIClient,
    ) {
        val output = output(task)
        if (output.exitCode == 0 && settings.exitCodeOption == "nonzero") {
            task.complete(
                """
                |<div>
                |<div><b>Command executed successfully</b></div>
                |${renderMarkdown("${CommandAutofixAction.tripleTilde}\n${output.output}\n${CommandAutofixAction.tripleTilde}")}
                |</div>
                |""".trimMargin()
            )
            return
        }
        if (settings.exitCodeOption == "zero" && output.exitCode != 0) {
            task.complete(
                """
                |<div>
                |<div><b>Command failed</b></div>
                |${renderMarkdown("${CommandAutofixAction.tripleTilde}\n${output.output}\n${CommandAutofixAction.tripleTilde}")}
                |</div>
                |""".trimMargin()
            )
            return
        }
        try {
            task.add(
                """
            |<div>
            |<div><b>Command exit code: ${output.exitCode}</b></div>
            |${renderMarkdown("${CommandAutofixAction.tripleTilde}\n${output.output}\n${CommandAutofixAction.tripleTilde}")}
            |</div>
            """.trimMargin()
            )
            fixAll(settings, output, task, ui, api)
        } catch (e: Exception) {
            task.error(ui, e)
        }
    }

    private fun fixAll(
        settings: Settings,
        output: OutputResult,
        task: SessionTask,
        ui: ApplicationInterface,
        api: OpenAIClient,
    ) {
        Retryable(ui, task) { content ->
            fixAllInternal(settings, output, task, ui, mutableSetOf(), api)
            content.clear()
            ""
        }
    }

    private fun fixAllInternal(
        settings: Settings,
        output: OutputResult,
        task: SessionTask,
        ui: ApplicationInterface,
        changed: MutableSet<Path>,
        api: OpenAIClient,
    ) {
        val plan = ParsedActor(
            resultClass = ParsedErrors::class.java,
            prompt = """
                |You are a helpful AI that helps people with coding.
                |
                |You will be answering questions about the following project:
                |
                |Project Root: ${settings.workingDirectory?.absolutePath ?: ""}
                |
                |Files:
                |${projectSummary()}
                |
                |Given the response of a build/test process, identify one or more distinct errors.
                |For each error:
                |   1) predict the files that need to be fixed
                |   2) predict related files that may be needed to debug the issue
                |   3) specify a search string to find relevant files - be as specific as possible
                |${if (settings.additionalInstructions.isNotBlank()) "Additional Instructions:\n  ${settings.additionalInstructions}\n" else ""}
                """.trimMargin(),
            model = AppSettingsState.instance.defaultSmartModel()
        ).answer(
            listOf(
                """
                |The following command was run and produced an error:
                |
                |${CommandAutofixAction.tripleTilde}
                |${output.output}
                |${CommandAutofixAction.tripleTilde}
                """.trimMargin()
            ), api = api
        )
        task.add(
            AgentPatterns.displayMapInTabs(
                mapOf(
                    "Text" to renderMarkdown(plan.text, ui = ui),
                    "JSON" to renderMarkdown(
                        "${CommandAutofixAction.tripleTilde}json\n${JsonUtil.toJson(plan.obj)}\n${CommandAutofixAction.tripleTilde}",
                        ui = ui
                    ),
                )
            )
        )
        val progressHeader = task.header("Processing tasks")
        plan.obj.errors?.forEach { error ->
            task.header("Processing error: ${error.message}")
            task.verbose(renderMarkdown("```json\n${JsonUtil.toJson(error)}\n```", tabs = false, ui = ui))
            // Search for files using the provided search strings
            val searchResults = error.searchStrings?.flatMap { searchString ->
                filteredWalk(settings.workingDirectory!!) { !isGitignore(it.toPath()) }
                    .filter { isLLMIncludable(it) }
                    .filter { it.readText().contains(searchString, ignoreCase = true) }
                    .map { it.toPath() }
                    .toList()
            }?.toSet() ?: emptySet()
            task.verbose(
                renderMarkdown(
                    """
                    |Search results:
                    |
                    |${searchResults.joinToString("\n") { "* `$it`" }}
                    """.trimMargin(), tabs = false, ui = ui
                )
            )
            Retryable(ui, task) { content ->
                fix(
                    error, searchResults.toList().map { it.toFile().absolutePath },
                    output, ui, content, settings.autoFix, changed, api
                )
                content.toString()
            }
        }
        progressHeader?.clear()
        task.append("", false)
    }

    private fun fix(
        error: ParsedError,
        additionalFiles: List<String>? = null,
        output: OutputResult,
        ui: ApplicationInterface,
        content: StringBuilder,
        autoFix: Boolean,
        changed: MutableSet<Path>,
        api: OpenAIClient,
    ) {
        val paths =
            (
                    (error.fixFiles ?: emptyList()) +
                            (error.relatedFiles ?: emptyList()) +
                            (additionalFiles ?: emptyList())
                    ).map { File(it).toPath() }
        val prunedPaths = prunePaths(paths, 50 * 1024)
        val summary = codeSummary(prunedPaths)
        val response = SimpleActor(
            prompt = """
                    |You are a helpful AI that helps people with coding.
                    |
                    |You will be answering questions about the following code:
                    |
                    |$summary
                    |
                    |
                    |Response should use one or more code patches in diff format within ${CommandAutofixAction.tripleTilde}diff code blocks.
                    |Each diff should be preceded by a header that identifies the file being modified.
                    |The diff format should use + for line additions, - for line deletions.
                    |The diff should include 2 lines of context before and after every change.
                    |
                    |Example:
                    |
                    |Here are the patches:
                    |
                    |### src/utils/exampleUtils.js
                    |${CommandAutofixAction.tripleTilde}diff
                    | // Utility functions for example feature
                    | const b = 2;
                    | function exampleFunction() {
                    |-   return b + 1;
                    |+   return b + 2;
                    | }
                    |${CommandAutofixAction.tripleTilde}
                    |
                    |### tests/exampleUtils.test.js
                    |${CommandAutofixAction.tripleTilde}diff
                    | // Unit tests for exampleUtils
                    | const assert = require('assert');
                    | const { exampleFunction } = require('../src/utils/exampleUtils');
                    | 
                    | describe('exampleFunction', () => {
                    |-   it('should return 3', () => {
                    |+   it('should return 4', () => {
                    |     assert.equal(exampleFunction(), 3);
                    |   });
                    | });
                    |${CommandAutofixAction.tripleTilde}
                    |
                    |If needed, new files can be created by using code blocks labeled with the filename in the same manner.
                    """.trimMargin(),
            model = AppSettingsState.instance.defaultSmartModel()
        ).answer(
            listOf(
                """
                |The following command was run and produced an error:
                |
                |${CommandAutofixAction.tripleTilde}
                |${output.output}
                |${CommandAutofixAction.tripleTilde}
                |
                |Focus on and Fix the Error:
                |  ${error.message?.replace("\n", "\n  ") ?: ""}
                |${if (settings.additionalInstructions.isNotBlank()) "Additional Instructions:\n  ${settings.additionalInstructions}\n" else ""}
                """.trimMargin()
            ), api = api
        )
        var markdown = ui.socketManager?.addApplyFileDiffLinks(
            root = root.toPath(),
            response = response,
            ui = ui,
            api = api,
            shouldAutoApply = { path ->
                if (autoFix && !changed.contains(path)) {
                    changed.add(path)
                    true
                } else {
                    false
                }
            }
        )
        content.clear()
        content.append("<div>${renderMarkdown(markdown!!)}</div>")
    }

}