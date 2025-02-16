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
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.GPT4Tokenizer
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.FileValidationUtils
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.getChildClient
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.relativeTo

class MultiDiffChatAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  override fun isEnabled(event: AnActionEvent): Boolean {
    if (FileValidationUtils.expandFileList(
        *PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)?.map { it.toFile }?.toTypedArray<File>() ?: arrayOf()
      ).isEmpty()
    ) return false
    return super.isEnabled(event)
  }

  override fun handle(event: AnActionEvent) {
    try {
      val root = getRoot(event) ?: throw RuntimeException("No file or folder selected")
      val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)
      val initialFiles = FileValidationUtils.expandFileList(*virtualFiles?.map { it.toFile }?.toTypedArray() ?: arrayOf()).map {
        it.toPath().relativeTo(root)
      }.toSet()
      val session = Session.newGlobalID()
      SessionProxyServer.metadataStorage.setSessionName(
        null,
        session,
        "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
      )
      SessionProxyServer.chats[session] = PatchApp(root.toFile(), initialFiles)
      ApplicationServer.appInfoMap[session] = AppInfoData(
        applicationName = "Code Chat",
        singleInput = true,
        stickyInput = false,
        loadImages = false,
        showMenubar = false
      )
      val server = AppServer.getServer(event.project)
      launchBrowser(server, session.toString())
    } catch (e: Exception) {
      // Comprehensive error logging
      log.error("Error in MultiDiffChatAction", e)
      UITools.showErrorDialog(e.message ?: "", "Error")
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
        browse(uri)
      } catch (e: Throwable) {
        log.warn("Error opening browser", e)
      }
    }.start()
  }

  inner class PatchApp(
    override val root: File,
    private val initialFiles: Set<Path>,
  ) : ApplicationServer(
    applicationName = "Multi-file Patch Chat",
    path = "/patchChat",
    showMenubar = false,
  ) {
    // Add proper logging
    private val log = LoggerFactory.getLogger(PatchApp::class.java)

    override val singleInput = false
    override val stickyInput = true

    // Add validation
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
          "# $path\n```$extension\n$code\n```"
        }
    }


    override fun userMessage(
      session: Session,
      user: User?,
      userMessage: String,
      ui: ApplicationInterface,
      api: API
    ) {
      try {
        fun mainActor(): SimpleActor {
          return SimpleActor(
              prompt = """
                                  You are a helpful AI that helps people with coding.
                                  
                                  You will be answering questions about the following code:
                                  
                                  """.trimIndent() + codeSummary() + patchFormatPrompt,
              model = AppSettingsState.instance.smartModel.chatModel()
          )
        }

        val settings = getSettings(session, user) ?: Settings()
        if (api is ChatClient) api.budget = settings.budget ?: 2.00

        val task = ui.newTask()
        task.add("Processing request...")

        val api = (api as ChatClient).getChildClient(task)
        val codex = GPT4Tokenizer()
        task.verbose(renderMarkdown(getCodeFiles().joinToString("\n") { path ->
          "* $path - ${codex.estimateTokenCount(root.resolve(path.toFile()).readText())} tokens"
        }))
        val toInput = { it: String -> listOf(codeSummary(), it) }
        Discussable(
          task = task,
          userMessage = { userMessage },
          heading = renderMarkdown(userMessage),
          initialResponse = { it: String -> mainActor().answer(toInput(it), api = api) },
          outputFn = { design: String ->
            """<div>${
              renderMarkdown(design) {
                AddApplyFileDiffLinks.instrumentFileDiffs(
                  ui.socketManager!!,
                  root = root.toPath(),
                  response = it,
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
          },
          ui = ui,
          reviseResponse = { userMessages: List<Pair<String, Role>> ->
            mainActor().respond(messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
              .toTypedArray<ApiModel.ChatMessage>()),
              input = toInput(userMessage),
              api = api)
          },
          atomicRef = AtomicReference(),
          semaphore = Semaphore(0),
        ).call()
      } catch (e: Exception) {
        log.error("Error processing user message", e)
        ui.newTask().error(ui, e)
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(MultiDiffChatAction::class.java)

  }
}

private fun Path.isBinary() = try {
  this.toFile().length() > 4 * 1024 * 1024 || Files.readAllBytes(this).any { it == 0.toByte() }
} catch (e: Exception) {
  false
}