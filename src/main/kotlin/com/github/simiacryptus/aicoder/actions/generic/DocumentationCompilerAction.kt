package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.JTextArea

class DocumentationCompilerAction : FileContextAction<DocumentationCompilerAction.Settings>() {

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (UITools.getSelectedFile(event)?.isDirectory == false) return false
    return super.isEnabled(event)
  }

  class SettingsUI {

    @Name("Files to Process")
    var filesToProcessScrollPane: JBScrollPane = JBScrollPane()
  }

  class UserSettings(
    var transformationMessage: String = "Create user documentation",
    var outputFilename: String = "compiled_documentation.md",
    var filesToProcess: List<Path> = listOf(),
  )

  class Settings(
    val settings: UserSettings? = null,
    val project: Project? = null
  )

  override fun getConfig(project: Project?, e: AnActionEvent): Settings {
    val root = UITools.getSelectedFolder(e)?.toNioPath()
    val filesToProcess: CheckBoxList<Path> = CheckBoxList()
    val files = Files.walk(root)
      .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
      .toList().filterNotNull().toTypedArray()
    filesToProcess.setItems(files.toMutableList()) { path ->
      root?.relativize(path)?.toString() ?: path.toString()
    }
    files.forEach { path ->
      filesToProcess.setItemSelected(path, true)
    }
    val settingsUI = SettingsUI().apply {
      filesToProcessScrollPane.setViewportView(filesToProcess)
    }
    val settings: UserSettings = UITools.showDialog2(
      project,
      settingsUI,
      UserSettings::class.java,
      "Compile Documentation"
    ) { }
    settings.filesToProcess = files.filter { path -> filesToProcess.isItemSelected(path) }.toList()
    //.map { path -> return@map root?.resolve(path) }.filterNotNull()
    return Settings(settings, project)
  }

  override fun processSelection(state: SelectionState, config: Settings?): Array<File> {
    val root = state.selectedFile.toPath()
    var outputPath = root.resolve(config?.settings?.outputFilename ?: "compiled_documentation.md")
    if (outputPath.toFile().exists()) {
      val extension = outputPath.toString().split(".").last()
      val name = outputPath.toString().split(".").dropLast(1).joinToString(".")
      val fileIndex = (1..Int.MAX_VALUE).find {
        !root.resolve("$name.$it.$extension").toFile().exists()
      }
      outputPath = root.resolve("$name.$fileIndex.$extension")
    }
    val executorService = Executors.newFixedThreadPool(4)
    outputPath.parent.toFile().mkdirs()
    val transformationMessage = config?.settings?.transformationMessage ?: "Create user documentation"
    val markdownContent = StringBuilder()
    try {
      val selectedPaths = config?.settings?.filesToProcess ?: listOf()
      val partitionedPaths = Files.walk(root)
        .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
        .toList().groupBy { selectedPaths.contains(it) }
      val pathList = partitionedPaths[true]!!
        .toList().filterNotNull()
        .map<Path, Future<Path>> { path ->
          executorService.submit<Path?> {
            val fileContent = IOUtils.toString(FileInputStream(path.toFile()), "UTF-8") ?: return@submit null
            val transformContent = transformContent(fileContent, transformationMessage)
            markdownContent.append("# ${root.relativize(path)}\n\n")
            markdownContent.append(transformContent.replace("(?s)(?<![^\\n])#".toRegex(), "\n##"))
            markdownContent.append("\n\n")
            path
          }
        }.toTypedArray().map { future ->
          try {
            future.get() ?: return@map null
          } catch (e: Exception) {
            log.warn("Error processing file", e)
            return@map null
          }
        }.filterNotNull()
      Files.write(outputPath, markdownContent.toString().toByteArray())
      open(config?.project!!, outputPath)
      return arrayOf(outputPath.toFile())
    } finally {
      executorService.shutdown()
    }
  }

  private fun transformContent(fileContent: String, transformationMessage: String) = api.chat(
    ApiModel.ChatRequest(
      model = AppSettingsState.instance.defaultChatModel().modelName,
      temperature = AppSettingsState.instance.temperature,
      messages = listOf(
        ApiModel.ChatMessage(
          ApiModel.Role.system, """
            You will combine natural language instructions with a user provided code example to document code.
            """.trimIndent().toContentList(), null
        ),
        ApiModel.ChatMessage(ApiModel.Role.user, fileContent.toContentList()),
        ApiModel.ChatMessage(ApiModel.Role.user, transformationMessage.toContentList()),
      ),
    ),
    AppSettingsState.instance.defaultChatModel()
  ).choices.first().message?.content?.trim() ?: fileContent

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
}
