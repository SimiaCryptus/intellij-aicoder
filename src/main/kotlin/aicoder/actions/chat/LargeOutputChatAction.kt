package aicoder.actions.chat

import aicoder.actions.BaseAction
import aicoder.actions.EnhancedChatSocketManager
import aicoder.actions.SessionProxyServer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.core.actors.LargeOutputActor
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

class LargeOutputChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    private val systemPrompt = """
        You are a helpful AI coding assistant. Please provide detailed, well-structured responses.
        Break down complex explanations into clear sections using the ellipsis notation.
    """.trimIndent()
    
    private val userInterfacePrompt = """
        # Enhanced Code Chat
        This chat interface uses structured responses to better organize complex information.
        Feel free to ask coding questions - responses will be broken down into clear sections.
    """.trimIndent()
    
    private val model by lazy { AppSettingsState.instance.smartModel.chatModel() }

    override fun handle(e: AnActionEvent) {
        val project = e.project ?: return

        try {
            UITools.runAsync(project, "Initializing Enhanced Chat", true) { progress ->
                progress.isIndeterminate = true
                progress.text = "Setting up enhanced chat session..."

                val session = Session.newGlobalID()
                val largeOutputActor = LargeOutputActor(
                    model = model,
                    temperature = 0.3,
                    maxIterations = 3
                )

                SessionProxyServer.metadataStorage.setSessionName(
                    null, 
                    session, 
                    "Enhanced Chat @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
                )
                
                SessionProxyServer.agents[session] = EnhancedChatSocketManager(
                    session = session,
                    model = model,
                    userInterfacePrompt = userInterfacePrompt,
                    systemPrompt = systemPrompt,
                    api = api,
                    storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome),
                    applicationClass = ApplicationServer::class.java,
                    largeOutputActor = largeOutputActor
                )

                ApplicationServer.appInfoMap[session] = AppInfoData(
                    applicationName = "Enhanced Code Chat",
                    singleInput = false,
                    stickyInput = true,
                    loadImages = false,
                    showMenubar = false
                )

                val server = AppServer.getServer(project)
                val uri = server.server.uri.resolve("/#$session")
                
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        BaseAction.log.info("Opening enhanced chat browser to $uri")
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
        private val log = LoggerFactory.getLogger(LargeOutputChatAction::class.java)
    }
}