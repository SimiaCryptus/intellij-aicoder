package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.ApplicationEvents
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.IdeaKotlinInterpreter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.apps.coding.CodingAgent
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File

class InternalCoderAction : BaseAction() {

  val path = "/internalCoder"

  override fun handle(e: AnActionEvent) {
    val session = StorageInterface.newGlobalID()
    val server = AppServer.getServer(e.project)
    val codingApp = initApp(server, path)
    val socketManager = codingApp.newSession(null, session)
    codingApp.sessions[session] = socketManager

    val symbols: MutableMap<String, Any> = mapOf(
      "event" to e,
    ).toMutableMap()
    e.getData(CommonDataKeys.EDITOR)?.apply { symbols["editor"] = this }
    e.getData(CommonDataKeys.PSI_FILE)?.apply { symbols["file"] = this }
    e.getData(CommonDataKeys.PSI_ELEMENT)?.apply { symbols["element"] = this }
    e.getData(CommonDataKeys.VIRTUAL_FILE)?.apply { symbols["virtualFile"] = this }
    e.project?.apply { symbols["project"] = this }
    e.getData(CommonDataKeys.SYMBOLS)?.apply { symbols["symbols"] = this }
    e.getData(CommonDataKeys.CARET)?.apply { symbols["psiElement"] = this }
    e.getData(CommonDataKeys.CARET)?.apply { symbols["psiElement"] = this }

    agents[session] = CodingAgent(
      api = api,
      dataStorage = ApplicationServices.dataStorageFactory(codingApp.root),
      session = session,
      user = null,
      ui = (socketManager as ApplicationSocketManager).applicationInterface,
      interpreter = IdeaKotlinInterpreter::class,
      symbols = symbols,
      temperature = 0.1,
      details = "Ensure that responses are printed; this is not a REPL."
    )

    Thread {
      Thread.sleep(500)
      try {
        Desktop.getDesktop().browse(server.server.uri.resolve("$path/#$session"))
      } catch (e: Throwable) {
        log.warn("Error opening browser", e)
      }
    }.start()
  }

  override fun isEnabled(event: AnActionEvent) = when {
    !AppSettingsState.instance.devActions -> false
    else -> true
  }

  companion object {
    private val log = LoggerFactory.getLogger(InternalCoderAction::class.java)
    private val agents = mutableMapOf<Session, CodingAgent<*>>()
    private fun initApp(server: AppServer, path: String): ApplicationServer {
      server.appRegistry[path]?.let { return it as ApplicationServer }
      val codingApp = object : ApplicationServer(
        applicationName = "IntelliJ Internal Coding Agent",
      ) {
        override fun userMessage(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API) {
          agents[session]?.start(userMessage)
        }
        override val root: File get() = File(ApplicationEvents.pluginHome, "coding_agent")
      }
      server.addApp(path, codingApp)
      return codingApp
    }
  }
}
