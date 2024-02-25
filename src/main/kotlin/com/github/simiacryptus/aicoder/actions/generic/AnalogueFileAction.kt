package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.ChatMessage
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import javax.swing.JTextArea

class AnalogueFileAction : FileContextAction<AnalogueFileAction.Settings>() {
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

  class Settings (
    var directive: String = ""
  )

  override fun getConfig(project: Project?): Settings? {
    return UITools.showDialog(
      project,
      SettingsUI::class.java,
      Settings::class.java,
      "Create Analogue File"
    )
  }

  override fun processSelection(state: SelectionState, config: Settings?): Array<File> {
    val analogue = generateFile(
      ProjectFile(
        path = state.projectRoot.toPath().relativize(state.selectedFile.toPath()).toString(),
        code = IOUtils.toString(FileInputStream(state.selectedFile), "UTF-8")
      ),
      config?.directive ?: ""
    )
    var outputPath = state.projectRoot.toPath().resolve(analogue.path)
    if (outputPath.toFile().exists()) {
      val extension = outputPath.toString().split(".").last()
      val name = outputPath.toString().split(".").dropLast(1).joinToString(".")
      val fileIndex = (1..Int.MAX_VALUE).find {
        !File(state.projectRoot, "$name.$it.$extension").exists()
      }
      outputPath = state.projectRoot.toPath().resolve("$name.$fileIndex.$extension")
    }
    outputPath.parent.toFile().mkdirs()
    FileUtils.write(outputPath.toFile(), analogue.code, "UTF-8")
    Thread.sleep(100)
    return arrayOf(outputPath.toFile())
  }

  private fun generateFile(baseFile: ProjectFile, directive: String): ProjectFile {
    val model = AppSettingsState.instance.defaultChatModel()
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
            Create a new file based on the following directive: $directive
            
            The file should be based on `${baseFile.path}` which contains the following code:
            
            ```
            ${baseFile.code}
            ```
            """.trimIndent().toContentList(), null
        )

      )
    )
    val response = api.chat(
      chatRequest,
      AppSettingsState.instance.defaultChatModel()
    ).choices.first().message?.content?.trim()
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
}
