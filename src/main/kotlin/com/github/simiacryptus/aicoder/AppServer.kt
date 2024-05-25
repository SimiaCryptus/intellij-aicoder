package com.github.simiacryptus.aicoder

import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.skyenet.webui.chat.ChatServer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class AppServer(
    private val localName: String,
    private val port: Int,
    project: Project?
) {

    private val log by lazy { LoggerFactory.getLogger(javaClass) }

    val server by lazy {
        val server = Server(InetSocketAddress(localName, port))
        server.handler = contexts
        server
    }

    private val handlers = arrayOf<WebAppContext>(
        newWebAppContext(SessionProxyServer(), "/")
    ).toMutableList()

    private val contexts by lazy {
        val contexts = ContextHandlerCollection()
        contexts.handlers = handlers.toTypedArray()
        contexts
    }

    private fun newWebAppContext(server: ChatServer, path: String): WebAppContext {
        val context = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(context, null)
        context.baseResource = server.baseResource
        context.classLoader = AppServer::class.java.classLoader
        context.contextPath = path
        context.welcomeFiles = arrayOf("index.html")
        server.configure(context)
        return context
    }


    private val serverLock = Object()
    private val progressThread = Thread {
        try {
            UITools.run(
                project, "Running CodeChat Server on $port", false
            ) {
                while (isRunning(it)) {
                    Thread.sleep(1000)
                }
                synchronized(serverLock) {
                    if (it.isCanceled) {
                        log.info("Server cancelled")
                        server.stop()
                    } else {
                        log.info("Server stopped")
                    }
                }
            }
        } finally {
            log.info("Stopping Server")
            server.stop()
        }
    }

    private fun isRunning(it: ProgressIndicator) = synchronized(serverLock) { !it.isCanceled && server.isRunning }
    fun start() {
        server.start()
        progressThread.start()
    }

    companion object {
        @Transient
        private var server: AppServer? = null
        fun getServer(project: Project?): AppServer {
            if (null == server || !server!!.server.isRunning) {
                server = AppServer(
                    AppSettingsState.instance.listeningEndpoint,
                    AppSettingsState.instance.listeningPort,
                    project
                )
                server!!.start()
            }
            return server!!
        }

    }

}