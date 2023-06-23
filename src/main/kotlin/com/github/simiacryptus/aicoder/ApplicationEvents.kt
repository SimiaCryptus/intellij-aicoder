package com.github.simiacryptus.aicoder

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import com.simiacryptus.openai.OpenAIClient
import java.io.File

class ApplicationEvents : ApplicationActivationListener {
    override fun applicationActivated(ideFrame: IdeFrame) {
        if(AppSettingsState.instance.apiLog) {
            //val baseDir = System.getProperty("java.io.tmpdir")
            // baseDir is the canonical idea log directory
            var logPath = System.getProperty("idea.log.path")
            if(logPath == null) {
                logPath = System.getProperty("java.io.tmpdir")
            }
            if(logPath == null) {
                logPath = System.getProperty("user.home")
            }
            val baseDir = File(logPath)
            val file = File(baseDir, "openai.log")
            file.deleteOnExit()
            OpenAIClient.auxillaryLog = file
        }
        super.applicationActivated(ideFrame)
    }
}
