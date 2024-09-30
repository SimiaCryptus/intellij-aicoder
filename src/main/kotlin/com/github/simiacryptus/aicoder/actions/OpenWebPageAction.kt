package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.util.BrowseUtil
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.net.URI

class OpenWebPageAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        browse(URI("http://apps.simiacrypt.us/"))
    }

}
