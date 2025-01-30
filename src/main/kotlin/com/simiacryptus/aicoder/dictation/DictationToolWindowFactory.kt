package com.simiacryptus.aicoder.dictation

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory


class DictationToolWindowFactory : ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager
    contentManager.addContent(contentManager.factory.createContent(ControlPanel(project), "Controls", false))
    contentManager.addContent(contentManager.factory.createContent(SettingsPanel(project), "Settings", false))
    contentManager.addContent(contentManager.factory.createContent(EventPanel(), "Debug", false))
    toolWindow.isShowStripeButton = true
  }

  override fun shouldBeAvailable(project: Project): Boolean {
    return true
  }
}