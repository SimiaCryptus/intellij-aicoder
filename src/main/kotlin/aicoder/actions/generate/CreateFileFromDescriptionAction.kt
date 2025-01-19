package aicoder.actions.generate

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.config.Name
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.ApiModel.*
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import java.io.File
import javax.swing.JTextArea

class CreateFileFromDescriptionAction : aicoder.actions.FileContextAction<CreateFileFromDescriptionAction.Settings>(false, true) {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  private val log = Logger.getInstance(CreateFileFromDescriptionAction::class.java)

  companion object {
    private const val DEFAULT_DIRECTIVE = "Create a new file"
    private const val FILE_PATH_REGEX = """File(?:name)?: ['`"]?([^'`"]+)['`"]?"""
  }

  class ProjectFile(var path: String = "", var code: String = "")

  class SettingsUI {
    @Name("Directive")
    val directive: JTextArea = JTextArea(
      DEFAULT_DIRECTIVE,
      3,
      120
    )
  }

  class Settings(
    var directive: String = "",
    val project: Project? = null
  )

  override fun getConfig(project: Project?, e: AnActionEvent): Settings? {
    val userSettings = UITools.showDialog(
      project,
      SettingsUI::class.java,
      Settings::class.java,
      "Create File From Description"
    ) ?: return null
    return Settings(userSettings.directive, project)
  }

  override fun processSelection(
    state: SelectionState,
    config: Settings?,
    progress: ProgressIndicator
  ): Array<File> {
    progress.isIndeterminate = false
    progress.text = "Generating file from description..."
    return try {
      processSelectionInternal(state, config, progress)
    } catch (e: Exception) {
      log.error("Failed to create file from description", e)
      UITools.showErrorDialog(
        "Failed to create file: ${e.message}",
        "Error"
      )
      emptyArray()
    }
  }

  private fun processSelectionInternal(
    state: SelectionState,
    config: Settings?,
    progress: ProgressIndicator
  ): Array<File> {
    require(state.projectRoot.exists()) { "Project root directory does not exist" }
    require(state.selectedFile.exists()) { "Selected file does not exist" }

    val projectRoot = state.projectRoot.toPath()
    val inputPath = projectRoot.relativize(state.selectedFile.toPath()).toString()
    val pathSegments = inputPath.split("/").toList()
    val updirSegments = pathSegments.takeWhile { it == ".." }
    val moduleRoot = projectRoot.resolve(pathSegments.take(updirSegments.size * 2).joinToString("/"))
    val filePath = pathSegments.drop(updirSegments.size * 2).joinToString("/")
    progress.text = "Generating file content..."
    progress.fraction = 0.3

    val generatedFile = generateFile(filePath, config?.directive ?: DEFAULT_DIRECTIVE)

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
    progress.text = "Writing file to disk..."
    progress.fraction = 0.8

    outputPath.parent.toFile().mkdirs()
    WriteCommandAction.runWriteCommandAction(config?.project) { outputPath.toFile().writeText(generatedFile.code) }
    Thread.sleep(100)

    return arrayOf(outputPath.toFile())
  }

  private fun generateFile(
    basePath: String,
    directive: String
  ): ProjectFile {
    require(directive.isNotBlank()) { "Directive cannot be empty" }
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
    try {
      val response = api.chat(
        chatRequest,
        AppSettingsState.instance.smartModel.chatModel()
      ).choices.firstOrNull()?.message?.content?.trim() ?: throw IllegalStateException("Empty response from AI")
      var outputPath = basePath
      val header = response.lines().firstOrNull() ?: throw IllegalStateException("Invalid response format")
      var body = response.lines().drop(1).joinToString("\n").trim().lines()
        .dropWhile { it.startsWith("```") }
        .dropLastWhile { it.startsWith("```") }
        .joinToString("\n")
      val pathPattern = FILE_PATH_REGEX.toRegex()
      if (pathPattern.matches(header)) {
        val match = pathPattern.matchEntire(header)!!
        outputPath = match.groupValues[1]
      }
      require(body.isNotBlank()) { "Generated file content cannot be empty" }
      return ProjectFile(path = outputPath, code = body)
    } catch (e: Exception) {
      log.error("Failed to generate file content", e)
      throw IllegalStateException("Failed to generate file content: ${e.message}", e)
    }
  }
}