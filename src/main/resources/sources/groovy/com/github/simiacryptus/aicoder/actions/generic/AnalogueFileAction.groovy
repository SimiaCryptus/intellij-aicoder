package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.OpenAIClient.ChatMessage
import com.simiacryptus.openai.OpenAIClient.ChatRequest
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import javax.swing.*
import java.nio.file.Path

class AnalogueFileAction extends FileContextAction<AnalogueFileAction.Settings> {

    private static class ProjectFile {
        public String path = ""
        public String code = ""
        public ProjectFile() {
        }
    }

    @SuppressWarnings("UNUSED")
    static class SettingsUI {
        @Name("Directive")
        public JTextArea directive = new JTextArea(
                /* text = */ """
                                Create test cases
                                """.stripIndent().trim(),
                /* rows = */ 3,
                /* columns = */ 120
        )
    }

    static class Settings {
        public String directive = ""
        public Settings() {
        }
    }

    @Override
    Settings getConfig(Project project) {
        return UITools.showDialog(
                project,
                SettingsUI.class,
                Settings.class,
                "Create Analogue File",
                {}
        )
    }

    @Override
    File[] processSelection(SelectionState state, Settings config) {
        ProjectFile analogue = generateFile(
                new ProjectFile(
                        path: state.projectRoot.toPath().relativize(state.selectedFile.toPath()),
                        code: IOUtils.toString(new FileInputStream(state.selectedFile), "UTF-8")
                ),
                config?.directive ?: ""
        )
        Path outputPath = state.projectRoot.toPath().resolve(analogue.path)
        if (outputPath.toFile().exists()) {
            String extension = outputPath.toString().split("\\.").last()
            String name = outputPath.toString().split("\\.").dropLast(1).joinToString(".")
            int fileIndex = (1..Integer.MAX_VALUE).find {
                !new File(state.projectRoot, "$name.$it.$extension").exists()
            }
            outputPath = state.projectRoot.toPath().resolve("$name.$fileIndex.$extension")
        }
        outputPath.parent.toFile().mkdirs()
        FileUtils.write(outputPath.toFile(), analogue.code, "UTF-8")
        Thread.sleep(100)
        return [outputPath.toFile()]
    }

    private ProjectFile generateFile(ProjectFile baseFile, String directive) {
        def chatRequest = new ChatRequest()
        def model = AppSettingsState.instance.defaultChatModel()
        chatRequest.model = model.modelName
        chatRequest.max_tokens = model.maxTokens
        chatRequest.temperature = AppSettingsState.instance.temperature
        chatRequest.messages = [
                new ChatMessage(
                        ChatMessage.Role.system, """
                            You will combine natural language instructions with a user provided code example to create a new file.
                            Provide a new filename and the code to be written to the file.
                            Paths should be relative to the project root and should not exist.
                            Output the file path using the a line with the format "File: <path>".
                            Output the file code directly after the header line with no additional decoration.
                            """.stripIndent()
                ),
                new ChatMessage(
                        ChatMessage.Role.user, """
                            Create a new file based on the following directive: $directive
                            
                            The file should be based on `${baseFile.path}` which contains the following code:
                            
                            ```
                            ${baseFile.code}
                            ```
                            """.stripIndent()
                )
        ]
        String response = api.chat(
                chatRequest,
                AppSettingsState.instance.defaultChatModel()
        ).choices?.first()?.message?.content?.trim()
        String outputPath = baseFile.path
        String header = response.split("\n").first()
        String body = response.split("\n").drop(1).join("\n").trim()
        if (body.contains("```")) {
            body = body.split("```.*").drop(1).first().trim()
        }
        def pathPattern = ~"""File(?:name)?: ['`"]?([^'`"]+)['`"]?"""
        def matcher = pathPattern.matcher(header)
        if (matcher.find()) {
            outputPath = matcher.group(1).trim()
        }
        return new ProjectFile(
                path: outputPath,
                code: body
        )
    }
}