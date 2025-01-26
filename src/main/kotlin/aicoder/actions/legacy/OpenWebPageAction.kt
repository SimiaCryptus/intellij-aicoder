package aicoder.actions.legacy

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import java.net.URI

class OpenWebPageAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    browse(URI("http://apps.simiacrypt.us/"))
  }

}
