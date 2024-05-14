package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.simiacryptus.skyenet.core.OutputInterceptor
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.platform.file.UsageManager
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible

class PluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Check if this is the first run after installation
        try {
            val currentThread = Thread.currentThread()
            val prevClassLoader = currentThread.contextClassLoader
            try {
                currentThread.contextClassLoader = PluginStartupActivity::class.java.classLoader
                init()
            } finally {
                currentThread.contextClassLoader = prevClassLoader
            }

            if (AppSettingsState.instance.showWelcomeScreen || AppSettingsState.instance.greetedVersion != AppSettingsState.WELCOME_VERSION) {
                val welcomeFile = "welcomePage.md"
                val resource = PluginStartupActivity::class.java.classLoader.getResource(welcomeFile)
                var virtualFile = resource?.let { VirtualFileManager.getInstance().findFileByUrl(it.toString()) }
                if (virtualFile == null) try {
                    val path = resource?.toURI()?.let { java.nio.file.Paths.get(it) }
                    virtualFile = path?.let { VirtualFileManager.getInstance().findFileByNioPath(it) }
                } catch (e: Exception) {
                    log.error("Error opening welcome page", e)
                }
                if (virtualFile == null) {
                    try {
                        val tempFile =
                            File.createTempFile(welcomeFile.substringBefore("."), "." + welcomeFile.substringAfter("."))
                        tempFile.deleteOnExit()
                        resource?.openStream()?.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        virtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(tempFile.toPath())
                    } catch (e: Exception) {
                        log.error("Error opening welcome page", e)
                    }
                }
                virtualFile?.let {
                    try {
                        ApplicationManager.getApplication().invokeLater {
                            FileEditorManager.getInstance(project).openFile(it, true).forEach { editor ->
                                try {
                                    editor::class.declaredMembers.filter { it.name == "setLayout" }.forEach { member ->
                                        member.isAccessible = true
                                        member.call(editor, TextEditorWithPreview.Layout.SHOW_PREVIEW)
                                    }
                                } catch (e: Exception) {
                                    log.error("Error opening welcome page", e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        log.error("Error opening welcome page", e)
                    }
                } ?: log.error("Welcome page not found")
                // Set showWelcomeScreen to false after showing it for the first time
                AppSettingsState.instance.greetedVersion = AppSettingsState.WELCOME_VERSION
                AppSettingsState.instance.showWelcomeScreen = false
            }
        } catch (e: Exception) {
            log.error("Error during plugin startup", e)
        }
    }

    private val isInitialized = AtomicBoolean(false)

    private fun init() {
        if (isInitialized.getAndSet(true)) return
        OutputInterceptor.setupInterceptor()
        ApplicationServices.clientManager = object : ClientManager() {
            override fun createClient(session: Session, user: User?, dataStorage: StorageInterface?) =
                IdeaOpenAIClient.instance
        }
        ApplicationServices.usageManager = UsageManager(File(AppSettingsState.instance.pluginHome, "usage"))
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
        val resolve = AppSettingsState.instance.pluginHome.resolve(".sys")
        DataStorage.SYS_DIR = resolve.apply { mkdirs() }
    }


    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(PluginStartupActivity::class.java)
    }
}