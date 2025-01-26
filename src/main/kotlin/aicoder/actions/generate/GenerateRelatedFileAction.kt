package aicoder.actions.generate

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.config.Name
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.ChatMessage
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.awt.BorderLayout
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTextArea

class GenerateRelatedFileAction : aicoder.actions.FileContextAction<GenerateRelatedFileAction.Settings>() {
  private val log = Logger.getInstance(GenerateRelatedFileAction::class.java)

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  override fun isEnabled(event: AnActionEvent): Boolean {
    return UITools.getSelectedFiles(event).size == 1 && super.isEnabled(event)
  }

  data class ProjectFile(
    val path: String = "",
    val code: String = ""
  )

  class SettingsUI {
    @Name("Directive")
    var directive: JTextArea = JTextArea(3, 120).apply {
      lineWrap = true
      wrapStyleWord = true
      text = "Create README.md"
    }

    @Name("Recent Commands")
    var recentCommandsDropdown: JComboBox<String> = JComboBox()

    init {
      directive.addFocusListener(object : java.awt.event.FocusAdapter() {
        override fun focusGained(e: java.awt.event.FocusEvent) {
          directive.selectAll()
        }
      })

      val panel = createPanel()
      panel.add(createDirectivePanel(), BorderLayout.NORTH)
      panel.add(createRecentCommandsPanel(), BorderLayout.SOUTH)
      populateRecentCommands()
    }

    private fun createPanel(): JPanel {
      return JPanel(BorderLayout())
    }

    private fun createDirectivePanel(): JPanel {
      val directivePanel = JPanel()
      directivePanel.add(directive)
      return directivePanel
    }

    private fun createRecentCommandsPanel(): JPanel {
      val recentCommandsPanel = JPanel()
      recentCommandsPanel.add(recentCommandsDropdown)
      return recentCommandsPanel
    }

    private fun populateRecentCommands() {
      val recentCommands = AppSettingsState.instance.getRecentCommands("generate").getMostRecent()
      recentCommands.forEach { recentCommandsDropdown.addItem(it) }
    }
  }

  class UserSettings(
    var directive: String = "",
  )

  class Settings(
    val settings: UserSettings? = null,
    val project: Project? = null
  )

  override fun getConfig(project: Project?, e: AnActionEvent): Settings? {
    val userSettings = UITools.showDialog(
        project,
        SettingsUI::class.java,
        UserSettings::class.java,
        "Create Analogue File"
    )
    return if (userSettings != null) {
      Settings(userSettings, project)
    } else {
      null
    }
  }

  override fun processSelection(state: SelectionState, config: Settings?, progress: ProgressIndicator): Array<File> {
    if (config?.settings == null) {
      log.info("Action canceled by user.")
      return emptyArray()
    }
    try {
      progress.isIndeterminate = false
      progress.text = "Reading source file..."
      progress.fraction = 0.2
      val root = getModuleRootForFile(state.selectedFile).toPath()
      val selectedFile = state.selectedFile
      val analogue = generateFile(
        baseFile = ProjectFile(
          path = root.relativize(selectedFile.toPath()).toString(),
          code = IOUtils.toString(FileInputStream(selectedFile), "UTF-8")
        ),
        directive = config?.settings?.directive ?: "",
        progress = progress
      )
      progress.text = "Generating output file..."
      progress.fraction = 0.6
      var outputPath = root.resolve(analogue.path)
      if (outputPath.toFile().exists()) {
        val extension = outputPath.toString().split(".").last()
        val name = outputPath.toString().split(".").dropLast(1).joinToString(".")
        val fileIndex = (1..Int.MAX_VALUE).find {
          !root.resolve("$name.$it.$extension").toFile().exists()
        }
        outputPath = root.resolve("$name.$fileIndex.$extension")
      }
      progress.text = "Writing output file..."
      progress.fraction = 0.8
      outputPath.parent.toFile().mkdirs()
      FileUtils.write(outputPath.toFile(), analogue.code, "UTF-8")
      open(config?.project!!, outputPath)
      return arrayOf(outputPath.toFile())
    } catch (e: Exception) {
      log.error("Failed to generate related file", e)
      throw e
    }
  }

  private fun generateFile(baseFile: ProjectFile, directive: String, progress: ProgressIndicator): ProjectFile = try {
    progress.text = "Generating content with AI..."
    progress.fraction = 0.4
    val model = AppSettingsState.instance.smartModel.chatModel()
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
          Role.user, ("""
                              Create a new file based on the following directive: """.trimIndent() + directive + """
                              
                              The file should be based on `""".trimIndent() + baseFile.path + """` which contains the following code:
                              
                              ```
                              """.trimIndent() + baseFile.code + """
                              ```
                              """.trimIndent()).toContentList(), null
        )
      )
    )
    val response = api.chat(chatRequest, model).choices.firstOrNull()?.message?.content?.trim() ?: throw IllegalStateException("No response from API")
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
    ProjectFile(
      path = outputPath,
      code = body ?: ""
    )
  } catch (e: Exception) {
    throw e
  }

  companion object {
    fun open(project: Project, outputPath: Path) {
      val functionRef = AtomicReference<(() -> Unit)?>(null)
      val function: () -> Unit = {
        val file = outputPath.toFile()
        if (file.exists()) {
          // Ensure the IDE is ready for file operations
          ApplicationManager.getApplication().invokeLater {
            val ioFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            if (false == (ioFile?.let { FileEditorManager.getInstance(project).isFileOpen(it) })) {
              val localFileSystem = LocalFileSystem.getInstance()
              // Refresh the file system to ensure the file is visible
              val virtualFile = localFileSystem.refreshAndFindFileByIoFile(file)
              virtualFile?.let {
                FileEditorManager.getInstance(project).openFile(it, true)
              } ?: scheduledPool.schedule(functionRef.get()!!, 100, TimeUnit.MILLISECONDS)
            } else {
              scheduledPool.schedule(functionRef.get()!!, 100, TimeUnit.MILLISECONDS)
            }
          }
        } else {
          scheduledPool.schedule(functionRef.get()!!, 100, TimeUnit.MILLISECONDS)
        }
      }
      functionRef.set(function)
      scheduledPool.schedule(function, 100, TimeUnit.MILLISECONDS)
    }

  }
}