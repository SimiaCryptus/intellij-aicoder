package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.ChatMessage
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javax.swing.JTextArea

class GenerateRelatedFileAction : FileContextAction<GenerateRelatedFileAction.Settings>() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.getSelectedFile(event)?.isDirectory == true) return false
        return super.isEnabled(event)
    }

    data class ProjectFile(
        val path: String = "",
        val code: String = ""
    )

    class SettingsUI {
        @Name("Directive")
        var directive: JTextArea = JTextArea(
            """
            Create test cases
            """.trimIndent(),
            3,
            120
        )
    }

    class UserSettings(
        var directive: String = "",
    )

    class Settings(
        val settings: UserSettings? = null,
        val project: Project? = null
    )

    override fun getConfig(project: Project?, e: AnActionEvent): Settings {
        return Settings(
            UITools.showDialog(
                project,
                SettingsUI::class.java,
                UserSettings::class.java,
                "Create Analogue File"
            ), project
        )
    }

    override fun processSelection(state: SelectionState, config: Settings?): Array<File> {
        val root = getModuleRootForFile(state.selectedFile).toPath()
        val selectedFile = state.selectedFile
        val analogue = generateFile(
            ProjectFile(
                path = root.relativize(selectedFile.toPath()).toString(),
                code = IOUtils.toString(FileInputStream(selectedFile), "UTF-8")
            ),
            config?.settings?.directive ?: ""
        )
        var outputPath = root.resolve(analogue.path)
        if (outputPath.toFile().exists()) {
            val extension = outputPath.toString().split(".").last()
            val name = outputPath.toString().split(".").dropLast(1).joinToString(".")
            val fileIndex = (1..Int.MAX_VALUE).find {
                !root.resolve("$name.$it.$extension").toFile().exists()
            }
            outputPath = root.resolve("$name.$fileIndex.$extension")
        }
        outputPath.parent.toFile().mkdirs()
        FileUtils.write(outputPath.toFile(), analogue.code, "UTF-8")
        open(config?.project!!, outputPath)
        return arrayOf(outputPath.toFile())
    }

    private fun generateFile(baseFile: ProjectFile, directive: String): ProjectFile {
        val model = AppSettingsState.instance.smartModel.chatModel()
        val chatRequest = ApiModel.ChatRequest(
            model = model.modelName,
            temperature = AppSettingsState.instance.temperature,
            messages = listOf(
                ChatMessage(
                    Role.system, """
            You will combine natural language instructions with a user provided code example to create a new file.
            Provide a new filename and the code to be written to the file.
            Paths should be relative to the project root and should not exist.
            Output the file path using the a line with the format "File: <path>".
            Output the file code directly after the header line with no additional decoration.
            """.trimIndent().toContentList(), null
                ),
                ChatMessage(
                    Role.user, """
            |Create a new file based on the following directive: $directive
            |
            |The file should be based on `${baseFile.path}` which contains the following code:
            |
            |```
            |${baseFile.code.let { /*escapeHtml4*/it/*.indent("  ")*/ }}
            |```
            """.trimMargin().toContentList(), null
                )

            )
        )
        val response = api.chat(chatRequest, model).choices.first().message?.content?.trim()
        var outputPath = baseFile.path
        val header = response?.split("\n")?.first()
        var body = response?.split("\n")?.drop(1)?.joinToString("\n")?.trim()
        if (body?.contains("```") == true) {
            body = body.split("```.*".toRegex()).drop(1).firstOrNull()?.trim() ?: body
        }
        val pathPattern = "File(?:name)?: ['\"]?([^'\"]+)['\"]?".toRegex()
        val matcher = pathPattern.find(header ?: "")
        if (matcher != null) {
            outputPath = matcher.groupValues[1].trim()
        }
        return ProjectFile(
            path = outputPath,
            code = body ?: ""
        )
    }

    companion object {
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
}
