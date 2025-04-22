package aicoder.actions.chat

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.MultiStepPatchAction.AutoDevApp.Settings
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.AddApplyFileDiffLinks
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.GPT4Tokenizer
import com.simiacryptus.skyenet.apps.general.renderMarkdown
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.util.FileSelectionUtils
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import kotlin.io.path.relativeTo

open class MultiDiffChatAction(
  protected val showLineNumbers: Boolean = false
) : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  override fun isEnabled(event: AnActionEvent): Boolean {
    if (FileSelectionUtils.expandFileList(
        *PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)?.map { it.toFile }?.toTypedArray<File>() ?: arrayOf()
      ).isEmpty()
    ) return false
    return super.isEnabled(event)
  }
  
  override fun handle(e: AnActionEvent) {
    try {
      val root = getRoot(e) ?: throw RuntimeException("No file or folder selected")
      val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)
      val initialFiles = FileSelectionUtils.expandFileList(*virtualFiles?.map { it.toFile }?.toTypedArray() ?: arrayOf()).map {
        it.toPath().relativeTo(root)
      }.toSet()
      val session = Session.newGlobalID()
      SessionProxyServer.metadataStorage.setSessionName(
        null,
        session,
        "${getActionName()} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
      )
      val model = AppSettingsState.instance.smartModel.chatModel()
      val parsingModel = AppSettingsState.instance.fastModel.chatModel()
      SessionProxyServer.agents[session] = PatchChatManager(
        session = session,
        model = model,
        parsingModel = parsingModel,
        root = root.toFile(),
        initialFiles = initialFiles,
        showLineNumbers = showLineNumbers
      )
      ApplicationServer.appInfoMap[session] = AppInfoData(
        applicationName = "Code Chat",
        singleInput = false,
        stickyInput = true,
        loadImages = false,
        showMenubar = false
      )
      val server = AppServer.getServer(e.project)
      launchBrowser(server, session.toString())
    } catch (e: Exception) {
      // Comprehensive error logging
      log.error("Error in MultiDiffChatAction", e)
      UITools.showErrorDialog(e.message ?: "", "Error")
    }
  }
  protected open fun getActionName(): String = if (showLineNumbers) "MultiDiffChatWithLineNumbers" else "MultiDiffChat"


  private fun getRoot(event: AnActionEvent): Path? {
    val folder = UITools.getSelectedFolder(event)
    return if (null != folder) {
      folder.toFile.toPath()
    } else {
      getModuleRootForFile(UITools.getSelectedFile(event)?.parent?.toFile ?: return null).toPath()
    }
  }

  private fun launchBrowser(server: AppServer, session: String) {
    Thread {
      Thread.sleep(500)
      try {
        val uri = server.server.uri.resolve("/#$session")
        BaseAction.log.info("Opening browser to $uri")
        browse(uri)
      } catch (e: Throwable) {
        log.warn("Error opening browser", e)
      }
    }.start()
  }
  
  inner class PatchChatManager(
    session: Session,
    model: ChatModel,
    parsingModel: ChatModel,
    val root: File,
    private val initialFiles: Set<Path>,
    private val showLineNumbers: Boolean = false
  ) : ChatSocketManager(
    session = session,
    model = model,
    parsingModel = parsingModel,
    systemPrompt = "",
    api = api,
    applicationClass = ApplicationServer::class.java,
    storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome),
  ) {
    override val systemPrompt: String
      get() = """
        You are a helpful AI that helps people with coding.
        You will be answering questions about the following code:
        ${codeSummary()}
        ${if (showLineNumbers) "\nNote: Line numbers are shown at the beginning of each line in the format 'NUMBER | CODE'. These are for reference only and should not be included in any patches or code modifications.\n" else ""}
        ${patchFormatPrompt}
      """.trimIndent()

    private fun getCodeFiles(): Set<Path> {
      if (!root.exists()) {
        log.warn("Root directory does not exist: $root")
        return emptySet()
      }
      return initialFiles.filter { path ->
        val file = root.toPath().resolve(path).toFile()
        val exists = file.exists()
        if (!exists) log.warn("File does not exist: $file")
        exists
      }.toSet()
    }

    private fun codeSummary(): String {
      return getCodeFiles().associateWith { root.toPath().resolve(it).toFile().readText(Charsets.UTF_8) }
        .entries.joinToString("\n\n") { (path, code) ->
          val extension =
            path.toString().split('.').lastOrNull()?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }
          if (showLineNumbers) {
            val lines = code.lines()
            val lineNumberWidth = lines.size.toString().length
            val numberedLines = lines.mapIndexed { index, line ->
              String.format("%${lineNumberWidth}d | %s", index + 1, line)
            }.joinToString("\n")
            "# $path\n```$extension\n$numberedLines\n```"
          } else {
            "# $path\n```$extension\n$code\n```"
          }
        }
    }
    
    override fun renderResponse(response: String, task: SessionTask) = renderMarkdown(response) { html ->
      AddApplyFileDiffLinks.instrumentFileDiffs(
        this,
        root = root.toPath(),
        response = html,
        handle = { newCodeMap ->
          newCodeMap.forEach { (path, newCode) ->
            task.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
          }
        },
        ui = ui,
        api = api,
      )
    }
    
    override fun respond(api: ChatClient, task: SessionTask, userMessage: String): String {
      // Display token count information
      val codex = GPT4Tokenizer()
      task.verbose((getCodeFiles().joinToString("\n") { path ->
        "* $path - ${codex.estimateTokenCount(root.resolve(path.toFile()).readText())} tokens"
      }).renderMarkdown())
      
      // Set budget if needed
      val settings = Settings()
      api.budget = settings.budget ?: 2.00
      
      // Use the parent implementation
      return super.respond(api, task, userMessage)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(MultiDiffChatAction::class.java)

  }
}
class MultiDiffChatWithLineNumbersAction : MultiDiffChatAction(showLineNumbers = true) {
  override fun getActionName(): String = "MultiDiffChatWithLineNumbers"
}