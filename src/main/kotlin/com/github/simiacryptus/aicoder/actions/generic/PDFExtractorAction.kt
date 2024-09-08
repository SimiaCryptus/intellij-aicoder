package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.skyenet.apps.general.PDFExtractorApp
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import org.slf4j.LoggerFactory
import java.awt.Desktop

class PDFExtractorAction : BaseAction() {
    val path = "/pdfExtractor"

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent): Boolean {
        if(!super.isEnabled(event)) return false
        val selectedFile = UITools.getSelectedFile(event)
        return selectedFile != null && selectedFile.name.endsWith(".pdf", ignoreCase = true)
    }

    override fun handle(e: AnActionEvent) {
        val selectedFile = UITools.getSelectedFile(e)
        if (selectedFile == null || !selectedFile.name.endsWith(".pdf", ignoreCase = true)) {
            UITools.showErrorDialog(e.project, "Please select a PDF file.", "Invalid Selection")
            return
        }

        val session = StorageInterface.newGlobalID()
        val pdfFile = selectedFile.toFile
        DataStorage.sessionPaths[session] = pdfFile.parentFile

        val pdfExtractorApp = PDFExtractorApp(
            applicationName = "PDF Extractor",
            path = path,
            fileInput = pdfFile.toPath(),
            api = api
        )

        SessionProxyServer.chats[session] = pdfExtractorApp
        val server = AppServer.getServer(e.project)
        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                Desktop.getDesktop().browse(uri)
            } catch (e: Throwable) {
                BaseAction.log.warn("Error opening browser", e)
            }
        }.start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(PDFExtractorAction::class.java)
    }
}