package aicoder.actions.knowledge

import aicoder.actions.BaseAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.aicoder.util.findRecursively
import com.simiacryptus.skyenet.apps.parse.DocumentRecord.Companion.saveAsBinary
import com.simiacryptus.skyenet.apps.parse.ProgressState
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

class SaveAsQueryIndexAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  data class IndexConfig(
    val threadCount: Int = 8,
    val batchSize: Int = 100
  )

  private fun getConfig(project: com.intellij.openapi.project.Project?): IndexConfig {
    // Could be enhanced to show a dialog for configuration
    return IndexConfig()
  }


  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    val selectedFiles = UITools.getSelectedFiles(event)
    return selectedFiles.isNotEmpty() && selectedFiles.any { file ->
      file.isDirectory || file.name.endsWith(".parsed.json")
    }
  }


  override fun handle(e: AnActionEvent) {
    val selectedFiles = UITools.getSelectedFiles(e)
    if (selectedFiles.isEmpty()) {
      UITools.showErrorDialog(
        "Please select JSON files to convert.",
        "No Files Selected"
      )
      return
    }
    val jsonFiles = selectedFiles.flatMap { file ->
      when {
        file.isDirectory -> file.findRecursively { it.name.endsWith(".parsed.json") }
        file.name.endsWith(".parsed.json") -> listOf(file)
        else -> emptyList()
      }
    }
    if (jsonFiles.isEmpty()) {
      UITools.showErrorDialog("No .parsed.json files found in selection.", "No Valid Files")
      return
    }
    val config = getConfig(e.project)
    ProgressManager.getInstance().run(object : Task.Backgroundable(e.project, "Indexing Vectors", true) {
      override fun run(indicator: ProgressIndicator) {
        val threadPool = Executors.newFixedThreadPool(config.threadCount)
        try {
          indicator.isIndeterminate = false
          indicator.fraction = 0.0
          indicator.text = "Initializing vector indexing..."

          saveAsBinary(
            openAIClient = IdeaOpenAIClient.instance,
            pool = threadPool,
            progressState = ProgressState().apply {
              onUpdate += {
                indicator.fraction = it.progress / it.max
                indicator.text = "Processing files (${it.progress}/${it.max})"
                if (indicator.isCanceled) {
                  throw InterruptedException("Operation cancelled by user")
                }
              }
            },
            inputPaths = jsonFiles.map { it.path }.toTypedArray()
          )

          indicator.fraction = 1.0
          indicator.text = "Vector indexing complete"
          log.info("Conversion to Data complete")
          UITools.showInfoMessage("Vector indexing completed successfully", "Success")
        } catch (ex: InterruptedException) {
          log.info("Vector indexing cancelled by user")
          UITools.showInfoMessage("Vector indexing cancelled", "Cancelled")
        } catch (ex: Exception) {
          log.error("Error during binary conversion", ex)
          UITools.showErrorDialog("Error during conversion: ${ex.message}", "Conversion Failed")
        } finally {
          threadPool.shutdownNow()
        }
      }
    })
  }


  companion object {
    private val log = LoggerFactory.getLogger(SaveAsQueryIndexAction::class.java)
  }
}