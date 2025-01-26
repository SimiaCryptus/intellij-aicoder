package aicoder.actions.markdown

import aicoder.actions.SelectionAction
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import org.slf4j.LoggerFactory

class MarkdownImplementActionGroup : ActionGroup() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  private val markdownLanguages = listOf(
    "sql", "java", "asp", "c", "clojure", "coffee", "cpp", "csharp", "css", "bash", "go", "java", "javascript",
    "less", "make", "matlab", "objectivec", "pascal", "PHP", "Perl", "python", "rust", "scss", "sql", "svg",
    "swift", "ruby", "smalltalk", "vhdl"
  )

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isEnabled(e)
    super.update(e)
  }

  companion object {
    private val log = LoggerFactory.getLogger(MarkdownImplementActionGroup::class.java)
    fun isEnabled(e: AnActionEvent): Boolean {
      return try {
        val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
        ComputerLanguage.Markdown == computerLanguage && UITools.hasSelection(e)
      } catch (ex: Exception) {
        log.error("Error checking action enablement", ex)
        false
      }
    }
  }

  override fun getChildren(e: AnActionEvent?): Array<AnAction> {
    if (e == null) return emptyArray()
    val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return emptyArray()
    val actions = markdownLanguages.map { language -> MarkdownImplementAction(language) }
    return actions.toTypedArray()
  }

  open class MarkdownImplementAction(private val language: String) : SelectionAction<String>(true) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    init {
      templatePresentation.text = language
      templatePresentation.description = language
    }

    interface ConversionAPI {
      fun implement(text: String, humanLanguage: String, computerLanguage: String): ConvertedText

      class ConvertedText {
        var code: String? = null
        var language: String? = null
      }
    }

    private fun getProxy(): ConversionAPI {
      return ChatProxy(
        clazz = ConversionAPI::class.java,
        api = api,
        model = AppSettingsState.instance.smartModel.chatModel(),
        temperature = AppSettingsState.instance.temperature,
        deserializerRetries = 5
      ).create()
    }

    override fun getConfig(project: Project?): String {
      return ""
    }

    override fun processSelection(state: SelectionState, config: String?, progress: ProgressIndicator): String {
      return try {
        progress.text = "Generating $language code..."
        progress.isIndeterminate = true
        val code = getProxy().implement(state.selectedText ?: "", "autodetect", language).code
          ?: throw IllegalStateException("No code generated")
        "\n\n```$language\n${code.trim()}\n```\n"
      } catch (e: Exception) {
        log.error("Error processing selection", e)
        UITools.showErrorDialog("Failed to convert code: ${e.message}", "Conversion Error")
        state.selectedText ?: ""
      }
    }
  }
}