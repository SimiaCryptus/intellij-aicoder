@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.util.describe.Description
import java.awt.Desktop
import java.util.UUID

class LaunchSkyenetAction : BaseAction() {

    override fun isEnabled(event: AnActionEvent): Boolean {
        return isEnabled()
    }

    interface TestTools {
        fun getProject(): Project
        fun getSelectedFolder(): VirtualFile

        @Description("Prints to script output")
        fun print(msg: String): Unit
    }

    override fun handle(e: AnActionEvent) {
        val project = e.project!!
        val selectedFolder = UITools.getSelectedFolder(e)!!
        val server = AppServer.getServer(e.project)
        val uuid = UUID.randomUUID().toString()
        server.addApp("/$uuid", SkyenetProjectCodingSessionServer(project, selectedFolder))
        Thread {
            Thread.sleep(500)
            try {
                Desktop.getDesktop().browse(server.server.uri.resolve("/$uuid/index.html"))
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    private fun isEnabled(): Boolean {
        if (UITools.isSanctioned()) return false
        return AppSettingsState.instance.devActions
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LaunchSkyenetAction::class.java)
    }

}
