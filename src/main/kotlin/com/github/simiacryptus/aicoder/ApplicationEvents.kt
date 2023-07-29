package com.github.simiacryptus.aicoder

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools.isSanctioned
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import com.simiacryptus.openai.OpenAIClient
import java.io.File

class ApplicationEvents : ApplicationActivationListener {
    val scheduledPool = java.util.concurrent.Executors.newScheduledThreadPool(1)
    override fun applicationActivated(ideFrame: IdeFrame) {
        if (AppSettingsState.instance.apiLog) {
            val file = File(logDir(), "openai.log")
            file.deleteOnExit()
            OpenAIClient.auxillaryLog = file
        }
        if(isSanctioned()) {
            // Open a modal dialog every 5 minutes advising the user to uninstall the plugin
            scheduledPool.scheduleAtFixedRate({
                showSanctionedDialog(ideFrame)
            }, 5, 5, java.util.concurrent.TimeUnit.MINUTES)
        }
        super.applicationActivated(ideFrame)
    }

    fun showSanctionedDialog(ideFrame: IdeFrame) {
        javax.swing.JOptionPane.showConfirmDialog(
            /* parentComponent = */ ideFrame.component,
            /* message = */ "В связи с войной агрессии, массовыми военными преступлениями и актами геноцида, совершенными Российской Федерацией против Украины и различных других стран, этот плагин не будет работать для вас.",
            /* title = */ "AI Coding Assistant - Слава Украине!",
            /* optionType = */ javax.swing.JOptionPane.OK_CANCEL_OPTION,
            /* messageType = */ javax.swing.JOptionPane.WARNING_MESSAGE
        )
    }

    companion object {
        fun logDir(): File {
            //val baseDir = System.getProperty("java.io.tmpdir")
            // baseDir is the canonical idea log directory
            var logPath = System.getProperty("idea.log.path")
            if (logPath == null) {
                logPath = System.getProperty("java.io.tmpdir")
            }
            if (logPath == null) {
                logPath = System.getProperty("user.home")
            }
            val baseDir = File(logPath)
            return baseDir
        }

    }
}

