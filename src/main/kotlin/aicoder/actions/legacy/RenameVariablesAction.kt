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

/**
 * Action to suggest and apply variable name improvements in code.
 * Supports multiple programming languages and uses AI to generate naming suggestions.
 */

open class RenameVariablesAction : SelectionAction<String>() {
  private val log = Logger.getInstance(RenameVariablesAction::class.java)

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

  interface RenameAPI {
    fun suggestRenames(
      code: String,
      computerLanguage: String,
      humanLanguage: String
    ): SuggestionResponse

    class SuggestionResponse {
      var suggestions: MutableList<Suggestion> = mutableListOf()

      class Suggestion {
        var originalName: String? = null
        var suggestedName: String? = null
      }
    }
  }

  val proxy: RenameAPI
    get() {
      return ChatProxy(
        clazz = RenameAPI::class.java,
        api = api,
        model = AppSettingsState.instance.smartModel.chatModel(),
        temperature = AppSettingsState.instance.temperature,
        deserializerRetries = 5
      ).create()
    }

  override fun getConfig(project: Project?): String {
    return ""
  }

  @Throws(Exception::class)
  override fun processSelection(event: AnActionEvent?, state: SelectionState, config: String?): String {
    try {
      val renameSuggestions = UITools.run(event?.project, "Analyzing Code", true, true) { progress ->
        progress.text = "Generating rename suggestions..."
        proxy
          .suggestRenames(
            state.selectedText ?: "",
            state.language?.name ?: state.editor?.virtualFile?.extension ?: "",
            AppSettingsState.instance.humanLanguage
          )
          .suggestions
          .filter { it.originalName != null && it.suggestedName != null }
          .associate { it.originalName!! to it.suggestedName!! }
      }
      if (renameSuggestions.isEmpty()) {
        UITools.showInfoMessage("No rename suggestions found", "No Changes")
        return state.selectedText ?: ""
      }
      val selectedSuggestions = Companion.choose(renameSuggestions)
      return UITools.run(event?.project, "Applying Changes", true, true) { progress ->
        progress.text = "Applying selected renames..."
        var selectedText = state.selectedText
        val filter = renameSuggestions.filter { it.key in selectedSuggestions }
        filter.forEach { (key, value) ->
          selectedText = selectedText?.replace(key, value)
        }
        selectedText ?: ""
      }
    } catch (e: Exception) {
      log.error("Error during rename operation", e)
      UITools.showErrorDialog("Failed to process rename operation: ${e.message}", "Error")
      throw e
    }
  }

  companion object {
    fun choose(renameSuggestions: Map<String, String>): Set<String> {
      return UITools.showCheckboxDialog(
        "Select which items to rename",
        renameSuggestions.keys.toTypedArray(),
        renameSuggestions.map { (key, value) -> "$key -> $value" }.toTypedArray()
      ).toSet()
    }
  }
}