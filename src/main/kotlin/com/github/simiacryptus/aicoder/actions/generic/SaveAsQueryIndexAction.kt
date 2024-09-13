package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.skyenet.apps.general.parsers.DefaultParsingModel
import com.simiacryptus.skyenet.apps.general.parsers.DocumentRecord.Companion.saveAsBinary
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors

class SaveAsQueryIndexAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!super.isEnabled(event)) return false
        val selectedFiles = UITools.getSelectedFiles(event)
        return selectedFiles.isNotEmpty() && selectedFiles.all { it.name.endsWith(".parsed.json") }
    }

    override fun handle(e: AnActionEvent) {
        val selectedFiles = UITools.getSelectedFiles(e)
        if (selectedFiles.isEmpty()) {
            UITools.showErrorDialog(e.project, "Please select JSON files to convert.", "No Files Selected")
            return
        }

        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            .withTitle("Select Output Directory")
            .withDescription("Choose where to save the Parquet file")

        FileChooser.chooseFile(descriptor, e.project, null) { outputDir: VirtualFile? ->
            if (outputDir != null) {
                val outputPath = File(outputDir.path, "document.index.data").absolutePath
                ProgressManager.getInstance().run(object : Task.Backgroundable(e.project, "Converting to Parquet") {
                    override fun run(indicator: ProgressIndicator) {
                        try {
                            indicator.isIndeterminate = false
                            indicator.fraction = 0.0
                            saveAsBinary(
                                IdeaOpenAIClient.instance,
                                outputPath,
                                Executors.newFixedThreadPool(8),
                                *selectedFiles.map { it.path }.toTypedArray()
                            )
                            indicator.fraction = 1.0
                            log.info("Conversion to Data complete")
                        } catch (ex: Exception) {
                            log.error("Error during Parquet conversion", ex)
                            UITools.showErrorDialog(e.project, "Error during conversion: ${ex.message}", "Conversion Failed")
                        }
                    }
                })
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SaveAsQueryIndexAction::class.java)
    }
}