package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.OpenAIClient.ChatMessage
import com.simiacryptus.openai.OpenAIClient.ChatRequest

import javax.swing.*

class CreateFileAction extends FileContextAction<Settings> {

    CreateFileAction() {
        super(false, true)
    }

    static class ProjectFile {
        public String path = ""
        public String code = ""

        ProjectFile() {
        }
    }

    static class SettingsUI {
        @Name("Directive")
        public JTextArea directive = new JTextArea(
            /* text = */ """
                Create a default log4j configuration file
                """.stripIndent().trim(),
            /* rows = */ 3,
            /* columns = */ 120
        )

        SettingsUI() {
        }
    }

    static class Settings {
        public String directive = ""

        Settings() {
        }
    }

    @Override
    File[] processSelection(
        SelectionState state,
        Settings config
    ) {
        def projectRoot = state.projectRoot.toPath()
        def inputPath = projectRoot.relativize(state.selectedFile.toPath()).toString()
        def pathSegments = inputPath.split("/").toList()
        def updirSegments = pathSegments.takeWhile { it == ".." }.toList()
        def moduleRoot = projectRoot.resolve(pathSegments.take(updirSegments.size() * 2).join("/"))
        def filePath = pathSegments.drop(updirSegments.size() * 2).join("/")

        def generatedFile = generateFile(filePath, config?.directive ?: "")

        def path = generatedFile.path
        def outputPath = moduleRoot.resolve(path)
        if (outputPath.toFile().exists()) {
            def extension = path.split(".").last()
            def name = path.split(".").init().join(".")
            def fileIndex = (1..Integer.MAX_VALUE).find {
                !new File("$name.$it.$extension").exists()
            }
            path = "$name.$fileIndex.$extension"
            outputPath = projectRoot.resolve(path)
        }
        outputPath.parent.toFile().mkdirs()
        outputPath.toFile().text = generatedFile.code
        Thread.sleep(100)

        return [outputPath.toFile()] as File[]
    }

    private ProjectFile generateFile(
        String basePath,
        String directive
    ) {
        def chatRequest = new ChatRequest()
        def model = AppSettingsState.instance.defaultChatModel()
        chatRequest.model = model.modelName
        chatRequest.temperature = AppSettingsState.instance.temperature
        chatRequest.messages = [
            //language=TEXT
            new ChatMessage(
                ChatMessage.Role.system, """
                    You will interpret natural language requirements to create a new file.
                    Provide a new filename and the code to be written to the file.
                    Paths should be relative to the project root and should not exist.
                    Output the file path using the a line with the format "File: <path>".
                    Output the file code directly after the header line with no additional decoration.
                """.stripIndent(), null
            ),
            //language=TEXT
            new ChatMessage(
                ChatMessage.Role.user, """
                    Create a new file based on the following directive: $directive
                    
                    The file location should be based on the selected path `${basePath}`
                """.stripIndent(), null
            )
        ]
        def response = api.chat(
            chatRequest,
            AppSettingsState.instance.defaultChatModel()
        ).choices?.first()?.message?.content?.trim()
        def outputPath = basePath
        def header = response.split("\n").first()
        def body = response.split("\n").drop(1).join("\n").trim()
        if (body.startsWith("```")) {
            // Remove beginning ``` (optionally ```language) and ending ```
            body = body.split("\n").drop(1).init().join("\n").trim()
        }
        def pathPattern = ~"""File(?:name)?: ['`"]?([^'`"]+)['`"]?"""
        if (header =~ pathPattern) {
            def match = (header =~ pathPattern)[0]
            outputPath = match[1].toString()
        }
        return new ProjectFile(
            path: outputPath,
            code: body
        )
    }

    @Override
    Settings getConfig(Project project) {
        return UITools.showDialog(
            project,
            SettingsUI,
            Settings,
            "Create File from Requirements", {}
        )
    }
}