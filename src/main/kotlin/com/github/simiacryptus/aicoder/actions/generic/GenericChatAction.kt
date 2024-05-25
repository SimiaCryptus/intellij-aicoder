package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.BaseAction.Companion
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import org.slf4j.LoggerFactory
import java.awt.Desktop

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

        val server = AppServer.getServer(e.project)

        Thread {
            Thread.sleep(500)
            try {

                val uri = server.server.uri.resolve("/#$session")
                BaseAction.log.info("Opening browser to $uri")
                Desktop.getDesktop().browse(uri)
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
