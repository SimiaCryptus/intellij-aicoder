package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.models.ApiModel.*
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import java.io.File
import javax.swing.JTextArea

class CreateFileFromDescriptionAction : FileContextAction<CreateFileFromDescriptionAction.Settings>(false, true) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    class ProjectFile(var path: String = "", var code: String = "")

    class SettingsUI {
        @Name("Directive")
        var directive: JTextArea = JTextArea(
            """
            Create a new file
            """.trimIndent(),
            3,
            120
        )
    }

    class Settings(
        var directive: String = "",
        val project: Project? = null
    )

    override fun getConfig(project: Project?, e: AnActionEvent): Settings {
        val userSettings = UITools.showDialog(
            project,
            SettingsUI::class.java,
            Settings::class.java,
            "Create File From Description"
        )
        return Settings(userSettings?.directive ?: "", project)
    }

    override fun processSelection(
        state: SelectionState,
        config: Settings?
    ): Array<File> {
        val projectRoot = state.projectRoot.toPath()
        val inputPath = projectRoot.relativize(state.selectedFile.toPath()).toString()
        val pathSegments = inputPath.split("/").toList()
        val updirSegments = pathSegments.takeWhile { it == ".." }
        val moduleRoot = projectRoot.resolve(pathSegments.take(updirSegments.size * 2).joinToString("/"))
        val filePath = pathSegments.drop(updirSegments.size * 2).joinToString("/")

        val generatedFile = generateFile(filePath, config?.directive ?: "Create a new file")

        var path = generatedFile.path
        var outputPath = moduleRoot.resolve(path)
        if (outputPath.toFile().exists()) {
            val extension = path.substringAfterLast(".")
            val name = path.substringBeforeLast(".")
            val fileIndex = (1..Int.MAX_VALUE).find {
                !File("$name.$it.$extension").exists()
            }
            path = "$name.$fileIndex.$extension"
            outputPath = moduleRoot.resolve(path)
        } else {
            outputPath = moduleRoot.resolve(path)
        }
        outputPath.parent.toFile().mkdirs()
        outputPath.toFile().writeText(generatedFile.code)
        Thread.sleep(100)

        return arrayOf(outputPath.toFile())
    }

    private fun generateFile(
        basePath: String,
        directive: String
    ): ProjectFile {
        val model = AppSettingsState.instance.smartModel.chatModel()
        val chatRequest = ChatRequest(
            model = model.modelName,
            temperature = AppSettingsState.instance.temperature,
            messages = listOf(
                ChatMessage(
                    Role.system, """
                    You will interpret natural language requirements to create a new file.
                    Provide a new filename and the code to be written to the file.
                    Paths should be relative to the project root and should not exist.
                    Output the file path using the a line with the format "File: <path>".
                    Output the file code directly after the header line with no additional decoration.
                """.trimIndent().toContentList(), null
                ),
                ChatMessage(
                    Role.user, """
                    Create a new file based on the following directive: $directive
                    
                    The file location should be based on the selected path `$basePath`
                """.trimIndent().toContentList(), null
                )
            )
        )
        val response = api.chat(
            chatRequest,
            AppSettingsState.instance.smartModel.chatModel()
        ).choices.first().message?.content?.trim() ?: ""
        var outputPath = basePath
        val header = response.lines().first()
        var body = response.lines().drop(1).joinToString("\n").trim().lines().dropWhile { it.startsWith("```") }.dropLastWhile { it.startsWith("```") }.joinToString("\n")
        val pathPattern = """File(?:name)?: ['`"]?([^'`"]+)['`"]?""".toRegex()
        if (pathPattern.matches(header)) {
            val match = pathPattern.matchEntire(header)!!
            outputPath = match.groupValues[1]
        }
        return ProjectFile(
            path = outputPath,
            code = body
        )
    }
}