package aicoder.actions.legacy

import aicoder.actions.SelectionAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.ApiModel.*
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList

/**
 * Action that appends AI-generated text to the current selection.
 * Uses ChatGPT to generate contextually relevant continuations of the selected text.
 *
 * @see SelectionAction
 */

class AppendTextWithChatAction : SelectionAction<String>() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

  override fun getConfig(project: Project?): String {
    return ""
  }

  override fun processSelection(state: SelectionState, config: String?, progress: ProgressIndicator): String {
    try {
      val settings = AppSettingsState.instance
      val request = ChatRequest(
        model = settings.smartModel,
        temperature = settings.temperature
      ).copy(
        temperature = settings.temperature,
        messages = listOf(
          ChatMessage(Role.system, "Append text to the end of the user's prompt".toContentList(), null),
          ChatMessage(Role.user, state.selectedText.toString().toContentList(), null)
        ),
      )
      val chatResponse = api.chat(request, settings.smartModel.chatModel())
      val originalText = state.selectedText ?: ""
      val generatedText = chatResponse.choices[0].message?.content ?: ""
      // Remove duplicate text if AI response includes the original text
      return originalText + if (generatedText.startsWith(originalText))
        generatedText.substring(originalText.length) else generatedText
    } catch (e: Exception) {
      UITools.error(log, "Failed to generate text continuation", e)
      return state.selectedText ?: ""
    }
  }
}