package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
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

  class SettingsUI {
    @Name("Transformation Message")
    var transformationMessage: JTextArea = JTextArea(
      "Create user documentation",
      3,
      120
    )
  }

  class UserSettings(
    var transformationMessage: String = "Create user documentation",
  )

  class Settings(
    val settings: UserSettings? = null,
    val project: Project? = null
  )

  override fun getConfig(project: Project?) = Settings(
    UITools.showDialog(
      project,
      SettingsUI::class.java,
      UserSettings::class.java,
      "Compile Documentation"
    ), project
  )

  override fun processSelection(state: SelectionState, config: Settings?): Array<File> {
    val root = state.selectedFile.toPath()
    var outputPath = root.resolve("compiled_documentation.md")
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
      val pathList = Files.walk(root)
        .filter { Files.isRegularFile(it) && !Files.isDirectory(it) }
        .toList().filterNotNull()
        .map<Path, Future<Path>> { path ->
          executorService.submit<Path?> {
            val fileContent = IOUtils.toString(FileInputStream(path.toFile()), "UTF-8") ?: return@submit null
            val transformContent = transformContent(fileContent, transformationMessage)
            markdownContent.append("# ${root.relativize(path)}\n\n")
            markdownContent.append(transformContent.replace("(?s)\n#".toRegex(), "\n##"))
            markdownContent.append("\n\n")
            path
          }
        }.toTypedArray().map { future -> future.get() ?: return@map null }.filterNotNull()
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
