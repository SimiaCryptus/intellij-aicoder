package aicoder.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.UITools
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

abstract class FileContextAction<T : Any>(
  private val supportsFiles: Boolean = true,
  private val supportsFolders: Boolean = true,
) : BaseAction() {

  data class SelectionState(
    val selectedFile: File,
    val projectRoot: File,
  )

  abstract fun processSelection(state: SelectionState, config: T?, progress: ProgressIndicator): Array<File>

  final override fun handle(e: AnActionEvent) {
    val config = getConfig(e.project, e)
    if (config == null) {
      log.warn("No configuration found for ${javaClass.simpleName}")
      return
    }
    val virtualFile = UITools.getSelectedFile(e) ?: UITools.getSelectedFolder(e) ?: run {
      log.warn("No file or folder selected")
      return
    }
    val project = e.project ?: return
    val projectBasePath = project.basePath ?: run {
      log.error("Project base path is null")
      return
    }
    val projectRoot = File(projectBasePath).toPath()
    Thread {
      try {
        UITools.redoableTask(e) {
          UITools.run(e.project, templateText!!, true) { progress ->
            val newFiles = try {
              processSelection(
                SelectionState(
                  selectedFile = virtualFile.toNioPath().toFile(),
                  projectRoot = projectRoot.toFile(),
                ), config, progress
              )
            } catch (ex: Exception) {
              log.error("Error processing selection", ex)
              throw ex
            } finally {
              if (progress.isCanceled) throw InterruptedException()
            }
            val start = System.currentTimeMillis()
            val fileSystem = LocalFileSystem.getInstance()
            val firstFile = newFiles.firstOrNull() ?: throw IllegalStateException("No files were generated")
            var refreshedFile: VirtualFile? = null
            while (refreshedFile == null) {
              if (System.currentTimeMillis() - start > 10000) {
                throw IllegalStateException("Timeout waiting for file to appear: ${firstFile.absolutePath}")
              }
              refreshedFile = fileSystem.refreshAndFindFileByIoFile(firstFile)
              Thread.sleep(500)
            }
            UITools.writeableFn(e) {
              val files = newFiles.mapNotNull { file ->
                val generatedFile = fileSystem.refreshAndFindFileByIoFile(file)
                if (generatedFile == null) {
                  log.warn("Generated file not found: ${file.path}")
                } else {
                  open(project, file.toPath())
                }
                generatedFile
              }.toTypedArray<VirtualFile?>()
              Runnable {
                files.forEach { it?.delete(this@FileContextAction) }
              }
            }
          }
        }
      } catch (e: Throwable) {
        UITools.error(log, "Error in ${javaClass.simpleName}", e)
      }
    }.start()
  }

  open fun getConfig(project: Project?, e: AnActionEvent): T? = null

  var isDevAction = false
  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    if (isDevAction && !AppSettingsState.instance.devActions) return false
    val virtualFile = UITools.getSelectedFile(event) ?: UITools.getSelectedFolder(event) ?: return false
    return if (virtualFile.isDirectory) supportsFolders else supportsFiles
  }

  companion object {
    private val log = LoggerFactory.getLogger(FileContextAction::class.java)

    fun open(project: Project, outputPath: Path) {
      log.info("Opening file: $outputPath")
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