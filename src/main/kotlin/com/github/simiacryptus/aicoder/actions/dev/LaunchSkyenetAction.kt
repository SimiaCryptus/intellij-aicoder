@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.FormBuilder
import com.simiacryptus.openai.APIClientBase
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.OutputInterceptor
import com.simiacryptus.skyenet.body.ClasspathResource
import com.simiacryptus.skyenet.body.SessionServerUtil.asJava
import com.simiacryptus.skyenet.body.SkyenetCodingSessionServer
import com.simiacryptus.skyenet.heart.WeakGroovyInterpreter
import com.simiacryptus.util.describe.Description
import org.eclipse.jetty.util.resource.Resource
import org.jdesktop.swingx.JXButton
import java.awt.Desktop
import java.util.Map
import java.util.function.Supplier

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
        // Random port from range 8000-9000
        val port = (8000 + (Math.random() * 1000).toInt())
        val skyenet = object : SkyenetCodingSessionServer(
            applicationName = "IdeaAgent",
            baseURL = "http://localhost:$port",
            model = AppSettingsState.instance.defaultChatModel(),
            apiKey = AppSettingsState.instance.apiKey
        ) {
            override val baseResource: Resource
                get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase))

            override fun hands() = Map.of(
                "ide",
                object : TestTools {
                    override fun getProject(): Project {
                        return e.project!!
                    }

                    override fun getSelectedFolder(): VirtualFile {
                        return UITools.getSelectedFolder(e)!!
                    }

                    val toolStream =
                        OutputInterceptor.createInterceptorStream(
                            System.out
                            //PrintStream(NullOutputStream.NULL_OUTPUT_STREAM)
                        )

                    override fun print(msg: String) {
                        toolStream.println(msg)
                    }
                } as Object,
            ).asJava

            override fun toString(e: Throwable): String {
                return e.message ?: e.toString()
            }

            override fun heart(hands: Map<String, Object>): Heart = object : WeakGroovyInterpreter(hands) {
                override fun <T : Any> wrapExecution(fn: Supplier<T?>): T? {
                    return UITools.run(
                        e.project, "Running Script", false
                    ) {
                        fn.get()
                    }
                }
            }
        }
        val server = skyenet.start(port)


        Thread {
            try {
                UITools.run(
                    e.project, "Running Skyenet Server on $port", false
                ) {
                    while (!it.isCanceled && server.isRunning) {
                        Thread.sleep(1000)
                    }
                    if(it.isCanceled) {
                        log.info("Server cancelled")
                        server.stop()
                    } else {
                        log.info("Server stopped")
                    }
                }
            } finally {
                log.info("Stopping Server")
                server.stop()
            }
        }.start()

        Thread {
            Thread.sleep(500)
            try {
                Desktop.getDesktop().browse(server.uri.resolve("/index.html"))
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
