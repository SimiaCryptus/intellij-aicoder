package aicoder.actions.generate

import aicoder.actions.test.TestResultAutofixAction
import aicoder.actions.test.TestResultAutofixAction.Companion.getProjectStructure
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.config.Name
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import org.apache.commons.io.IOUtils
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.swing.*


class GenerateDocumentationAction : aicoder.actions.FileContextAction<GenerateDocumentationAction.Settings>() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (UITools.getSelectedFile(event)?.isDirectory == false) return false
    return super.isEnabled(event)
  }

  class SettingsUI {
    @Name("Single Output File")
    val singleOutputFile = JCheckBox("Produce a single output file", true)

    @Name("Files to Process")
    val filesToProcess = CheckBoxList<Path>()

    @Name("AI Instruction")
    val transformationMessage = JBTextArea(4, 40)

    @Name("Recent Instructions")
    val recentInstructions = JComboBox<String>()

    @Name("Output File")
    val outputFilename = JBTextField()

    @Name("Output Directory")
    val outputDirectory = JBTextField()
  }

  class UserSettings(
    var transformationMessage: String = "Create user documentation",
    var outputFilename: String = "compiled_documentation.md",
    var filesToProcess: List<Path> = listOf(),
    var singleOutputFile: Boolean = true,
    var outputDirectory: String = "docs/"
  )

  class Settings(
    val settings: UserSettings? = null,
    val project: Project? = null
  )

  override fun getConfig(project: Project?, e: AnActionEvent): Settings? {
    val root = UITools.getSelectedFolder(e)?.toNioPath()
    val files = Files.walk(root)
      .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
      .toList().filterNotNull().sortedBy { it.toString() }.toTypedArray()
    val settingsUI = SettingsUI().apply {
      filesToProcess.setItems(files.toMutableList()) { path ->
        root?.relativize(path)?.toString() ?: path.toString()
      }
      files.forEach { path ->
        filesToProcess.setItemSelected(path, true)
      }
      outputDirectory.text = "docs/"
    }
    val mruDocumentationInstructions = AppSettingsState.instance.getRecentCommands("DocumentationInstructions")
    settingsUI.recentInstructions.model = DefaultComboBoxModel(
      mruDocumentationInstructions.getMostRecent(10).map {
        "${it.split(" ").first()} ${it.split(" ").drop(1).joinToString(" ")}"
      }.toTypedArray()
    )
    settingsUI.recentInstructions.selectedIndex = -1
    settingsUI.recentInstructions.addActionListener { updateUIFromSelection(settingsUI) }
    val dialog = DocumentationCompilerDialog(project, settingsUI)
    dialog.show()
    val settings: UserSettings = dialog.userSettings
    settings.singleOutputFile = settingsUI.singleOutputFile.isSelected
    settings.outputDirectory = settingsUI.outputDirectory.text
    val result = dialog.isOK
    settings.filesToProcess = when {
      result -> files.filter { path -> settingsUI.filesToProcess.isItemSelected(path) }.sortedBy { it.toString() }.toList()
      else -> listOf()
    }
    if (settings.filesToProcess.isEmpty()) return null
    mruDocumentationInstructions.addInstructionToHistory("${settings.outputFilename} ${settings.transformationMessage}")
    //.map { path -> return@map root?.resolve(path) }.filterNotNull()
    return Settings(settings, project)
  }

  private fun updateUIFromSelection(settingsUI: SettingsUI) {
    val selected = settingsUI.recentInstructions.selectedItem as? String
    if (selected != null) {
      val parts = selected.split(" ", limit = 2)
      if (parts.size == 2) {
        settingsUI.outputFilename.text = parts[0]
        settingsUI.transformationMessage.text = parts[1]
      } else {
        settingsUI.transformationMessage.text = selected
      }
    } else {
      settingsUI.transformationMessage.text = ""
    }
  }

  override fun processSelection(state: SelectionState, config: Settings?, progress: ProgressIndicator): Array<File> {
    progress.fraction = 0.0
    if (config?.settings == null) {
      // Dialog was cancelled, return empty array
      return emptyArray<File>().also {
        // Ensure we don't attempt to open any files when dialog is cancelled
        return@also
      }
    }
    progress.text = "Initializing documentation generation..."

    val selectedFolder = state.selectedFile.toPath()
    val gitRoot = TestResultAutofixAction.findGitRoot(selectedFolder) ?: selectedFolder
    val outputDirectory = config.settings.outputDirectory
    var outputPath =
      selectedFolder.resolve(config.settings.outputFilename)
    val relativePath = gitRoot.relativize(outputPath)
    outputPath = gitRoot.resolve(outputDirectory).resolve(relativePath)
    if (outputPath.toFile().exists()) {
      val extension = outputPath.toString().split(".").last()
      val name = outputPath.toString().split(".").dropLast(1).joinToString(".")
      val fileIndex = (1..Int.MAX_VALUE).find {
        !selectedFolder.resolve("$name.$it.$extension").toFile().exists()
      }
      outputPath = selectedFolder.resolve("$name.$fileIndex.$extension")
    }
    val executorService = Executors.newFixedThreadPool(4)
    val transformationMessage = config.settings.transformationMessage
    val markdownContent = TreeMap<String, String>()
    try {
      val selectedPaths = config.settings.filesToProcess.sortedBy { it.toString() }
      val partitionedPaths = Files.walk(selectedFolder)
        .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
        .toList().sortedBy { it.toString() }.groupBy { selectedPaths.contains(it) }
      val totalFiles = partitionedPaths[true]?.size ?: 0
      var processedFiles = 0
      val pathList = partitionedPaths[true]
        ?.toList()?.filterNotNull()
        ?.map<Path, Future<Path>> { path ->
          executorService.submit<Path?> {
            var retries = 0
            val maxRetries = 3
            while (retries < maxRetries) {
              try {
                val fileContent =
                  IOUtils.toString(FileInputStream(path.toFile()), "UTF-8") ?: return@submit null
                val transformContent = transformContent(path, fileContent, transformationMessage)
                processTransformedContent(
                  path,
                  transformContent,
                  config,
                  selectedFolder,
                  gitRoot,
                  outputDirectory,
                  outputPath,
                  markdownContent
                )
                synchronized(progress) {
                  processedFiles++
                  progress.fraction = processedFiles.toDouble() / totalFiles
                  progress.text = "Processing file ${processedFiles} of ${totalFiles}"
                }
                return@submit path
              } catch (e: Exception) {
                retries++
                if (retries >= maxRetries) {
                  log.error("Failed to process file after $maxRetries attempts: $path", e)
                  return@submit null
                }
                log.warn("Error processing file: $path. Retrying (attempt $retries)", e)
                Thread.sleep(1000L * retries) // Exponential backoff
              }
            }
            null
          }
        }?.toTypedArray()?.map { future ->
          try {
            future.get(2, TimeUnit.MINUTES) // Set a timeout for each file processing
          } catch (e: Exception) {
            when (e) {
              is TimeoutException -> log.error("File processing timed out", e)
              else -> log.error("Error processing file", e)
            }
            null
          }
        }?.filterNotNull() ?: listOf()
      if (config.settings.singleOutputFile == true) {
        val sortedContent = markdownContent.entries.joinToString("\n\n") { (path, content) ->
          "# $path\n\n$content"
        }
        outputPath.parent.toFile().mkdirs()
        outputPath.parent.toFile().mkdirs()
        Files.write(outputPath, sortedContent.toByteArray())
        open(config.project!!, outputPath)
        return arrayOf(outputPath.toFile())
      } else {
        val outputDir = selectedFolder.resolve(outputDirectory)
        outputDir.toFile().mkdirs()
        open(config.project!!, selectedFolder.resolve(outputDirectory))
        return pathList.map { it.toFile() }.toTypedArray()
      }
    } finally {
      executorService.shutdown()
    }
  }

  private fun processTransformedContent(
    path: Path,
    transformContent: String,
    config: Settings?,
    selectedFolder: Path,
    gitRoot: Path,
    outputDirectory: String,
    outputPath: Path,
    markdownContent: TreeMap<String, String>
  ) {
    if (config?.settings?.singleOutputFile == true) {
      markdownContent[selectedFolder.relativize(path).toString()] = transformContent.replace("(?s)(?<![^\\n])#".toRegex(), "\n##")
    } else {
      var individualOutputPath = /*selectedFolder*/ selectedFolder.relativize(
        path.parent.resolve(
          path.fileName.toString().split('.').dropLast(1)
            .joinToString(".") + "." + outputPath.fileName
        )
      )
      individualOutputPath = selectedFolder.resolve(individualOutputPath)
      individualOutputPath = gitRoot.relativize(individualOutputPath)
      individualOutputPath = gitRoot.resolve(outputDirectory).resolve(individualOutputPath)
      individualOutputPath.parent.toFile().mkdirs()
      Files.write(individualOutputPath, transformContent.toByteArray())
    }
  }

  private fun transformContent(path: Path, fileContent: String, transformationMessage: String) = api.chat(
    ApiModel.ChatRequest(
      model = AppSettingsState.instance.smartModel,
      temperature = AppSettingsState.instance.temperature,
      messages = listOf(
        ApiModel.ChatMessage(
          ApiModel.Role.system, """
                        You will combine natural language instructions with a user provided code example to document code.
                        """.trimIndent().toContentList(), null
        ),
        ApiModel.ChatMessage(
          ApiModel.Role.user,
          "## Project:\n${findGitRoot(path)?.let { getProjectStructure(it) }}\n\n## $path:\n```\n$fileContent\n```\n\nInstructions: $transformationMessage".toContentList()
        ),
      ),
    ),
    AppSettingsState.instance.smartModel.chatModel()
  ).choices.first().message?.content?.trim() ?: fileContent

  fun findGitRoot(path: Path?): Path? {
    var current: Path? = path
    while (current != null) {
      if (current.resolve(".git").toFile().exists()) {
        return current
      }
      current = current.parent
    }
    return null
  }

  companion object {
    private val scheduledPool = Executors.newScheduledThreadPool(1)
    fun open(project: Project, outputPath: Path) {
      lateinit var function: () -> Unit
      function = {
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
              } ?: scheduledPool.schedule(function, 100, TimeUnit.MILLISECONDS)
            } else {
              scheduledPool.schedule(function, 100, TimeUnit.MILLISECONDS)
            }
          }
        } else {
          scheduledPool.schedule(function, 100, TimeUnit.MILLISECONDS)
        }
      }
      scheduledPool.schedule(function, 100, TimeUnit.MILLISECONDS)
    }
  }

  inner class DocumentationCompilerDialog(project: Project?, private val settingsUI: SettingsUI) : DialogWrapper(project) {
    val userSettings = UserSettings()

    init {
      title = "Compile Documentation"
      // Set the default values for the UI elements from userSettings
      settingsUI.transformationMessage.text = userSettings.transformationMessage
      settingsUI.outputFilename.text = userSettings.outputFilename
      settingsUI.outputDirectory.text = userSettings.outputDirectory
      settingsUI.singleOutputFile.isSelected = userSettings.singleOutputFile
      settingsUI.recentInstructions.addActionListener {
        val selected = settingsUI.recentInstructions.selectedItem as? String
        selected?.let {
          updateUIFromSelection(settingsUI)
        }
      }
      init()
    }

    override fun createCenterPanel(): JComponent {
      val panel = JPanel(BorderLayout()).apply {
        val filesScrollPane = JBScrollPane(settingsUI.filesToProcess).apply {
          preferredSize = Dimension(600, 400) // Increase the size for better visibility
        }
        add(filesScrollPane, BorderLayout.CENTER) // Make the files list the dominant element

        val optionsPanel = JPanel().apply {
          layout = BoxLayout(this, BoxLayout.Y_AXIS)
          border = BorderFactory.createEmptyBorder(10, 10, 10, 10) // Add some padding
          add(JLabel("Recent Instructions"))
          add(settingsUI.recentInstructions)
          add(Box.createVerticalStrut(10))
          add(JLabel("AI Instruction"))
          add(settingsUI.transformationMessage)
          add(Box.createVerticalStrut(10))
          add(Box.createVerticalStrut(10)) // Add some vertical spacing
          add(JLabel("Output File"))
          add(settingsUI.outputFilename)
          add(Box.createVerticalStrut(10))
          add(JLabel("Output Directory"))
          add(settingsUI.outputDirectory)
          add(Box.createVerticalStrut(10))
          add(settingsUI.singleOutputFile)
        }
        add(optionsPanel, BorderLayout.SOUTH)
      }
      return panel
    }

    override fun doOKAction() {
      if (!validateInput()) {
        return
      }
      super.doOKAction()
      userSettings.transformationMessage = settingsUI.transformationMessage.text
      userSettings.outputFilename = settingsUI.outputFilename.text
      userSettings.outputDirectory = settingsUI.outputDirectory.text
      // Assuming filesToProcess already reflects the user's selection
//          userSettings.filesToProcess = settingsUI.filesToProcess.selectedValuesList
      userSettings.filesToProcess =
        settingsUI.filesToProcess.items.filter { path -> settingsUI.filesToProcess.isItemSelected(path) }
      userSettings.singleOutputFile = settingsUI.singleOutputFile.isSelected
    }

    private fun validateInput(): Boolean {
      if (settingsUI.transformationMessage.text.isBlank()) {
        Messages.showErrorDialog("AI Instruction cannot be empty", "Input Error")
        return false
      }
      if (settingsUI.outputFilename.text.isBlank()) {
        Messages.showErrorDialog("Output File cannot be empty", "Input Error")
        return false
      }
      if (settingsUI.outputDirectory.text.isBlank()) {
        Messages.showErrorDialog("Output Directory cannot be empty", "Input Error")
        return false
      }
      return true
    }
  }
}

val <T> CheckBoxList<T>.items: List<T>
  get() {
    val items = mutableListOf<T>()
    for (i in 0 until model.size) {
      items.add(getItemAt(i)!!)
    }
    return items
  }