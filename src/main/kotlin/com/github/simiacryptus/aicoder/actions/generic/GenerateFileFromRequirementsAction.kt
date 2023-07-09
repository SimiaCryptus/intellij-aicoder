package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.simiacryptus.openai.APIClientBase
import com.simiacryptus.openai.OpenAIClient.ChatMessage
import com.simiacryptus.openai.OpenAIClient.ChatRequest
import java.io.File
import java.nio.file.Path
import javax.swing.JTextArea

class GenerateFileFromRequirementsAction : BaseAction() {


    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(GenerateFileFromRequirementsAction::class.java)
    }

    private data class ProjectFile(
        var path: String? = "",
        var code: String? = "",
    )

    @Suppress("UNUSED")
    class SettingsUI {
        @Name("Directive")
        val directive = JTextArea(
            /* text = */ """
                |Create test cases
                """.trimMargin().trim(),
            /* rows = */ 3,
            /* columns = */ 120
        )
    }

    data class Settings(
        var directive: String = "",
    )

    override fun handle(e: AnActionEvent) {
        UITools.showDialog(e, SettingsUI::class.java, Settings::class.java, "Create File from Requirements") { config ->
            handleImplement(e, config)
        }
    }

    private fun handleImplement(
        e: AnActionEvent,
        config: Settings,
    ) = Thread {
        val virtualFile = UITools.getSelectedFolder(e) ?: return@Thread
        // Get module root

        val project = e.project ?: return@Thread

        UITools.run(
            project, "Generating File", true
        ) {
            val projectRoot: Path = File(project.basePath!!).toPath()
            val inputPath = projectRoot.relativize(File(virtualFile.canonicalPath!!).toPath()).toString()
            val pathSegments = inputPath.split("/").toTypedArray()
            val updirSegments = pathSegments.takeWhile { it == ".." }.toTypedArray()
            val moduleRoot = projectRoot.resolve(pathSegments.take(updirSegments.size * 2).joinToString { "/" })
            val filePath = pathSegments.drop(updirSegments.size * 2).joinToString { "/" }

            val generatedFile = try {
                generateFile(filePath, config.directive)
            } finally {
                if (it.isCanceled) throw InterruptedException()
            }

            UITools.writeableFn(e) {
                try {
                    var path = generatedFile.path!!
                    var outputPath = moduleRoot.resolve(path)
                    if (outputPath.toFile().exists()) {
                        val extension = path.split(".").takeLast(1).first()
                        val name = path.split(".").dropLast(1).joinToString(".")
                        val fileIndex = (1 until Int.MAX_VALUE).first {
                            !File("$name.$it.$extension").exists()
                        }
                        path = "$name.$fileIndex.$extension"
                        outputPath = projectRoot.resolve(path)
                    }
                    outputPath.parent.toFile().mkdirs()
                    outputPath.toFile().writeText(generatedFile.code!!)
                    Thread.sleep(100)
                    val localFileSystem = LocalFileSystem.getInstance()
                    localFileSystem.findFileByIoFile(outputPath.toFile().parentFile)?.refresh(false, true)
                    val newFile = localFileSystem.findFileByIoFile(outputPath.toFile())
                    if (newFile == null) {
                        log.warn("Generated file not found: $path")
                    } else {
                        newFile.refresh(false, false)
                        FileEditorManager.getInstance(project).openFile(newFile, true)
                    }
                    Runnable { newFile?.delete(this@GenerateFileFromRequirementsAction) }
                } catch (e: Exception) {
                    log.warn("Error generating file", e)
                    throw RuntimeException(e)
                }
            }

        }
    }.start()

    private fun generateFile(
        basePath: String,
        directive: String,
    ): ProjectFile {
        val api = IdeaOpenAIClient.api
        val chatRequest = ChatRequest()
        val model = AppSettingsState.instance.defaultChatModel()
        chatRequest.model = model.modelName
        chatRequest.max_tokens = model.maxTokens
        chatRequest.temperature = AppSettingsState.instance.temperature
        chatRequest.messages = arrayOf(
            //language=TEXT
            ChatMessage(
                ChatMessage.Role.system, """
                |You will interpret natural language requirements to create a new file.
                |Provide a new filename and the code to be written to the file.
                |Paths should be relative to the project root and should not exist.
                |Output the file path using the a line with the format "File: <path>".
                |Output the file code directly after the header line with no additional decoration.
                """.trimMargin()
            ),
            //language=TEXT
            ChatMessage(
                ChatMessage.Role.user, """
                |Create a new file based on the following directive: $directive
                |
                |The file location should be based on the selected path `${basePath}`
                """.trimMargin()
            )
        )
        val response = api.chat(
            chatRequest,
            AppSettingsState.instance.defaultChatModel()
        ).choices?.first()?.message?.content.orEmpty().trim()
        var outputPath = basePath
        val header = response.split("\n").first()
        var body = response.split("\n").drop(1).joinToString("\n").trim()
        if (body.startsWith("```")) {
            // Remove beginning ``` (optionally ```language) and ending ```
            body = body.split("\n").drop(1).dropLast(1).joinToString("\n").trim()
        }
        val pathPattern = Regex("""File(?:name)?: ['`"]?([^'`"]+)['`"]?""")
        if (pathPattern.containsMatchIn(header)) {
            val match = pathPattern.find(header)!!
            outputPath = match.groupValues[1].trim()
        }
        return ProjectFile(
            path = outputPath,
            code = body
        )
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (APIClientBase.isSanctioned()) return false
        if (null != UITools.getSelectedFile(event)) return false
        val virtualFolder = UITools.getSelectedFolder(event) ?: return false
        if (!virtualFolder.isDirectory) return false
        return true
    }

}
