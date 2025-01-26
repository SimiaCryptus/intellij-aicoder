package com.simiacryptus.aicoder.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory


class DictationSettingsToolWindowFactory : ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.contentManager.addContent(toolWindow.contentManager.factory.createContent(DictationSettingsPanel(project), "Settings", false))
    toolWindow.isShowStripeButton = true
  }

  override fun shouldBeAvailable(project: Project): Boolean {
    return true
  }
}