package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.parsers.DefaultParsingModel
import com.simiacryptus.skyenet.apps.parsers.DocumentParserApp
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import org.slf4j.LoggerFactory
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import java.io.File

class DocumentDataExtractorAction : BaseAction() {
    val path = "/pdfExtractor"
    private var settings = DocumentParserApp.Settings()

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent): Boolean {
        if(!super.isEnabled(event)) return false
        val selectedFile = UITools.getSelectedFile(event)
        return selectedFile != null && (
            selectedFile.name.endsWith(".pdf", ignoreCase = true) ||
            selectedFile.name.endsWith(".txt", ignoreCase = true) ||
            selectedFile.name.endsWith(".md", ignoreCase = true) ||
            selectedFile.name.endsWith(".html", ignoreCase = true)
        )
    }

    override fun handle(e: AnActionEvent) {
        val selectedFile = UITools.getSelectedFile(e)
        if (selectedFile == null || (!selectedFile.name.endsWith(
                ".pdf",
                ignoreCase = true
            ) && !selectedFile.name.endsWith(".txt", ignoreCase = true))
        ) {
            UITools.showErrorDialog(e.project, "Please select a PDF or text file.", "Invalid Selection")
            return
        }
        val configDialog = DocumentDataExtractorConfigDialog(e.project, settings)
        if (!configDialog.showAndGet()) return
        settings = configDialog.settings

        val session = StorageInterface.newGlobalID()
        val pdfFile = selectedFile.toFile
        DataStorage.sessionPaths[session] = pdfFile.parentFile

        val documentParserApp = object : DocumentParserApp(
            applicationName = "Document Extractor",
            path = path,
            api = api,
            fileInput = pdfFile.toPath(),
            parsingModel = DefaultParsingModel(AppSettingsState.instance.smartModel.chatModel(), 0.1),
        ) {
            override fun <T : Any> initSettings(session: Session): T = settings as T
            override val root: File get() = selectedFile.parent.toFile
        }

        SessionProxyServer.chats[session] = documentParserApp
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Code Chat",
            singleInput = true,
            stickyInput = false,
            loadImages = false,
            showMenubar = false
        )
        val server = AppServer.getServer(e.project)
        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                BaseAction.log.warn("Error opening browser", e)
            }
        }.start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(DocumentDataExtractorAction::class.java)
    }
}