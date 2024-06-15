package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.diff.addSaveLinks
import com.simiacryptus.skyenet.Retryable
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
import java.awt.Dimension
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*

class CommandAutofixAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(event: AnActionEvent) {
        val settings = getUserSettings(event) ?: return
        var root: Path? = null
        val codeFiles: MutableSet<Path> = mutableSetOf()
        fun codeSummary() = codeFiles.filter {
            root!!.resolve(it).toFile().exists()
        }.associateWith {
            root!!.resolve(it).toFile().readText(Charsets.UTF_8)
        }.entries.joinToString("\n\n") { (path, code) ->
            val extension = path.toString().split('.').lastOrNull()?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }
            """
			# $path
			```$extension
			${code.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
			```
			""".trimMargin()
        }

        val dataContext = event.dataContext
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        val folder = UITools.getSelectedFolder(event)
        root = if (null != folder) {
            folder.toFile.toPath()
        } else {
            getModuleRootForFile(UITools.getSelectedFile(event)?.parent?.toFile ?: throw RuntimeException("")).toPath()
        }
        val files = getFiles(virtualFiles, root!!)
        codeFiles.addAll(files)

        fun output(): OutputResult =
            run {
                val command = listOf(settings.executable.absolutePath) + settings.arguments.split(" ")
                val processBuilder = ProcessBuilder(command)
                processBuilder.directory(settings.workingDirectory)
                processBuilder.redirectErrorStream(true) // Merge standard error and standard output

                val process = processBuilder.start()
                val exitCode = process.waitFor()
                val output = process.inputStream.bufferedReader().readText()
                OutputResult(exitCode, output)
            }


        val session = StorageInterface.newGlobalID()
        val patchApp = PatchApp(root!!.toFile(), ::codeSummary, codeFiles, ::output, session)
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
    inner class PatchApp(
        override val root: File,
        val codeSummary: () -> String,
        val codeFiles: Set<Path> = setOf(),
        val output: () -> OutputResult,
        val session: Session,
    ) : ApplicationServer(
        applicationName = "Magic Code Fixer",
        path = "/fixCmd",
        showMenubar = false,
    ) {
        override val singleInput = true
        override val stickyInput = false
        override fun newSession(user: User?, session: Session): SocketManager {
            val socketManager = super.newSession(user, session)
            val ui = (socketManager as ApplicationSocketManager).applicationInterface
            val task = ui.newTask()
            val tripleTilde = "`"+"``" // This is a workaround for the markdown parser when editing this file
            Retryable(
                ui = ui,
                task = task,
                process = { content ->
                    val newTask = ui.newTask(false)
                    newTask.add("Running Command")
                    Thread {
                        run(content, tripleTilde, ui, newTask, session)
                    }.start()
                    newTask.placeholder
                }
            ).apply {
                set(label(size), process(container))
            }
            return socketManager
        }
    }

    private fun PatchApp.run(
        content: StringBuilder,
        tripleTilde: String,
        ui: ApplicationInterface,
        task: SessionTask,
        session: Session
    ): String {
        val output = output()
        content.set("""<div>${renderMarkdown("```\n${output}\n```")}</div>""")
        if (output.exitCode == 0) {
            return """
                                |<div>
                                |<div><b>Command executed successfully</b></div>
                                |${renderMarkdown("```\n${output}\n```")}
                                |</div>
                                |""".trimMargin()
        }
        val response = SimpleActor(
            prompt = """
                            |You are a helpful AI that helps people with coding.
                            |
                            |You will be answering questions about the following code:
                            |
                            |${codeSummary()}
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
                            |${tripleTilde}
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
                            |${tripleTilde}
                            |
                            |If needed, new files can be created by using code blocks labeled with the filename in the same manner.
                            """.trimMargin(),
            model = AppSettingsState.instance.defaultSmartModel()
        ).answer(
            listOf(
                """
                            |The following command was run and produced an error:
                            |
                            |$tripleTilde
                            |$output
                            |${tripleTilde}
                            |""".trimMargin()
            ), api = api
        )
        var markdown = ui.socketManager?.addApplyFileDiffLinks(
            root = root.toPath(),
            code = { codeFiles.associateWith { root.resolve(it.toFile()).readText(Charsets.UTF_8) } },
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
        return """
                        |<div>${renderMarkdown("```\n${output}\n```")}</div>
                        |<div>${renderMarkdown(markdown!!)}</div>
                        |""".trimMargin()
    }

    data class Settings(
        var executable: File,
        var arguments: String = "",
        var filesToProcess: List<Path> = listOf(),
        var workingDirectory: File? = null
    )

    private fun getFiles(
        virtualFiles: Array<out VirtualFile>?, root: Path
    ): MutableSet<Path> {
        val codeFiles = mutableSetOf<Path>()    // Set to avoid duplicates
        virtualFiles?.forEach { file ->
            if (file.isDirectory) {
                codeFiles.addAll(getFiles(file.children, root))
            } else {
                codeFiles.add(root.relativize(file.toNioPath()))
            }
        }
        return codeFiles
    }

    private fun getUserSettings(event: AnActionEvent?): Settings? {
        val root = UITools.getSelectedFolder(event ?: return null)?.toNioPath() ?: event.project?.basePath?.let { File(it).toPath() }
        val files = UITools.getSelectedFiles(event).map { it.path.let { File(it).toPath() } }.toMutableSet()
        if(files.isEmpty()) Files.walk(root)
            .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
            .toList().filterNotNull().forEach { files.add(it) }
        val settingsUI = SettingsUI(root!!.toFile()).apply {
            filesToProcess.setItems(files.toMutableList()) { path ->
                root?.relativize(path)?.toString() ?: path.toString()
            }
            files.forEach { path ->
                filesToProcess.setItemSelected(path, true)
            }
        }
        val dialog = CommandSettingsDialog(event?.project, settingsUI)
        dialog.show()
        return if (dialog.isOK) {
            val executable = File(settingsUI.commandField.text)
            AppSettingsState.instance.toolExecutable = executable.absolutePath
            Settings(
                executable = File(settingsUI.commandField.text),
                arguments = settingsUI.argumentsField.text,
                filesToProcess = files.filter { path -> settingsUI.filesToProcess.isItemSelected(path) }.toList(),
                workingDirectory = File(settingsUI.workingDirectoryField.text)
            )
        } else {
            null
        }
    }

    class SettingsUI(root: File) {
        val argumentsField = JTextField("run build")
        val filesToProcess = CheckBoxList<Path>()
        val commandField = JTextField(AppSettingsState.instance.toolExecutable).apply {
            isEditable = false
        }
        val commandButton = JButton("...").apply {
            addActionListener {
                val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = false
        }
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    commandField.text = fileChooser.selectedFile.absolutePath
                }
            }
        }
        val workingDirectoryField = JTextField(root.absolutePath).apply {
            isEditable = false
        }
        val workingDirectoryButton = JButton("...").apply {
            addActionListener {
                val fileChooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isMultiSelectionEnabled = false
                }
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    workingDirectoryField.text = fileChooser.selectedFile.absolutePath
                }
            }
        }
    }

    class CommandSettingsDialog(project: Project?, private val settingsUI: SettingsUI) : DialogWrapper(project) {
        init {
            title = "Command Autofix Settings"
            init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout()).apply {
                val filesScrollPane = JBScrollPane(settingsUI.filesToProcess).apply {
                    preferredSize = Dimension(400, 300) // Adjust the preferred size as needed
                }
                add(JLabel("Files to Process"), BorderLayout.NORTH)
                add(filesScrollPane, BorderLayout.CENTER) // Make the files list the dominant element

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
                }
                add(optionsPanel, BorderLayout.SOUTH)
            }
            return panel
        }
    }

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(CommandAutofixAction::class.java)
    }
}