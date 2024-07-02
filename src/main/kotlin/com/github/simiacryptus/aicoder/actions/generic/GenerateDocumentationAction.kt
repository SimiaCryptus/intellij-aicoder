package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.actions.test.TestResultAutofixAction
import com.github.simiacryptus.aicoder.actions.test.TestResultAutofixAction.Companion
import com.github.simiacryptus.aicoder.actions.test.TestResultAutofixAction.Companion.findGitRoot
import com.github.simiacryptus.aicoder.actions.test.TestResultAutofixAction.Companion.getProjectStructure
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import org.apache.commons.io.IOUtils
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.*


class GenerateDocumentationAction : FileContextAction<GenerateDocumentationAction.Settings>() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.getSelectedFile(event)?.isDirectory == false) return false
        return super.isEnabled(event)
    }

    class SettingsUI {
        @Name("Single Output File")
        val singleOutputFile = JCheckBox("Produce a single output file", true)

        @Name("Files to Process")
        val filesToProcess = CheckBoxList<Path>()

        @Name("AI Instruction")
        val transformationMessage = JBTextArea(4, 40)

        @Name("Output File")
        val outputFilename = JBTextField()

        @Name("Output Directory")
        val outputDirectory = JBTextField()
    }

    class UserSettings(
        var transformationMessage: String = "Create user documentation",
        var outputFilename: String = "compiled_documentation.md",
        var filesToProcess: List<Path> = listOf(),
        var singleOutputFile: Boolean = true,
        var outputDirectory: String = "docs/"
    )

    class Settings(
        val settings: UserSettings? = null,
        val project: Project? = null
    )

    override fun getConfig(project: Project?, e: AnActionEvent): Settings {
        val root = UITools.getSelectedFolder(e)?.toNioPath()
        val files = Files.walk(root)
            .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
            .toList().filterNotNull().toTypedArray()
        val settingsUI = SettingsUI().apply {
            filesToProcess.setItems(files.toMutableList()) { path ->
                root?.relativize(path)?.toString() ?: path.toString()
            }
            files.forEach { path ->
                filesToProcess.setItemSelected(path, true)
            }
            outputDirectory.text = "docs/"
        }
        val dialog = DocumentationCompilerDialog(project, settingsUI)
        dialog.show()
        val settings: UserSettings = dialog.userSettings
        settings.singleOutputFile = settingsUI.singleOutputFile.isSelected
        settings.outputDirectory = settingsUI.outputDirectory.text
        val result = dialog.isOK
        settings.filesToProcess = when {
            result -> files.filter { path -> settingsUI.filesToProcess.isItemSelected(path) }.toList()
            else -> listOf()
        }
        //.map { path -> return@map root?.resolve(path) }.filterNotNull()
        return Settings(settings, project)
    }

    override fun processSelection(state: SelectionState, config: Settings?): Array<File> {
        val selectedFolder = state.selectedFile.toPath()
        val gitRoot = TestResultAutofixAction.findGitRoot(selectedFolder) ?: selectedFolder
        val outputDirectory = config?.settings?.outputDirectory ?: "docs/"
        var outputPath =
            selectedFolder.resolve(config?.settings?.outputFilename ?: "compiled_documentation.md")
        val relativePath = gitRoot.relativize(outputPath)
        outputPath =  gitRoot.resolve(outputDirectory).resolve(relativePath)
        if (outputPath.toFile().exists()) {
            val extension = outputPath.toString().split(".").last()
            val name = outputPath.toString().split(".").dropLast(1).joinToString(".")
            val fileIndex = (1..Int.MAX_VALUE).find {
                !selectedFolder.resolve("$name.$it.$extension").toFile().exists()
            }
            outputPath = selectedFolder.resolve("$name.$fileIndex.$extension")
        }
        val executorService = Executors.newFixedThreadPool(4)
        outputPath.parent.toFile().mkdirs()
        val transformationMessage = config?.settings?.transformationMessage ?: "Create user documentation"
        val markdownContent = StringBuilder()
        try {
            val selectedPaths = config?.settings?.filesToProcess ?: listOf()
            val partitionedPaths = Files.walk(selectedFolder)
                .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
                .toList().groupBy { selectedPaths.contains(it) }
            val pathList = partitionedPaths[true]!!
                .toList().filterNotNull()
                .map<Path, Future<Path>> { path ->
                    executorService.submit<Path?> {
                        val fileContent =
                            IOUtils.toString(FileInputStream(path.toFile()), "UTF-8") ?: return@submit null
                        val transformContent = transformContent(path, fileContent, transformationMessage)
                        if (config?.settings?.singleOutputFile == true) {
                            markdownContent.append("# ${selectedFolder.relativize(path)}\n\n")
                            markdownContent.append(transformContent.replace("(?s)(?<![^\\n])#".toRegex(), "\n##"))
                        } else {
                            var individualOutputPath = /*selectedFolder*/ selectedFolder.relativize(path.parent.resolve(
                                path.fileName.toString().split('.').dropLast(1)
                                    .joinToString(".") + "." + outputPath.fileName
                            ))
                            individualOutputPath = selectedFolder.resolve(individualOutputPath)
                            individualOutputPath = gitRoot.relativize(individualOutputPath)
                            individualOutputPath = gitRoot.resolve(outputDirectory).resolve(individualOutputPath)
                            individualOutputPath.parent.toFile().mkdirs()
                            Files.write(individualOutputPath, transformContent.toByteArray())
                        }
                        path
                    }
                }.toTypedArray().map { future ->
                    try {
                        future.get()
                    } catch (e: Exception) {
                        log.warn("Error processing file", e)
                        return@map null
                    }
                }.filterNotNull()
            if (config?.settings?.singleOutputFile == true) {
                Files.write(outputPath, markdownContent.toString().toByteArray())
                open(config?.project!!, outputPath)
                return arrayOf(outputPath.toFile())
            } else {
                open(config?.project!!, selectedFolder.resolve(outputDirectory))
                return pathList.toList().map { it.toFile() }.toTypedArray()
            }
        } finally {
            executorService.shutdown()
        }
    }

    private fun transformContent(path: Path, fileContent: String, transformationMessage: String) = api.chat(
        ApiModel.ChatRequest(
            model = AppSettingsState.instance.smartModel.chatModel().modelName,
            temperature = AppSettingsState.instance.temperature,
            messages = listOf(
                ApiModel.ChatMessage(
                    ApiModel.Role.system, """
                        You will combine natural language instructions with a user provided code example to document code.
                        """.trimIndent().toContentList(), null
                ),
                ApiModel.ChatMessage(
                    ApiModel.Role.user,
                    """
                    |## Project:
                    |${findGitRoot(path)?.let { getProjectStructure(it) }}
                    |
                    |## $path:
                    |```
                    |$fileContent
                    |```
                    |
                    |Instructions: $transformationMessage
                    """.trimMargin().toContentList()
                ),
            ),
        ),
        AppSettingsState.instance.smartModel.chatModel()
    ).choices.first().message?.content?.trim() ?: fileContent

    fun findGitRoot(path: Path?): Path? {
        var current: Path? = path
        while (current != null) {
            if (current.resolve(".git").toFile().exists()) {
                return current
            }
            current = current.parent
        }
        return null
    }

    companion object {
        private val scheduledPool = Executors.newScheduledThreadPool(1)
        fun open(project: Project, outputPath: Path) {
            lateinit var function: () -> Unit
            function = {
                val file = outputPath.toFile()
                if (file.exists()) {
                    // Ensure the IDE is ready for file operations
                    ApplicationManager.getApplication().invokeLater {
                        val ioFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                        if (false == (ioFile?.let { FileEditorManager.getInstance(project).isFileOpen(it) })) {
                            val localFileSystem = LocalFileSystem.getInstance()
                            // Refresh the file system to ensure the file is visible
                            val virtualFile = localFileSystem.refreshAndFindFileByIoFile(file)
                            virtualFile?.let {
                                FileEditorManager.getInstance(project).openFile(it, true)
                            } ?: scheduledPool.schedule(function, 100, TimeUnit.MILLISECONDS)
                        } else {
                            scheduledPool.schedule(function, 100, TimeUnit.MILLISECONDS)
                        }
                    }
                } else {
                    scheduledPool.schedule(function, 100, TimeUnit.MILLISECONDS)
                }
            }
            scheduledPool.schedule(function, 100, TimeUnit.MILLISECONDS)
        }
    }

    class DocumentationCompilerDialog(project: Project?, private val settingsUI: SettingsUI) : DialogWrapper(project) {
        val userSettings = UserSettings()

        init {
            title = "Compile Documentation"
            // Set the default values for the UI elements from userSettings
            settingsUI.transformationMessage.text = userSettings.transformationMessage
            settingsUI.outputFilename.text = userSettings.outputFilename
            settingsUI.outputDirectory.text = userSettings.outputDirectory
            init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout()).apply {
                val filesScrollPane = JBScrollPane(settingsUI.filesToProcess).apply {
                    preferredSize = Dimension(400, 300) // Adjust the preferred size as needed
                }
                add(filesScrollPane, BorderLayout.CENTER) // Make the files list the dominant element

                val optionsPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    add(JLabel("AI Instruction"))
                    add(settingsUI.transformationMessage)
                    add(JLabel("Output File"))
                    add(settingsUI.outputFilename)
                    add(JLabel("Output Directory"))
                    add(settingsUI.outputDirectory)
                    add(settingsUI.singleOutputFile)
                }
                add(optionsPanel, BorderLayout.SOUTH)
            }
            return panel
        }

        override fun doOKAction() {
            super.doOKAction()
            userSettings.transformationMessage = settingsUI.transformationMessage.text
            userSettings.outputFilename = settingsUI.outputFilename.text
            userSettings.outputDirectory = settingsUI.outputDirectory.text
            // Assuming filesToProcess already reflects the user's selection
//          userSettings.filesToProcess = settingsUI.filesToProcess.selectedValuesList
            userSettings.filesToProcess =
                settingsUI.filesToProcess.items.filter { path -> settingsUI.filesToProcess.isItemSelected(path) }
            userSettings.singleOutputFile = settingsUI.singleOutputFile.isSelected
        }
    }
}

val <T> CheckBoxList<T>.items: List<T>
    get() {
        val items = mutableListOf<T>()
        for (i in 0 until model.size) {
            items.add(getItemAt(i)!!)
        }
        return items
    }