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
import org.apache.commons.io.IOUtils
import java.io.File
import javax.swing.JTextArea

class AnalogueFileAction : BaseAction() {

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
            |""".trimMargin().trim(),
            /* rows = */ 3,
            /* columns = */ 120
        )
    }

    data class Settings(
        var directive: String = "",
    )

    override fun handle(e: AnActionEvent) {
        UITools.showDialog(e, SettingsUI::class.java, Settings::class.java, "Create Analogue File") { config ->
            handleImplement(e, config)
        }
    }

    private fun handleImplement(
        e: AnActionEvent,
        config: Settings,
    ) = Thread {
        val virtualFile = UITools.getSelectedFile(e) ?: return@Thread
        val project = e.project ?: return@Thread
        UITools.run(
            project, "Generating File", true
        ) {
            val projectRoot = File(project.basePath!!).toPath()
            val inputPath = projectRoot.relativize(File(virtualFile.canonicalPath!!).toPath())
            val analogue = try {
                val directive = config.directive
                val baseFile = ProjectFile(
                    path = inputPath.toString(),
                    code = IOUtils.toString(virtualFile.inputStream, "UTF-8")
                )
                generateFile(baseFile, directive)
            } finally {
                if (it.isCanceled) throw InterruptedException()
            }

            UITools.writeableFn(e) {
                try {
                    var path = analogue.path!!
                    var outputPath = projectRoot.resolve(path)
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
                    outputPath.toFile().writeText(analogue.code!!)
                    Thread.sleep(100)
                    val localFileSystem = LocalFileSystem.getInstance()
                    localFileSystem.findFileByIoFile(outputPath.toFile().parentFile)?.refresh(false, true)
                    val generatedFile = localFileSystem.findFileByIoFile(outputPath.toFile())
                    if (generatedFile == null) {
                        log.warn("Generated file not found: $path")
                    } else {
                        generatedFile.refresh(false, false)
                        FileEditorManager.getInstance(project).openFile(generatedFile, true)
                    }
                    Runnable { generatedFile?.delete(this@AnalogueFileAction) }
                } catch (e: Exception) {
                    log.warn("Error generating file", e)
                    throw RuntimeException(e)
                }
            }
        }
    }.start()

    private fun generateFile(
        baseFile: ProjectFile,
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
                    |You will combine natural language instructions with a user provided code example to create a new file.
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
                    |The file should be based on `${baseFile.path}` which contains the following code:
                    |
                    |```
                    |${baseFile.code}
                    |```
                """.trimMargin()
            )
        )
        val response = api.chat(
            chatRequest,
            AppSettingsState.instance.defaultChatModel()
        ).choices?.first()?.message?.content.orEmpty().trim()
        var outputPath = baseFile.path
        val header = response.split("\n").first()
        var body = response.split("\n").drop(1).joinToString("\n").trim()
        if (body.startsWith("```")) {
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
        val virtualFile = UITools.getSelectedFile(event) ?: return false
        if (virtualFile.isDirectory) return false
        return true
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(AnalogueFileAction::class.java)
    }
}

