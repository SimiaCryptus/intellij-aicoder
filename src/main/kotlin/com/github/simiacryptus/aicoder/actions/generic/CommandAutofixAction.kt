package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.diff.addSaveLinks
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
import javax.swing.*

class CommandAutofixAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(event: AnActionEvent) {
        val settings = getUserSettings(event) ?: return
        val dataContext = event.dataContext
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        val folder = UITools.getSelectedFolder(event)
        var root = if (null != folder) {
            folder.toFile.toPath()
        } else {
            event.project?.basePath?.let { File(it).toPath() }
        }!!

        val session = StorageInterface.newGlobalID()
        val patchApp = object : PatchApp(root.toFile(), session, settings) {
            override fun codeFiles() = getFiles(virtualFiles)
                .filter { it.toFile().length() < 1024 * 1024 / 2 } // Limit to 0.5MB
                .map { root.relativize(it) ?: it }.toSet()

            override fun codeSummary(paths: List<Path>): String = paths
                .filter { settings.workingDirectory?.resolve(it.toFile())?.exists() == true }
                .joinToString("\n\n") { path ->
                    """
                    |# ${path}
                    |$tripleTilde${path.toString().split('.').lastOrNull()}
                    |${settings.workingDirectory?.resolve(path.toFile())?.readText(Charsets.UTF_8)}
                    |$tripleTilde
                    """.trimMargin()
                }
            override fun projectSummary(): String {
                val codeFiles = codeFiles()
                val str = codeFiles
                    .asSequence()
                    .filter { settings.workingDirectory?.toPath()?.resolve(it)?.toFile()?.exists() == true }
                    .distinct().sorted()
                    .joinToString("\n") { path ->
                        "* ${path} - ${settings.workingDirectory?.toPath()?.resolve(path)?.toFile()?.length() ?: "?"} bytes".trim()
                    }
                return str
            }

            override fun output(task: SessionTask): OutputResult = run {
                val command = listOf(settings.executable.absolutePath) + settings.arguments.split(" ")
                val processBuilder = ProcessBuilder(command)
                processBuilder.directory(settings.workingDirectory)
                processBuilder.redirectErrorStream(true) // Merge standard error and standard output
                val buffer = StringBuilder()
                val taskOutput = task.add("")
                val process = processBuilder.start()
                val bufferedReader = process.inputStream.bufferedReader()
                while (process.isAlive) {
                    val line = bufferedReader.readLine()
                    buffer.append(line + "\n")
                    taskOutput?.set("<pre>\n$buffer\n</pre>")
                    task.append("", true)
                }
                task.append("", false)
                val exitCode = process.waitFor()
                val output = buffer.toString()
                taskOutput?.clear()
                OutputResult(exitCode, output)
            }
        }
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
    abstract inner class PatchApp(
        override val root: File,
        val session: Session,
        val settings: Settings,
    ) : ApplicationServer(
        applicationName = "Magic Code Fixer",
        path = "/fixCmd",
        showMenubar = false,
    ) {
        abstract fun codeFiles(): Set<Path>
        abstract fun codeSummary(paths: List<Path>): String
        abstract fun output(task: SessionTask): OutputResult
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
                        run(ui, newTask, session, settings)
                    }.start()
                    newTask.placeholder
                }
            )
            return socketManager
        }

        abstract fun projectSummary(): String
    }

    private fun PatchApp.run(
        ui: ApplicationInterface,
        task: SessionTask,
        session: Session,
        settings: Settings
    ) {
        val output = output(task)
        if (output.exitCode == 0 && settings.exitCodeOption == "nonzero") {
            task.complete(
                """
                |<div>
                |<div><b>Command executed successfully</b></div>
                |${renderMarkdown("${tripleTilde}\n${output.output}\n${tripleTilde}")}
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
                |${renderMarkdown("${tripleTilde}\n${output.output}\n${tripleTilde}")}
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
            |${renderMarkdown("${tripleTilde}\n${output.output}\n${tripleTilde}")}
            |</div>
            """.trimMargin()
            )
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
                    """.trimMargin(),
                model = AppSettingsState.instance.defaultSmartModel()
            ).answer(
                listOf(
                    """
                        |The following command was run and produced an error:
                        |
                        |$tripleTilde
                        |${output.output}
                        |$tripleTilde
                        |""".trimMargin()
                ), api = api
            )
            task.add(AgentPatterns.displayMapInTabs(
                mapOf(
                    "Text" to renderMarkdown(plan.text, ui = ui),
                    "JSON" to renderMarkdown(
                        "${tripleTilde}json\n${JsonUtil.toJson(plan.obj)}\n$tripleTilde",
                        ui = ui
                    ),
                )
            ))
            plan.obj.errors?.forEach { error ->
                val summary = codeSummary(
                    ((error.fixFiles ?: emptyList()) + (error.relatedFiles ?: emptyList())).map { File(it).toPath() })
                val response = SimpleActor(
                    prompt = """
                    |You are a helpful AI that helps people with coding.
                    |
                    |You will be answering questions about the following code:
                    |
                    |$summary
                    |
                    |
                    |Response should use one or more code patches in diff format within ${tripleTilde}diff code blocks.
                    |Each diff should be preceded by a header that identifies the file being modified.
                    |The diff format should use + for line additions, - for line deletions.
                    |The diff should include 2 lines of context before and after every change.
                    |
                    |Example:
                    |
                    |Here are the patches:
                    |
                    |### src/utils/exampleUtils.js
                    |${tripleTilde}diff
                    | // Utility functions for example feature
                    | const b = 2;
                    | function exampleFunction() {
                    |-   return b + 1;
                    |+   return b + 2;
                    | }
                    |$tripleTilde
                    |
                    |### tests/exampleUtils.test.js
                    |${tripleTilde}diff
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
                    |$tripleTilde
                    |
                    |If needed, new files can be created by using code blocks labeled with the filename in the same manner.
                    """.trimMargin(),
                    model = AppSettingsState.instance.defaultSmartModel()
                ).answer(
                    listOf(
                        """
                        |The following command was run and produced an error:
                        |
                        |${tripleTilde}
                        |${output.output}
                        |${tripleTilde}
                        |
                        |Focus on and Fix the Error:
                        |  ${error.message?.replace("\n", "\n  ") ?: ""}
                        |""".trimMargin()
                    ), api = api
                )
                var markdown = ui.socketManager?.addApplyFileDiffLinks(
                    root = root.toPath(),
                    code = {
                        val map = codeFiles().associateWith { root.resolve(it.toFile()).readText(Charsets.UTF_8) }
                        map
                    },
                    response = response,
                    handle = { newCodeMap ->
                        newCodeMap.forEach { (path, newCode) ->
                            task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                        }
                    },
                    ui = ui,
                )
                markdown = ui.socketManager?.addSaveLinks(
                    response = markdown!!,
                    task = task,
                    ui = ui,
                    handle = { path, newCode ->
                        root.resolve(path.toFile()).writeText(newCode, Charsets.UTF_8)
                    },
                )
                task.complete("<div>${renderMarkdown(markdown!!)}</div>")
            }
        } catch (e: Exception) {
            task.error(ui, e)
        }
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
        val fixFiles: List<String>? = null
    )


    data class Settings(
        var executable: File,
        var arguments: String = "",
        var workingDirectory: File? = null,
        var exitCodeOption: String = "0"
    )

    private fun getFiles(
        virtualFiles: Array<out VirtualFile>?
    ): MutableSet<Path> {
        val codeFiles = mutableSetOf<Path>()    // Set to avoid duplicates
        virtualFiles?.forEach { file ->
            if (file.isDirectory) {
                if(file.name.startsWith(".")) return@forEach
                if(Companion.isGitignore(file)) return@forEach
                codeFiles.addAll(getFiles(file.children))
            } else {
                codeFiles.add((file.toNioPath()))
            }
        }
        return codeFiles
    }

    private fun getUserSettings(event: AnActionEvent?): Settings? {
        val root = UITools.getSelectedFolder(event ?: return null)?.toNioPath() ?: event.project?.basePath?.let {
            File(
                it
            ).toPath()
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
            Settings(
                executable = executable,
                arguments = settingsUI.argumentsField.text,
                workingDirectory = File(settingsUI.workingDirectoryField.text),
                exitCodeOption = if (settingsUI.exitCodeZero.isSelected) "0" else if (settingsUI.exitCodeAny.isSelected) "any" else "nonzero"
            )
        } else {
            null
        }
    }

    class SettingsUI(root: File) {
        val argumentsField = JTextField("run build")
        val commandField = ComboBox<String>(AppSettingsState.instance.executables.toTypedArray()).apply {
            isEditable = true
            AppSettingsState.instance.executables.forEach { addItem(it) }
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
                }
                add(optionsPanel, BorderLayout.SOUTH)
            }
            return panel
        }
    }

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(CommandAutofixAction::class.java)
        val tripleTilde = "`" + "``" // This is a workaround for the markdown parser when editing this file
        fun isGitignore(file: VirtualFile): Boolean {
            var currentDir = file.toNioPath().toFile().parentFile
            currentDir ?: return false
            while (!currentDir.resolve(".git").exists()) {
                currentDir.resolve(".gitignore").let {
                    if (it.exists()) {
                        val gitignore = it.readText()
                        if (gitignore.split("\n").any { line ->
                            val pattern = line.trim().trimEnd('/').replace(".", "\\.").replace("*", ".*")
                            line.trim().isNotEmpty()
                                    && !line.startsWith("#")
                                    && file.name.trimEnd('/').matches(Regex(pattern))
                        }) {
                            return true
                        }
                    }
                }
                currentDir = currentDir.parentFile ?: return false
            }
            currentDir.resolve(".gitignore").let {
                if (it.exists()) {
                    val gitignore = it.readText()
                    if (gitignore.split("\n").any { line ->
                            val pattern = line.trim().trimEnd('/').replace(".", "\\.").replace("*", ".*")
                            line.trim().isNotEmpty()
                                    && !line.startsWith("#")
                                    && file.name.trimEnd('/').matches(Regex(pattern))
                        }) {
                        return true
                    }
                }
            }
            return false
        }
    }
}