package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.code.CodingAgent
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.interpreter.ProcessInterpreter
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SessionTask

class ShellCommandAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent): Boolean {
        return UITools.getSelectedFolder(event) != null
    }

    override fun handle(e: AnActionEvent) {
        val project = e.project
        val selectedFolder = UITools.getSelectedFolder(e)?.toFile
        if (selectedFolder == null) {
            Messages.showErrorDialog(project, "Please select a directory", "Error")
            return
        }
        val session = Session.newGlobalID()
        ApplicationServer.appInfoMap[session] = AppInfoData(
            applicationName = "Code Chat",
            singleInput = true,
            stickyInput = false,
            loadImages = false,
            showMenubar = false
        )
        SessionProxyServer.chats[session] = object : ApplicationServer(
            applicationName = "Shell Agent",
            path = "/shellAgent",
            showMenubar = false,
        ) {
            override val singleInput = true
            override val stickyInput = false


            override fun userMessage(
                session: Session,
                user: User?,
                userMessage: String,
                ui: ApplicationInterface,
                api: API
            ) {
                val task = ui.newTask()
                val agent = object : CodingAgent<ProcessInterpreter>(
                    api = api,
                    dataStorage = dataStorage,
                    session = session,
                    user = user,
                    ui = ui,
                    interpreter = ProcessInterpreter::class,
                    symbols = mapOf(
                        "workingDir" to selectedFolder.absolutePath,
                        "language" to if (isWindows) "powershell" else "bash",
                        "command" to listOf(AppSettingsState.instance.shellCommand)
                    ),
                    temperature = AppSettingsState.instance.temperature,
                    details = """
                        Execute the following shell command(s) in the specified directory and provide the output.
                        Ensure to handle any errors or exceptions gracefully.
                    """.trimIndent(),
                    model = AppSettingsState.instance.smartModel.chatModel(),
                    mainTask = task,
                ) {
                    override fun displayFeedback(
                        task: SessionTask,
                        request: CodingActor.CodeRequest,
                        response: CodingActor.CodeResult
                    ) {
                        val formText = StringBuilder()
                        var formHandle: StringBuilder? = null
                        formHandle = task.add(
                            """
                      |<div style="display: flex;flex-direction: column;">
                      |${
                                if (!super.canPlay) "" else super.playButton(
                                    task,
                                    request,
                                    response,
                                    formText
                                ) { formHandle!! }
                            }
                      |${acceptButton(task, request, response, formText) { formHandle!! }}
                      |</div>
                      |${super.reviseMsg(task, request, response, formText) { formHandle!! }}
                      """.trimMargin(), className = "reply-message"
                        )
                        formText.append(formHandle.toString())
                        formHandle.toString()
                        task.complete()
                    }

                    fun acceptButton(
                        task: SessionTask,
                        request: CodingActor.CodeRequest,
                        response: CodingActor.CodeResult,
                        formText: StringBuilder,
                        formHandle: () -> StringBuilder
                    ): String {
                        return ui.hrefLink("Accept", "href-link play-button") {
                        }
                    }
                }.apply {
                    this.start(userMessage)
                }
            }
        }

        val server = AppServer.getServer(e.project)

        Thread {
            Thread.sleep(500)
            try {
                val uri = server.server.uri.resolve("/#$session")
                log.info("Opening browser to $uri")
                browse(uri)
            } catch (e: Throwable) {
                log.warn("Error opening browser", e)
            }
        }.start()
    }

    companion object {
        private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    }
}
