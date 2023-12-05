package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.IdeaKotlinInterpreter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.skyenet.apps.coding.CodingApp
import com.simiacryptus.skyenet.core.platform.*
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.util.*

class InternalCoderAction : BaseAction() {

  override fun handle(e: AnActionEvent) {
    val server = AppServer.getServer(e.project)
    val uuid = UUID.randomUUID().toString()
    val symbols: MutableMap<String, Any> = mapOf(
      "event" to e,
    ).toMutableMap()

    // TODO: Set this up at startup and lock ApplicationServices
    ApplicationServices.clientManager = object : ClientManager() {
      override fun createClient(session: Session, user: User?, dataStorage: StorageInterface?) =
        this@InternalCoderAction.api
    }
    IdeaKotlinInterpreter.project = e.getData(CommonDataKeys.PROJECT) ?: throw IllegalStateException("No project")

    e.getData(CommonDataKeys.EDITOR)?.apply { symbols["editor"] = this }
    e.getData(CommonDataKeys.PSI_FILE)?.apply { symbols["file"] = this }
    e.getData(CommonDataKeys.PSI_ELEMENT)?.apply { symbols["element"] = this }
    e.getData(CommonDataKeys.VIRTUAL_FILE)?.apply { symbols["virtualFile"] = this }
    IdeaKotlinInterpreter.project?.apply { symbols["project"] = this }
    e.getData(CommonDataKeys.SYMBOLS)?.apply { symbols["symbols"] = this }
    e.getData(CommonDataKeys.CARET)?.apply { symbols["psiElement"] = this }
    e.getData(CommonDataKeys.CARET)?.apply { symbols["psiElement"] = this }
    server.addApp(
      "/$uuid", CodingApp(
        "IntelliJ Internal Coding Agent",
        IdeaKotlinInterpreter::class,
        symbols
      )
    )
    Thread {
      Thread.sleep(500)
      try {
        Desktop.getDesktop().browse(server.server.uri.resolve("/$uuid/index.html"))
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

  }
}
