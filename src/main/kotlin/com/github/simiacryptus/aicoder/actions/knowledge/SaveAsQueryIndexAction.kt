package com.github.simiacryptus.aicoder.actions.knowledge

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.findRecursively
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.simiacryptus.skyenet.apps.parse.DocumentRecord.Companion.saveAsBinary
import com.simiacryptus.skyenet.apps.parse.ProgressState
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

class SaveAsQueryIndexAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!super.isEnabled(event)) return false
        if (!AppSettingsState.instance.devActions) return false
        val selectedFiles = UITools.getSelectedFiles(event)
        return selectedFiles.isNotEmpty() && selectedFiles.any { file ->
            file.isDirectory || file.name.endsWith(".parsed.json")
        }
    }

    override fun handle(e: AnActionEvent) {
        val selectedFiles = UITools.getSelectedFiles(e)
        if (selectedFiles.isEmpty()) {
            UITools.showErrorDialog(e.project, "Please select JSON files to convert.", "No Files Selected")
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
            UITools.showErrorDialog(e.project, "No .parsed.json files found in selection.", "No Valid Files")
            return
        }
        ProgressManager.getInstance().run(object : Task.Backgroundable(e.project, "Indexing Vectors", false) {
            override fun run(indicator: ProgressIndicator) {
                val threadPool = Executors.newFixedThreadPool(8)
                try {
                    indicator.isIndeterminate = false
                    indicator.fraction = 0.0
                    saveAsBinary(
                      openAIClient = IdeaOpenAIClient.instance,
                      pool = threadPool,
                      progressState = ProgressState().apply {
                        onUpdate += {
                          indicator.fraction = it.progress / it.max
                        }
                      },
                      inputPaths = jsonFiles.map { it.path }.toTypedArray()
                    )
                    indicator.fraction = 1.0
                    log.info("Conversion to Data complete")
                } catch (ex: Exception) {
                    log.error("Error during binary conversion", ex)
                    UITools.showErrorDialog(e.project, "Error during conversion: ${ex.message}", "Conversion Failed")
                } finally {
                    threadPool.shutdown()
                }
            }
        })
    }


    companion object {
        private val log = LoggerFactory.getLogger(SaveAsQueryIndexAction::class.java)
    }
}