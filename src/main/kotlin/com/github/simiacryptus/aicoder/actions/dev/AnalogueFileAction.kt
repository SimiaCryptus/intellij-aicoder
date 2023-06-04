package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore.loadText
import com.simiacryptus.openai.ChatMessage
import com.simiacryptus.openai.ChatRequest
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.Brain
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
            """
            |Create test cases
            |""".trimMargin().trim()
        )
    }

    data class Settings(
        var directive: String = "",
    )

    override fun actionPerformed(e: AnActionEvent) {
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
            val analogue = try {
                val directive = config.directive
                val baseFile = ProjectFile(
                    path = virtualFile.canonicalPath!!,
                    code = IOUtils.toString(virtualFile.inputStream, "UTF-8")
                )
                generateFile(baseFile, directive)
            } finally {
                if (it.isCanceled) throw InterruptedException()
            }

            UITools.writeableFn(e) {
                var path = analogue.path!!
                var file = File(path)
                if (file.exists()) {
                    val extension = path.split(".").takeLast(1)
                    val name = path.split(".").dropLast(1).joinToString(".")
                    val fileIndex = (1 until Int.MAX_VALUE).first {
                        !File("$name.$it.$extension").exists()
                    }
                    path = "$fileIndex.$it.$fileIndex"
                    file = File(path)
                }
                file.writeText(analogue.code!!)
                LocalFileSystem.getInstance().findFileByIoFile(File(path).parentFile)?.refresh(false, true)
                val generatedFile = LocalFileSystem.getInstance().findFileByIoFile(File(path))
                if (generatedFile == null) {
                    log.warn("Generated file not found: $path")
                } else {
                    generatedFile.refresh(false, false)
                    FileEditorManager.getInstance(project).openFile(generatedFile, true)
                }
                Runnable { generatedFile?.delete(this@AnalogueFileAction) }
            }
        }

    }.start()

    private fun generateFile(
        baseFile: ProjectFile,
        directive: String,
    ): ProjectFile {
        val api = OpenAIClient(
                key = AppSettingsState.instance.apiKey,
                apiBase = AppSettingsState.instance.apiBase,
                logLevel = AppSettingsState.instance.apiLogLevel
            )
        val chatRequest = ChatRequest()
        chatRequest.model = AppSettingsState.instance.model_chat
        chatRequest.max_tokens = AppSettingsState.instance.maxTokens
        chatRequest.temperature = AppSettingsState.instance.temperature
        chatRequest.messages = arrayOf(
                //language=TEXT
                ChatMessage(
                    ChatMessage.Role.system, """
                    |You will combine natural language instructions with a user provided code example to create a new file.
                    |Provide a new filename and the code to be written to the file.
                    |Paths should be relative to the project root and should not exist.
                    |Output the file path using the a line with the format "File: <path>".
                    |Output the file code in ``` code blocks labeled with language where appropriate.
                """.trimMargin()
                ),
                //language=TEXT
                ChatMessage(
                    ChatMessage.Role.user, """
                    |Create a new file based on the following directive: $directive
                    |
                    |The file should be based on `${baseFile.path}` which contains the following code:
                    |```
                    |${baseFile.code}
                    |```
                """.trimMargin()
                )
            )
        val response = api.chat(chatRequest).response.get().toString()
        val codeBlocks = Brain.extractCodeBlocks(response).filter { it.first != "text" }
        require(codeBlocks.size == 1) { "Expected 1 code block, but found ${codeBlocks.size}" }
        var outputPath = baseFile.path
        val pathPattern = Regex("""(?s)\n?File(?:name)?: (?:'|`|")?([^\n]+)(?:'|`|")\n""")
        if (pathPattern.containsMatchIn(response)) {
                val match = pathPattern.find(response)!!
                outputPath = match.groupValues[1]
            }
        return ProjectFile(
                path = outputPath,
                code = codeBlocks[0].second
            )
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        return AppSettingsState.instance.devActions
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(AnalogueFileAction::class.java)
    }
}
