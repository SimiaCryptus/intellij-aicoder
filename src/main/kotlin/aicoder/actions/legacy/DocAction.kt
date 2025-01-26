package aicoder.actions.legacy

import aicoder.actions.SelectionAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.aicoder.util.IndentedText
import com.simiacryptus.aicoder.util.psi.PsiUtil
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy

/**
 * Action that generates documentation for selected code blocks.
 * Supports multiple programming languages and documentation styles.
 */

class DocAction : SelectionAction<String>() {
  companion object {
    private const val DEFAULT_DESERIALIZER_RETRIES = 5
  }

  private val log = com.intellij.openapi.diagnostic.Logger.getInstance(DocAction::class.java)
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

  interface DocAction_VirtualAPI {
    fun processCode(
      code: String,
      operation: String,
      computerLanguage: String,
      humanLanguage: String
    ): DocAction_ConvertedText

    class DocAction_ConvertedText {
      var text: String? = null
      var language: String? = null
    }
  }

  private val proxy: DocAction_VirtualAPI by lazy {
    val chatProxy = ChatProxy(
      clazz = DocAction_VirtualAPI::class.java,
      api = api,
      model = AppSettingsState.instance.smartModel.chatModel(),
      temperature = AppSettingsState.instance.temperature,
      deserializerRetries = DEFAULT_DESERIALIZER_RETRIES
    )
    chatProxy.addExample(
      DocAction_VirtualAPI.DocAction_ConvertedText().apply {
        text = """
                    /**
                     *  Prints "Hello, world!" to the console
                     */
                    """.trimIndent()
        language = "English"
      }
    ) {
      it.processCode(
        """
                fun hello() {
                    println("Hello, world!")
                }
                """.trimIndent(),
        "Write detailed KDoc prefix for code block",
        "Kotlin",
        "English"
      )
    }
    chatProxy.create()
  }

  override fun getConfig(project: Project?): String {
    return ""
  }

  override fun processSelection(state: SelectionState, config: String?, progress: ProgressIndicator): String {
    try {
      val code = state.selectedText ?: return ""
      val indentedInput = IndentedText.fromString(code)
      val docString = proxy.processCode(
        indentedInput.textBlock.toString(),
        "Write detailed " + (state.language?.docStyle ?: "documentation") + " prefix for code block",
        state.language?.name ?: "",
        AppSettingsState.instance.humanLanguage
      ).text ?: ""
      return docString + code
    } catch (e: Exception) {
      log.error("Failed to generate documentation", e)
      throw RuntimeException(
        "Failed to generate documentation: ${e.message}",
        e
      )
    }

  }

  override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
    if (computerLanguage == ComputerLanguage.Text) return false
    if (computerLanguage?.docStyle == null) return false
    if (computerLanguage.docStyle.isBlank()) return false
    return true
  }

  override fun editSelection(state: EditorState, start: Int, end: Int): Pair<Int, Int> {
    if (state.psiFile == null) return super.editSelection(state, start, end)
    val codeBlock = PsiUtil.getCodeElement(state.psiFile, start, end)
    if (codeBlock == null) return super.editSelection(state, start, end)
    val textRange = codeBlock.textRange
    return Pair(textRange.startOffset, textRange.endOffset)
  }
}