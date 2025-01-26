package aicoder.actions.legacy

import aicoder.actions.SelectionAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.aicoder.util.psi.PsiUtil
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil
import java.util.*

/**
 * Action that implements stub methods/classes using AI code generation.
 * Extends SelectionAction to handle code selection and language detection.
 */

class ImplementStubAction : SelectionAction<String>() {
  private val log = Logger.getInstance(ImplementStubAction::class.java)
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

  interface VirtualAPI {
    fun editCode(
      code: String,
      operation: String,
      computerLanguage: String,
      humanLanguage: String
    ): ConvertedText

    class ConvertedText {
      var code: String? = null
      var language: String? = null
    }
  }

  private fun getProxy(): VirtualAPI {
    return ChatProxy(
      clazz = VirtualAPI::class.java,
      api = api,
      model = AppSettingsState.instance.smartModel.chatModel(),
      temperature = AppSettingsState.instance.temperature,
      deserializerRetries = 5
    ).create()
  }

  override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
    if (computerLanguage == null) return false
    return computerLanguage != ComputerLanguage.Text
  }

  override fun defaultSelection(editorState: EditorState, offset: Int): Pair<Int, Int> {
    val codeRanges = editorState.contextRanges.filter { PsiUtil.matchesType(it.name, PsiUtil.ELEMENTS_CODE) }
    if (codeRanges.isEmpty()) return editorState.line
    return codeRanges.minByOrNull { it.length() }?.range() ?: editorState.line
  }

  override fun getConfig(project: Project?): String {
    return ""
  }

  override fun processSelection(state: SelectionState, config: String?, progress: ProgressIndicator): String {
    try {
      val code = state.selectedText ?: ""
      val settings = AppSettingsState.instance
      val outputHumanLanguage = settings.humanLanguage
      val computerLanguage = state.language ?: return code
      if (!isLanguageSupported(computerLanguage)) {
        UITools.showWarning(null, "Language ${computerLanguage.name} is not supported")
        return code
      }
      return processCode(code = code, state = state, computerLanguage = computerLanguage, outputHumanLanguage = outputHumanLanguage, progress = progress)
    } catch (e: Exception) {
      log.error("Error implementing stub", e)
      UITools.showError(null, "Failed to implement stub: ${e.message}")
      return state.selectedText ?: ""
    }
  }

  private fun processCode(
    code: String,
    state: SelectionState,
    computerLanguage: ComputerLanguage,
    outputHumanLanguage: String,
    progress: ProgressIndicator
  ): String {

    val codeContext = state.contextRanges.filter {
      PsiUtil.matchesType(it.name, PsiUtil.ELEMENTS_CODE)
    }
    var smallestIntersectingMethod = ""
    if (codeContext.isNotEmpty()) smallestIntersectingMethod =
      codeContext.minByOrNull { it.length() }?.subString(state.entireDocument ?: "") ?: ""

    var declaration = code
    declaration = StringUtil.stripSuffix(declaration.trim(), smallestIntersectingMethod)
    declaration = declaration.trim()

    return getProxy().editCode(
      declaration,
      "Implement Stub",
      computerLanguage.name.lowercase(Locale.ROOT),
      outputHumanLanguage
    ).code ?: ""
  }

}