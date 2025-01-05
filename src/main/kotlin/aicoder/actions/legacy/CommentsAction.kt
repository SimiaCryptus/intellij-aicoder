package aicoder.actions.legacy

import aicoder.actions.SelectionAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy

class CommentsAction : SelectionAction<String>() {
  private val log = Logger.getInstance(CommentsAction::class.java)

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    return AppSettingsState.instance.enableLegacyActions
  }

  override fun getConfig(project: Project?): String {
    return ""
  }

  override fun processSelection(state: SelectionState, config: String?, progress: ProgressIndicator): String {
    try {
      val selectedText = state.selectedText ?: return ""
      val language = state.language?.toString() ?: state.editor?.virtualFile?.extension ?: return selectedText
      return ChatProxy(
        clazz = CommentsAction_VirtualAPI::class.java,
        api = api,
        model = AppSettingsState.instance.smartModel.chatModel(),
        temperature = AppSettingsState.instance.temperature,
        deserializerRetries = 5
      ).create().editCode(
        selectedText,
        "Add comments to each line explaining the code",
        language,
        AppSettingsState.instance.humanLanguage
      ).code ?: selectedText
    } catch (e: Exception) {
      log.error("Failed to process comments", e)
      throw e
    }
  }

  interface CommentsAction_VirtualAPI {
    fun editCode(
      code: String,
      operations: String,
      computerLanguage: String,
      humanLanguage: String
    ): CommentsAction_ConvertedText

    class CommentsAction_ConvertedText {
      var code: String? = null
      var language: String? = null
    }
  }
}