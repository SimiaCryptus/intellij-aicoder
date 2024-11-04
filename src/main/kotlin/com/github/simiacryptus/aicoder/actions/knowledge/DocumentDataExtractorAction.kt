package com.github.simiacryptus.aicoder.actions.knowledge

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.actions.generic.toFile
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.findRecursively
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.parse.CodeParsingModel
import com.simiacryptus.skyenet.apps.parse.DocumentParserApp
import com.simiacryptus.skyenet.apps.parse.DocumentParsingModel
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

class DocumentDataExtractorAction : BaseAction() {
  val path = "/pdfExtractor"
  private var settings = DocumentParserApp.Settings()

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    if (!AppSettingsState.instance.devActions) return false
    val selectedFiles = UITools.getSelectedFiles(event)
    val processableFiles = selectedFiles.flatMap { file ->
      when {
        file.isDirectory -> file.findRecursively { isValidFileType(it.name) }
        isValidFileType(file.name) -> listOf(file)
        else -> emptyList()
      }
    }
    return processableFiles.isNotEmpty()
  }

  fun isValidFileType(filename: String): Boolean = when {
    filename.endsWith(".parsed.json", ignoreCase = true) -> false
    filename.endsWith(".data", ignoreCase = true) -> false
    filename.endsWith(".pdf", ignoreCase = true) -> true
    filename.endsWith(".txt", ignoreCase = true) -> true
    filename.endsWith(".html", ignoreCase = true) -> true
    filename.endsWith(".htm", ignoreCase = true) -> true
    filename.endsWith(".md", ignoreCase = true) -> true
    else -> true // Allow other files for code parsing
  }

  override fun handle(e: AnActionEvent) {
    val selectedFiles = UITools.getSelectedFiles(e)
    val processableFiles = selectedFiles.flatMap { file ->
      when {
        file.isDirectory -> file.findRecursively { isValidFileType(it.name) }
        isValidFileType(file.name) -> listOf(file)
        else -> emptyList()
      }
    }
    if (processableFiles.isEmpty()) {
      UITools.showErrorDialog(e.project, "No valid files found in selection.", "No Valid Files")
      return
    }
    val selectedFile = processableFiles.first()
    val configDialog = DocumentDataExtractorConfigDialog(e.project, settings)
    if (!configDialog.showAndGet()) return
    settings = configDialog.settings

    val session = Session.newGlobalID()
    DataStorage.sessionPaths[session] = selectedFile.toFile.parentFile

    val smartModel = AppSettingsState.instance.smartModel.chatModel()
    val parsingModel = when {
      selectedFile.name.endsWith(".pdf", ignoreCase = true) -> DocumentParsingModel(smartModel, 0.1)
      selectedFile.name.endsWith(".txt", ignoreCase = true) -> DocumentParsingModel(smartModel, 0.1)
      selectedFile.name.endsWith(".html", ignoreCase = true) -> DocumentParsingModel(smartModel, 0.1)
      selectedFile.name.endsWith(".htm", ignoreCase = true) -> DocumentParsingModel(smartModel, 0.1)
      selectedFile.name.endsWith(".md", ignoreCase = true) -> DocumentParsingModel(smartModel, 0.1)
      else -> CodeParsingModel(smartModel, 0.1)
    }

    SessionProxyServer.Companion.chats[session] = object : DocumentParserApp(
      applicationName = "Document Extractor",
      path = this@DocumentDataExtractorAction.path,
      api = this@DocumentDataExtractorAction.api,
      fileInputs = processableFiles.map<VirtualFile, Path> { it.toNioPath() },
      parsingModel = parsingModel,
    ) {
      override fun <T : Any> initSettings(session: Session): T = this@DocumentDataExtractorAction.settings as T
      override val root: File get() = selectedFile.parent.toFile
    }
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