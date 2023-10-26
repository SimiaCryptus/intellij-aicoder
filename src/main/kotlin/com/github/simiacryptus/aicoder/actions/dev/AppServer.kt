package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.skyenet.body.WebSocketServer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class AppServer(
    val localName: String,
    val port: Int,
    project: Project?
) {

    val log by lazy { LoggerFactory.getLogger(javaClass) }

    var domainName: String = "http://$localName:$port"

    private val contexts by lazy {
        val contexts = ContextHandlerCollection()
        contexts.handlers = handlers.toTypedArray()
        contexts
    }

    @Synchronized
    fun addApp(path: String, socketServer: WebSocketServer) {
        try {
            synchronized(serverLock) {
                if (server.isRunning) server.stop() // Stop the server
                handlers += newWebAppContext(socketServer, path)
                contexts.handlers = handlers.toTypedArray()
                server.handler = contexts
                server.start() // Start the server again to reflect the new context
            }
        } catch (e: Exception) {
            log.error("Error while restarting the server with new context", e)
        }
    }

    fun newWebAppContext(server: WebSocketServer, path: String): WebAppContext {
        val context = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(context, null)
        context.baseResource = server.baseResource
        context.contextPath = path
        context.welcomeFiles = arrayOf("index.html")
        val webAppContext = context
        server.configure(webAppContext, baseUrl = "$domainName/$path")
        return webAppContext
    }

    private val handlers = arrayOf<WebAppContext>().toMutableList()

    val server by lazy {
        val server = Server(InetSocketAddress(localName, port))
        server.handler = contexts
        server
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

    fun isRunning(it: ProgressIndicator) = synchronized(serverLock) { !it.isCanceled && server.isRunning }
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

        fun stop() {
            server?.server?.stop()
            server = null
        }
    }

}