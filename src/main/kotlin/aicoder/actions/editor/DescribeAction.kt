package aicoder.actions.editor

import aicoder.actions.SelectionAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.aicoder.util.IndentedText
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy

// ... keep existing imports

class DescribeAction : SelectionAction<String>() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  private val log = Logger.getInstance(DescribeAction::class.java)

  data class SelectionState(
    val language: ComputerLanguage?,
    val selectedText: String?,
    val indent: String?
  )

  protected fun getState(event: AnActionEvent): SelectionState? {
    val editor: Editor = event.getData(CommonDataKeys.EDITOR) ?: return null
    val caret = editor.caretModel.primaryCaret
    val file: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)
    return SelectionState(
      language = file?.let { ComputerLanguage.findByExtension(it.extension ?: "") },
      selectedText = if (caret.hasSelection()) editor.document.getText(TextRange(caret.selectionStart, caret.selectionEnd)) else null,
      indent = UITools.getIndent(caret).toString()
    )
  }


  interface DescribeAction_VirtualAPI {
    fun describeCode(
      code: String,
      computerLanguage: String,
      humanLanguage: String
    ): DescribeAction_ConvertedText

    class DescribeAction_ConvertedText {
      var text: String? = null
      var language: String? = null
    }
  }

  private val proxy: DescribeAction_VirtualAPI
    get() = ChatProxy(
      clazz = DescribeAction_VirtualAPI::class.java,
      api = api,
      model = AppSettingsState.instance.smartModel.chatModel(),
      temperature = AppSettingsState.instance.temperature,
      deserializerRetries = 5
    ).create()

  override fun getConfig(project: Project?): String {
    // No configuration needed for this action
    return ""
  }


  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    val state = getState(event)
    return state?.selectedText?.isNotBlank() == true
  }

  override fun processSelection(state: SelectionAction.SelectionState, config: String?, progress: ProgressIndicator): String {
    try {
      val description = proxy.describeCode(
        IndentedText.fromString(state.selectedText).textBlock.toString().trim(),
        state.language?.name ?: state.editor?.virtualFile?.extension ?: "",
        AppSettingsState.instance.humanLanguage
      ).text ?: throw IllegalStateException("Failed to generate description")
      val wrapping = com.simiacryptus.aicoder.util.StringUtil.lineWrapping(description.trim(), 120)
      val numberOfLines = wrapping.trim().split("\n").reversed().dropWhile { it.isEmpty() }.size
      val commentStyle = if (numberOfLines == 1) {
        state.language?.lineComment
      } else {
        state.language?.blockComment
      }
      return buildString {
        append(state.indent)
        append(commentStyle?.fromString(wrapping)?.withIndent(state.indent!!) ?: wrapping)
        append("\n")
        append(state.indent)
        append(state.selectedText)
      }
    } catch (e: Exception) {
      log.error("Failed to describe code", e)
      throw e
    }
  }
}