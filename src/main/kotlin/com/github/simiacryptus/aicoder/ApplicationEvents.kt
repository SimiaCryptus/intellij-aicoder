package com.github.simiacryptus.aicoder

import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import com.simiacryptus.skyenet.core.OutputInterceptor
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.file.UsageManager
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class ApplicationEvents : ApplicationActivationListener {
    override fun applicationActivated(ideFrame: IdeFrame) {
        val currentThread = Thread.currentThread()
        val prevClassLoader = currentThread.contextClassLoader
        try {
            currentThread.contextClassLoader = ApplicationEvents::class.java.classLoader
            init(ideFrame)
        } finally {
            currentThread.contextClassLoader = prevClassLoader
        }
        super.applicationActivated(ideFrame)
    }

    private val isInitialized = AtomicBoolean(false)

    private fun init(ideFrame: IdeFrame) {
        if (isInitialized.getAndSet(true)) return
        OutputInterceptor.setupInterceptor()
        ApplicationServices.clientManager = object : ClientManager() {
            override fun createClient(session: Session, user: User?, dataStorage: StorageInterface?) =
                IdeaOpenAIClient.instance
        }
        ApplicationServices.usageManager = UsageManager(File(pluginHome, "usage"))
        ApplicationServices.authorizationManager = object : AuthorizationInterface {
            override fun isAuthorized(
                applicationClass: Class<*>?,
                user: User?,
                operationType: AuthorizationInterface.OperationType
            ) = true
        }
        ApplicationServices.authenticationManager = object : AuthenticationInterface {
            override fun getUser(accessToken: String?) = null
            override fun putUser(accessToken: String, user: User) = user
            override fun logout(accessToken: String, user: User) {}
        }
        ApplicationServices.isLocked = true
    }

    companion object {
        val pluginHome by lazy {
                var logPath = System.getProperty("idea.plugins.path")
                if (logPath == null) {
                    logPath = System.getProperty("java.io.tmpdir")
                }
                if (logPath == null) {
                    logPath = System.getProperty("user.home")
                }
                File(logPath, "AICodingAsst")
            }

    }
}

