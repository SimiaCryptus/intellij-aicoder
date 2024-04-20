package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValue
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.chat.ChatSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File

class DebugChatAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    val path = "/codeChat"
    val model = AppSettingsState.instance.smartModel.chatModel()

    override fun handle(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        Thread {
            getDebugSessionInfo(project) { debugSession, debugSessionInfo ->
                val systemPrompt = stackToString(debugSessionInfo, debugSession)
                val userInterfacePrompt = ""
                val session = StorageInterface.newGlobalID()
                agents[session] = ChatSocketManager(
                    session = session,
                    model = model,
                    initialAssistantPrompt = "",
                    userInterfacePrompt = userInterfacePrompt,
                    systemPrompt = systemPrompt,
                    api = api,
                    storage = ApplicationServices.dataStorageFactory(root),
                    applicationClass = ApplicationServer::class.java,
                )

                val server = AppServer.getServer(e.project)
                val app = initApp(server, path)
                app.sessions[session] = app.newSession(null, session)

                Thread {
                    Thread.sleep(500)
                    try {
                        Desktop.getDesktop().browse(server.server.uri.resolve("$path/#$session"))
                    } catch (e: Throwable) {
                        log.warn("Error opening browser", e)
                    }
                }.start()
            }
        }.start()
    }

    private fun getDebugSessionInfo(project: Project?, callback: (XDebugSession, List<XStackFrame>)->Unit): Unit {
        val debugSession: XDebugSession? = XDebuggerManager.getInstance(project ?: return).currentSession
        val activeExecutionStack: XExecutionStack? = debugSession?.suspendContext?.activeExecutionStack
        val frames = mutableListOf<XStackFrame>()
        activeExecutionStack?.computeStackFrames(0, object : XExecutionStack.XStackFrameContainer {
            override fun errorOccurred(errorMessage: String) {
                println(errorMessage)
            }

            override fun addStackFrames(stackFrames: MutableList<out XStackFrame>, last: Boolean) {
                frames.addAll(stackFrames)
                if (last) {
                    Thread { callback(debugSession, frames) }.start()
                }
            }
        })
    }

    override fun isEnabled(event: AnActionEvent) = true

    companion object {
        private val log = LoggerFactory.getLogger(CodeChatAction::class.java)
        private val agents = mutableMapOf<Session, SocketManager>()
        val root: File get() = File(AppSettingsState.instance.pluginHome, "code_chat")
        private fun initApp(server: AppServer, path: String): ChatServer {
            server.appRegistry[path]?.let { return it }
            val socketServer = object : ApplicationServer(
                applicationName = "Code Chat",
                path = path,
                showMenubar = false,
            ) {
                override val singleInput = false
                override val stickyInput = true
                override fun newSession(user: User?, session: Session) =
                    agents[session] ?: throw IllegalArgumentException("Unknown session: $session")
            }
            server.addApp(path, socketServer)
            return socketServer
        }

        fun stackToString(stack: List<XStackFrame>, debugSession: XDebugSession) = stack.joinToString("\n\n") { frame: XStackFrame ->
            var file: VirtualFile? = null
            var line: Int? = null
            val codeContext = frame.sourcePosition?.let { sourcePosition: XSourcePosition ->
                file   = sourcePosition.file
                line = sourcePosition.line
                // Return surrounding 3 lines of code
                val lines = file!!.contentsToByteArray().toString(Charsets.UTF_8).split("\n")
                val start = 0.coerceAtLeast(line!! - 3)
                val end = lines.size.coerceAtMost(line!! + 3)
                lines.subList(start, end).joinToString("\n")
            }
            val variables = try {
                frame.javaClass.getDeclaredMethod("getVisibleVariables")
                    .apply { isAccessible = true }
                    .invoke(frame)?.let { it as List<LocalVariableProxyImpl> }
            } catch (e: Throwable) {
                log.warn("Error getting variables", e)
                emptyList()
            }
            val variableMap = try {
                variables?.associate { variable ->
                    variable.variable.name() to getValueSynchronous(debugSession, frame, variable)
                }
            } catch (e: Throwable) {
                log.warn("Error getting variable values", e)
                null
            }
            val final = """
            |# ${file?.path}:${line}
            |
            |Context:
            |```${file?.extension}
            |${codeContext}
            |```
            |
            |Variables:
            |${
                variableMap?.entries?.joinToString("\n") { (name, value) ->
                    "|$name = $value"
                }
            }
            """.trimMargin()
            final
        }

        private fun getValueSynchronous(
            debugSession: XDebugSession,
            frame: XStackFrame,
            variable: LocalVariableProxyImpl
        ): String? {
            try {
                var value: String? = null
                val semaphore = java.util.concurrent.Semaphore(0)
//                val evaluator: XDebuggerEvaluator = debugSession.debugProcess.evaluator ?: return null
                val evaluator: XDebuggerEvaluator = frame.evaluator ?: debugSession.debugProcess.evaluator ?: return null

//evaluator.(variable.variable.name())

                evaluator.evaluate(
                    variable.variable.name(),
                    object : XDebuggerEvaluator.XEvaluationCallback {
                        override fun errorOccurred(errorMessage: String) {
                            log.warn("Error evaluating variable", Throwable(errorMessage))
                        }

                        override fun evaluated(result: XValue) {
//                            result.instanceEvaluator.
                            val xExpressionPromise = result.calculateEvaluationExpression()
                            log.info("Evaluating variable: ${variable.variable.name()} = $xExpressionPromise")
                            xExpressionPromise.onSuccess {
                                log.info("Evaluated variable: ${variable.variable.name()} = ${it}")
                                value = result.toString()
                                semaphore.release()
                            }
                        }
                    },
//                    null
                    frame.sourcePosition
                )





                semaphore.tryAcquire(300L, java.util.concurrent.TimeUnit.SECONDS)
                return value
            } catch (e: Exception) {
                log.warn("Error evaluating variable", e)
                return null
            }
        }

    }
}