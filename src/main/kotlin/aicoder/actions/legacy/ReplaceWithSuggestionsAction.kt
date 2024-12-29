package aicoder.actions.legacy

import aicoder.actions.SelectionAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

open class ReplaceWithSuggestionsAction : SelectionAction<String>() {
  private val log = Logger.getInstance(ReplaceWithSuggestionsAction::class.java)
  private val templateText = "Generating suggestions..."

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

  interface VirtualAPI {
    fun suggestText(template: String, examples: List<String>): Suggestions

    class Suggestions {
      var choices: List<String>? = null
    }
  }

  val proxy: VirtualAPI
    get() {
      return ChatProxy(
        clazz = VirtualAPI::class.java,
        api = api,
        model = AppSettingsState.instance.smartModel.chatModel(),
        temperature = AppSettingsState.instance.temperature,
        deserializerRetries = 5
      ).create()
    }

  override fun getConfig(project: Project?): String {
    // Could be enhanced to get user preferences for suggestion generation
    return ""
  }

  override fun processSelection(event: AnActionEvent?, state: SelectionState, config: String?): String {
    try {
      val choices: List<String> = UITools.run(event?.project, templateText, true, true) { progress ->
        progress.isIndeterminate = false
        progress.text = "Analyzing context..."
        progress.fraction = 0.2
        val selectedText = state.selectedText ?: return@run emptyList()
        val idealLength = 2.0.pow(2 + ceil(ln(selectedText.length.toDouble()))).toInt()
        progress.text = "Preparing context..."
        progress.fraction = 0.4
        val selectionStart = state.selectionOffset
        val allBefore = state.entireDocument?.substring(0, selectionStart) ?: ""
        val selectionEnd = state.selectionOffset + (state.selectionLength ?: 0)
        val allAfter = state.entireDocument?.substring(selectionEnd, state.entireDocument.length) ?: ""
        val before = StringUtil.getSuffixForContext(allBefore, idealLength).toString().replace('\n', ' ')
        val after = StringUtil.getPrefixForContext(allAfter, idealLength).toString().replace('\n', ' ')
        progress.text = "Generating suggestions..."
        progress.fraction = 0.6
        proxy.suggestText(
          "$before _____ $after",
          listOf(selectedText)
        ).choices ?: emptyList()
      }
      return choose(choices)
    } catch (e: Exception) {
      log.error("Failed to generate suggestions", e)
      UITools.showErrorDialog(
        "Failed to generate suggestions: ${e.message}",
        "Error"
      )
      return state.selectedText ?: ""
    }
  }

  open fun choose(choices: List<String>): String {
    return UITools.showRadioButtonDialog("Select an option to fill in the blank:", *choices.toTypedArray())
      ?.toString() ?: ""
  }
}