package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import org.slf4j.LoggerFactory

class GenericChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/codeChat"
    val systemPrompt = ""
    val userInterfacePrompt = ""
    val model = AppSettingsState.instance.smartModel.chatModel()

    override fun handle(e: AnActionEvent) {
        val session = StorageInterface.newGlobalID()
        SessionProxyServer.agents[session] = ChatSocketManager(
            session = session,
            model = model,
            initialAssistantPrompt = "",
            userInterfacePrompt = userInterfacePrompt,
            systemPrompt = systemPrompt,
            api = api,
            storage = ApplicationServices.dataStorageFactory(AppSettingsState.instance.pluginHome),
            applicationClass = ApplicationServer::class.java,
        )
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Code Chat",
            singleInput = false,
            stickyInput = true,
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
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(CodeChatAction::class.java)
    }
}
