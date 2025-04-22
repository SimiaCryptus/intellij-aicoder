package aicoder.actions.chat

import aicoder.actions.BaseAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

class GenericChatAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private val systemPrompt = ""
  private val userInterfacePrompt = ""
  
  override fun handle(e: AnActionEvent) {
    val project = e.project ?: return

    try {
      UITools.runAsync(project, "Initializing Chat", true) { progress ->
        progress.isIndeterminate = true
        progress.text = "Setting up chat session..."

        val session = Session.newGlobalID()
        aicoder.actions.SessionProxyServer.metadataStorage.setSessionName(
          null,
          session,
          "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
        )
        aicoder.actions.SessionProxyServer.agents[session] = ChatSocketManager(
          session = session,
          model = AppSettingsState.instance.smartModel.chatModel(),
          parsingModel = AppSettingsState.instance.fastModel.chatModel(),
          initialAssistantPrompt = "",
          systemPrompt = systemPrompt,
          api = api,
          applicationClass = ApplicationServer::class.java,
          storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome),
        )
        ApplicationServer.appInfoMap[session] = AppInfoData(
          applicationName = "Code Chat",
          singleInput = false,
          stickyInput = true,
          loadImages = false,
          showMenubar = false
        )
        val server = AppServer.getServer(project)

        val uri = server.server.uri.resolve("/#$session")
        ApplicationManager.getApplication().executeOnPooledThread {
          try {
            BaseAction.log.info("Opening browser to $uri")
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

  override fun isEnabled(event: AnActionEvent) = true

  companion object {
    private val log = LoggerFactory.getLogger(GenericChatAction::class.java)
  }
}