package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.FormBuilder
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.Description
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.OutputInterceptor
import com.simiacryptus.skyenet.body.ClasspathResource
import com.simiacryptus.skyenet.body.SessionServerUtil.asJava
import com.simiacryptus.skyenet.body.SkyenetSessionServer
import com.simiacryptus.skyenet.heart.WeakGroovyInterpreter
import com.simiacryptus.skyenet.util.AbbrevWhitelistYamlDescriber
import org.apache.commons.io.output.NullOutputStream
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.jdesktop.swingx.JXButton
import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.Map
import java.util.function.Supplier

class LaunchSkyenetAction : AnAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled()
        super.update(e)
    }

    interface TestTools {
        fun getProject(): Project
        fun getSelectedFolder(): VirtualFile
        @Description("Prints to script output")
        fun print(msg:String): Unit
    }

    override fun actionPerformed(e: AnActionEvent) {
        // Random port from range 8000-9000
        val port = (8000 + (Math.random() * 1000).toInt())
        val skyenet = object : SkyenetSessionServer(
            applicationName = "IdeaAgent",
            yamlDescriber = AbbrevWhitelistYamlDescriber(
                "com.simiacryptus",
            ),
            baseURL = "http://localhost:$port",
            model = AppSettingsState.instance.model_chat
        ) {
            override val baseResource: Resource?
                get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase))
            override val api: OpenAIClient
                get() = OpenAIClient(
                    AppSettingsState.instance.apiKey,
                    AppSettingsState.instance.apiBase,
                    AppSettingsState.instance.apiLogLevel
                )

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

            override fun heart(hands: java.util.Map<String, Object>): Heart = object : WeakGroovyInterpreter(hands) {
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
                log.info("Server Running on $port")
                server.join()
            } finally {
                log.info("Server Stopped")
            }
        }.start()

        Thread {
            try {
                val url = server.uri
                val formBuilder = FormBuilder.createFormBuilder()
                val openButton = JXButton("Open")
                openButton.addActionListener {
                    Desktop.getDesktop().browse(url.resolve("/index.html"))
                }
                formBuilder.addLabeledComponent("Server Running on $port", openButton)
                val showOptionDialog =
                    UITools.showOptionDialog(formBuilder.panel, "Close", title = "Server Running on $port", modal = false)
                log.info("showOptionDialog = $showOptionDialog")
            } finally {
                log.info("Stopping Server")
                server.stop()
            }
        }.start()
    }

    private fun isEnabled(): Boolean {
        if (UITools.isSanctioned()) return false
        //if (!AppSettingsState.instance.devActions) return false
        return true
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LaunchSkyenetAction::class.java)
    }

}