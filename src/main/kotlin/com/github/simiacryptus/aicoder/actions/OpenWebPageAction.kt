package com.github.simiacryptus.aicoder.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.awt.Desktop
import java.net.URI

class OpenWebPageAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI("http://apps.simiacrypt.us/"))
            }
        }
    }
}
