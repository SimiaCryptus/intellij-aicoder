package com.github.simiacryptus.aicoder.actions.knowledge

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.findRecursively
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.simiacryptus.skyenet.apps.parse.DocumentRecord
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.util.TensorflowProjector
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import org.slf4j.LoggerFactory
import kotlin.jvm.java

class CreateProjectorFromQueryIndexAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    if (!AppSettingsState.instance.devActions) return false
    val selectedFiles = UITools.getSelectedFiles(event)
    val processableFiles = selectedFiles.flatMap { file ->
      when {
        file.isDirectory -> file.findRecursively { it.name.endsWith(".index.data") }
        file.name.endsWith(".index.data") -> listOf(file)
        else -> emptyList()
      }
    }
    return processableFiles.isNotEmpty()
  }

  override fun handle(e: AnActionEvent) {
    val selectedFiles = UITools.getSelectedFiles(e)
    val processableFiles = selectedFiles.flatMap { file ->
      when {
        file.isDirectory -> file.findRecursively { it.name.endsWith(".index.data") }
        file.name.endsWith(".index.data") -> listOf(file)
        else -> emptyList()
      }
    }
    if (processableFiles.isEmpty()) {
      UITools.showErrorDialog(e.project, "Please select a valid query index file (.index.data).", "Invalid Selection")
      return
    }

    ProgressManager.getInstance().run(object : Task.Backgroundable(e.project, "Creating Projector") {
      override fun run(indicator: ProgressIndicator) {
        try {
          indicator.isIndeterminate = false
          indicator.fraction = 0.0

          val records = processableFiles.flatMap { DocumentRecord.readBinary(it.path) }
          val sessionID = Session.newGlobalID()

          ApplicationServer.appInfoMap[sessionID] = AppInfoData(
            applicationName = "Projector",
            singleInput = true,
            stickyInput = false,
            loadImages = false,
            showMenubar = false
          )

          SessionProxyServer.Companion.chats[sessionID] = object : ApplicationServer(
            applicationName = "Projector",
            path = "/projector",
            showMenubar = false,
          ) {
            override fun newSession(
              user: User?,
              session: Session
            ): SocketManager {
              val socketManager = super.newSession(user, session)
              val ui = (socketManager as ApplicationSocketManager).applicationInterface
              val projector = TensorflowProjector(api, dataStorage, sessionID, ui, null)
              val result = projector.writeTensorflowEmbeddingProjectorHtmlFromRecords(records)
              val task = ui.newTask(true)
              task.complete(result)
              return socketManager
            }
          }

          indicator.fraction = 1.0

          val server = AppServer.getServer(e.project)

          Thread {
            Thread.sleep(500)
            try {
              val uri = server.server.uri.resolve("/#$sessionID")
              BaseAction.log.info("Opening browser to $uri")
              browse(uri)
            } catch (e: Throwable) {
              log.warn("Error opening browser", e)
            }
          }.start()

        } catch (ex: Exception) {
          log.error("Error during projector creation", ex)
          UITools.showErrorDialog(e.project, "Error during projector creation: ${ex.message}", "Projector Creation Failed")
        }
      }
    })
  }

  companion object {
    private val log = LoggerFactory.getLogger(CreateProjectorFromQueryIndexAction::class.java)
  }
}