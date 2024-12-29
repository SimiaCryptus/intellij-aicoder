package aicoder.actions.knowledge

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.aicoder.util.findRecursively
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.parse.DocumentParserApp
import com.simiacryptus.skyenet.apps.parse.DocumentParsingModel
import com.simiacryptus.skyenet.apps.parse.ParsingModel
import com.simiacryptus.skyenet.apps.parse.ParsingModel.DocumentData
import com.simiacryptus.skyenet.apps.parse.ParsingModelType
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat

class DocumentDataExtractorAction : BaseAction(
  name = "Extract Document Data", description = "Extracts structured data from documents using AI"
) {
  val path = "/pdfExtractor"
  private var settings = DocumentParserApp.Settings()
  private var modelType = ParsingModelType.Document

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
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
    filename.split('.').lastOrNull()?.let {
      setOf(
        /* Images */
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "svg",
        /* Videos */
        "mp4", "webm", "ogg", "avi", "mov", "flv", "mkv", "wmv", "mpg", "mpeg", "3gp", "3g2",
        /* Audio */
        "mp3", "wav", "ogg", "flac", "aac", "wma", "m4a", "aiff", "alac",
        /* Archives */
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz",
        /* Executables */
        "exe", "dmg", "apk", "msi", "deb",
      ).contains(it)
    } == true -> false

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
      UITools.showErrorDialog("No valid files found in selection.", "No Valid Files")
      return
    }
    val selectedFile = processableFiles.first()
    val configDialog = DocumentDataExtractorConfigDialog(e.project, settings, modelType)
    if (!configDialog.showAndGet()) return
    settings = configDialog.settings
    modelType = configDialog.modelType as ParsingModelType<DocumentParsingModel>

    UITools.runAsync(e.project, "Initializing Document Extractor", true) { progress ->
      try {
        progress.text = "Setting up session..."
        val session = Session.newGlobalID()
        DataStorage.sessionPaths[session] = selectedFile.toFile.parentFile
        progress.text = "Configuring AI model..."
        val smartModel = AppSettingsState.instance.smartModel.chatModel()
        val parsingModel = ParsingModelType.getImpl(smartModel, 0.1, modelType)
        progress.text = "Initializing document parser..."
        SessionProxyServer.metadataStorage.setSessionName(
          null, session,
          "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
        )
        val app = object : DocumentParserApp(
          applicationName = "Document Extractor",
          path = this@DocumentDataExtractorAction.path,
          api = this@DocumentDataExtractorAction.api,
          fileInputs = processableFiles.map { it.toNioPath() },
          parsingModel = parsingModel as ParsingModel<DocumentData>,
          fastMode = settings.fastMode,
        ) {
          override fun <T : Any> initSettings(session: Session): T = this@DocumentDataExtractorAction.settings as T
          override val root: File get() = selectedFile.parent.toFile
        }
        SessionProxyServer.chats[session] = app
        ApplicationServer.appInfoMap[session] = AppInfoData(
          applicationName = "Document Data Extractor", singleInput = false, stickyInput = true, loadImages = false, showMenubar = false
        )
        progress.text = "Starting server..."
        val server = AppServer.getServer(e.project)
        launchBrowser(server, session.toString())
      } catch (ex: Throwable) {
        log.error("Failed to initialize document extractor", ex)
        UITools.showErrorDialog(
          "Failed to initialize document extractor: ${ex.message}", "Initialization Error"
        )
      }
    }
  }

  private fun launchBrowser(server: AppServer, session: String) {
    Thread {
      Thread.sleep(500)
      try {
        val uri = server.server.uri.resolve("/#$session")
        log.info("Opening browser to $uri")
        browse(uri)
      } catch (e: Throwable) {
        log.warn("Error opening browser", e)
      }
    }.start()
  }

  companion object {
    private val log = LoggerFactory.getLogger(DocumentDataExtractorAction::class.java)
  }
}