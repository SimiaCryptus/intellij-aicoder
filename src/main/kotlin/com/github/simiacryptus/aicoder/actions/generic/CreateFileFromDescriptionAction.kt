package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.simiacryptus.jopenai.models.ApiModel.*
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import java.io.File

class CreateFileFromDescriptionAction : FileContextAction<CreateFileFromDescriptionAction.Settings>(false, true) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    class ProjectFile(var path: String = "", var code: String = "")

    class Settings(var directive: String = "")

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

        val generatedFile = generateFile(filePath, config?.directive ?: "")

        var path = generatedFile.path
        var outputPath = moduleRoot.resolve(path)
        if (outputPath.toFile().exists()) {
            val extension = path.substringAfterLast(".")
            val name = path.substringBeforeLast(".")
            val fileIndex = (1..Int.MAX_VALUE).find {
                !File("$name.$it.$extension").exists()
            }
            path = "$name.$fileIndex.$extension"
            outputPath = projectRoot.resolve(path)
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
        var body = response.lines().drop(1).joinToString("\n").trim()
        if (body.startsWith("```")) {
            // Remove beginning ``` (optionally ```language) and ending ```
            body = body.split("\n").drop(1).joinToString("\n").trim()
        }
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