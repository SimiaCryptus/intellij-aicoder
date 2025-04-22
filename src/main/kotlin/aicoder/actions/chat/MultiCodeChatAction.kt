package aicoder.actions.chat

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.MultiStepPatchAction
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.AddApplyFileDiffLinks
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.GPT4Tokenizer
import com.simiacryptus.skyenet.apps.general.renderMarkdown
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
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

/**
 * Action that enables multi-file code chat functionality.
 * Allows users to select multiple files and discuss them with an AI assistant.
 * Supports code modifications through patch application.
 *
 * @see BaseAction
 */

class MultiCodeChatAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun handle(event: AnActionEvent) {
    val root = getRoot(event) ?: return
    val codeFiles = getFiles(PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext) ?: arrayOf(), root).toMutableSet()
    fun codeSummary() = codeFiles.filter {
      root.resolve(it).toFile().exists()
    }.associateWith { root.resolve(it).toFile().readText(Charsets.UTF_8) }
      .entries.joinToString("\n\n") { (path, code) ->
        val extension = path.toString().split('.').lastOrNull()?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }
        "# $path\n```$extension\n$code\n```"
      }
    try {
      UITools.runAsync(event.project, "Initializing Chat", true) { progress ->
        progress.isIndeterminate = true
        progress.text = "Setting up chat session..."
        val session = Session.newGlobalID()
        SessionProxyServer.metadataStorage.setSessionName(
          null,
          session,
          "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
        )
        val model = AppSettingsState.instance.smartModel.chatModel()
        val parsingModel = AppSettingsState.instance.fastModel.chatModel()
        SessionProxyServer.agents[session] = CodeChatManager(
          session = session,
          model = model,
          parsingModel = parsingModel,
          root = root.toFile(),
          codeFiles = codeFiles
        )
        ApplicationServer.appInfoMap[session] = AppInfoData(
          applicationName = "Code Chat",
          singleInput = false,
          stickyInput = true,
          loadImages = false,
          showMenubar = false
        )
        val server = AppServer.getServer(event.project)
        launchBrowser(server, session.toString())
      }
    } catch (e: Throwable) {
      UITools.error(log, "Failed to initialize chat session", e)
    }
  }

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
        BrowseUtil.browse(uri)
      } catch (e: Throwable) {
        log.warn("Error opening browser", e)
      }
    }.start()
  }

  override fun isEnabled(event: AnActionEvent): Boolean {
    val root = getRoot(event) ?: return false
    val files = getFiles(PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext) ?: arrayOf(), root)
    if (files.isEmpty()) return false
    return super.isEnabled(event)
  }
  
  /** Chat manager that handles the chat interface and code modifications */
  inner class CodeChatManager(
    session: Session,
    model: ChatModel,
    parsingModel: ChatModel,
    val root: File,
    private val codeFiles: Set<Path>
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
      """.trimIndent()
    
    private fun codeSummary(): String {
      return codeFiles.filter { path ->
        val file = root.toPath().resolve(path).toFile()
        val exists = file.exists()
        if (!exists) log.warn("File does not exist: $file")
        exists
      }.associateWith { root.toPath().resolve(it).toFile().readText(Charsets.UTF_8) }
        .entries.joinToString("\n\n") { (path, code) ->
          val extension = path.toString().split('.').lastOrNull()?.let { it }
          "# $path\n```$extension\n$code\n```"
        }
    }
    
    override fun renderResponse(response: String, task: SessionTask) = """<div>${
      renderMarkdown(response) { html ->
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
    }</div>"""
    
    override fun respond(api: ChatClient, task: SessionTask, userMessage: String): String {
      // Display token count information
      val codex = GPT4Tokenizer()
      task.verbose((codeFiles.joinToString("\n") { path ->
        "* $path - ${codex.estimateTokenCount(root.resolve(path.toFile()).readText())} tokens"
      }).renderMarkdown())
      
      // Set budget if needed
      val settings = MultiStepPatchAction.AutoDevApp.Settings()
      api.budget = settings.budget ?: 2.00
      
      // Use the parent implementation
      return super.respond(api, task, userMessage)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(MultiCodeChatAction::class.java)

    fun getFiles(
      virtualFiles: Array<out VirtualFile>?,
      root: Path
    ): Set<Path> = virtualFiles?.flatMap { file ->
      if (file.isDirectory && !file.name.startsWith(".")) {
        getFiles(file.children, root)
      } else {
        setOf(root.relativize(file.toNioPath()))
      }
    }?.toSet() ?: emptySet()
  }
}