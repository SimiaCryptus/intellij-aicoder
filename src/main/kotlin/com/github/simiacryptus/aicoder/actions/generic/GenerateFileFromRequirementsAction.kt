package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.openai.OpenAIClient.ChatMessage
import com.simiacryptus.openai.OpenAIClient.ChatRequest
import com.simiacryptus.openai.OpenAIClient
import java.io.File
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

    override fun actionPerformed(e: AnActionEvent) {
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
            val moduleRoot = getModuleRoot(project, virtualFile)
            val projectRoot = File(moduleRoot).toPath()
            val inputPath = projectRoot.relativize(File(virtualFile.canonicalPath!!).toPath())
            val generatedFile = try {
                generateFile(inputPath.toString(), config.directive)
            } finally {
                if (it.isCanceled) throw InterruptedException()
            }

            UITools.writeableFn(e) {
                try {
                    var path = generatedFile.path!!
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

    private fun getModuleRoot(project: Project, virtualFile: VirtualFile): String {
        val moduleRoots = getModuleRoots(project)
        val moduleRoot = moduleRoots.firstOrNull { virtualFile.path.startsWith(it) }
        return moduleRoot ?: moduleRoots.first()
    }

    private fun getModuleRoots(project: Project): List<String> {

        project.actualComponentManager

        val moduleRoots = mutableListOf<String>()
        val projectRoot = File(project.basePath!!)
        val projectRootPath = projectRoot.toPath()
        val moduleFiles = projectRoot.listFiles()!!
        for (moduleFile in moduleFiles) {
            if (moduleFile.isDirectory) {
                val moduleRootPath = moduleFile.toPath()
                if (moduleRootPath.startsWith(projectRootPath)) {
                    moduleRoots.add(moduleRootPath.toString())
                    log.info("Module root: ${moduleRootPath.toString()}")
                }
            }
        }
        return moduleRoots
    }

    private fun generateFile(
        basePath: String,
        directive: String,
    ): ProjectFile {
        val api = OpenAIClient(
            key = AppSettingsState.instance.apiKey,
            apiBase = AppSettingsState.instance.apiBase,
        )
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
        val response = api.chat(chatRequest).choices?.first()?.message?.content.orEmpty().trim()
        var outputPath = basePath
        val header = response.split("\n").first()
        var body = response.split("\n").drop(1).joinToString("\n").trim()
        if(body.startsWith("```")) {
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
        if(UITools.isSanctioned()) return false
        if(null != UITools.getSelectedFile(event)) return false
        val virtualFolder = UITools.getSelectedFolder(event) ?: return false
        if (!virtualFolder.isDirectory) return false
        return true
    }

}