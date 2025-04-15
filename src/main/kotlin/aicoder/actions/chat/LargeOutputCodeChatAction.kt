package aicoder.actions.chat

import aicoder.actions.BaseAction
import aicoder.actions.LargeOutputChatSocketManager
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.core.actors.LargeOutputActor
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.text.SimpleDateFormat

class LargeOutputCodeChatAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private val model by lazy { AppSettingsState.instance.smartModel.chatModel() }
  private val parsingModel by lazy { AppSettingsState.instance.fastModel.chatModel() }

  override fun handle(e: AnActionEvent) {
    val project = e.project ?: return
    val root = getRoot(e) ?: return
    val codeFiles = MultiCodeChatAction.getFiles(
      PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext) ?: arrayOf(),
      root
    )

    if (codeFiles.isEmpty()) {
      log.warn("No code files selected")
      return
    }

    val codeSummary = generateCodeSummary(root, codeFiles)

    try {
      UITools.runAsync(project, "Initializing Enhanced Code Chat", true) { progress ->
        progress.isIndeterminate = true
        progress.text = "Setting up enhanced code chat session..."

        val session = Session.newGlobalID()

        SessionProxyServer.metadataStorage.setSessionName(
          null,
          session,
          "Code Analysis Chat @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
        )

        SessionProxyServer.agents[session] = LargeOutputChatSocketManager(
          session = session,
          model = model,
          parsingModel = parsingModel,
          userInterfacePrompt = """
                        # Enhanced Code Analysis Chat
                        Analyzing the following files:
                        ${codeFiles.joinToString("\n") { "* $it" }}
                        
                        This chat interface provides structured responses to help analyze and modify code.
                        Feel free to ask questions about the code - responses will be organized into clear sections.
                    """.trimIndent(),
          systemPrompt = """
                        You are a helpful AI coding assistant analyzing multiple code files. 
                        Please provide detailed, well-structured responses about the code.
                        Break down complex explanations into clear sections using markdown headers.
                        When suggesting code changes, use proper diff format with + and - prefixes.
                    """.trimIndent() + "\n\nCode being analyzed:\n\n$codeSummary",
          api = api,
          storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome),
          applicationClass = ApplicationServer::class.java,
          largeOutputActor = LargeOutputActor(
            model = model,
            temperature = 0.3,
            maxIterations = 3
          )
        )

        ApplicationServer.appInfoMap[session] = AppInfoData(
          applicationName = "Enhanced Code Analysis Chat",
          singleInput = false,
          stickyInput = true,
          loadImages = false,
          showMenubar = false
        )

        val uri = AppServer.getServer(project).server.uri.resolve("/#$session")
        ApplicationManager.getApplication().executeOnPooledThread {
          try {
            BaseAction.log.info("Opening enhanced code chat browser to $uri")
            browse(uri)
          } catch (e: Throwable) {
            UITools.error(log, "Failed to open browser", e)
          }
        }
      }
    } catch (e: Throwable) {
      log.warn("Error opening browser", e)
    }
  }

  private fun generateCodeSummary(root: Path, codeFiles: Set<Path>): String {
    return codeFiles.filter {
      root.resolve(it).toFile().exists()
    }.joinToString("\n\n") { path ->
      val code = root.resolve(path).toFile().readText(Charsets.UTF_8)
      val extension = path.toString().split('.').lastOrNull() ?: ""
      "# $path\n```$extension\n$code\n```"
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

  override fun isEnabled(event: AnActionEvent): Boolean {
    val root = getRoot(event) ?: return false
    val files = MultiCodeChatAction.getFiles(
      PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext) ?: arrayOf(),
      root
    )
    return files.isNotEmpty() && super.isEnabled(event)
  }

  companion object {
    private val log = LoggerFactory.getLogger(LargeOutputCodeChatAction::class.java)
  }
}