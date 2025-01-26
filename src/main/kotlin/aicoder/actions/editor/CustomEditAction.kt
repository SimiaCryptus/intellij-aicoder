package aicoder.actions.editor

import aicoder.actions.SelectionAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import javax.swing.JOptionPane

/**
 * Action that allows custom editing of code selections using AI.
 * Supports multiple languages and provides custom edit instructions.
 */
open class CustomEditAction : SelectionAction<String>(requiresSelection = true) {
  private val log = Logger.getInstance(CustomEditAction::class.java)
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  interface VirtualAPI {
    fun editCode(
      code: String,
      operation: String,
      computerLanguage: String,
      humanLanguage: String
    ): EditedText

    data class EditedText(
      var code: String? = null,
      var language: String? = null
    )
  }

  val proxy: VirtualAPI
    get() {
      val chatProxy = ChatProxy(
        clazz = VirtualAPI::class.java,
        api = api,
        model = AppSettingsState.instance.smartModel.chatModel(),
        temperature = AppSettingsState.instance.temperature,
      )
      chatProxy.addExample(
        VirtualAPI.EditedText(
          """
                // Print Hello, World! to the console
                println("Hello, World!")
                """.trimIndent(),
          "java"
        )
      ) {
        it.editCode(
          """println("Hello, World!")""",
          "Add code comments",
          "java",
          "English"
        )
      }
      return chatProxy.create()
    }

  override fun getConfig(project: Project?): String? {
    return UITools.showInputDialog(
      null,
      "Enter edit instruction:",
      "Edit Code",
      JOptionPane.QUESTION_MESSAGE
    ) as String?
  }

  override fun processSelection(state: SelectionState, config: String?, progress: ProgressIndicator): String {
    if (config.isNullOrBlank()) return state.selectedText ?: ""
    return try {
      progress.isIndeterminate = true
      progress.text = "Applying edit: $config"
      val settings = AppSettingsState.instance
      val outputHumanLanguage = settings.humanLanguage
      settings.getRecentCommands("customEdits").addInstructionToHistory(config)
      val result = proxy.editCode(
        state.selectedText ?: "",
        config,
        state.language?.name ?: state.editor?.virtualFile?.extension ?: "unknown",
        outputHumanLanguage
      )
      result.code ?: state.selectedText ?: ""
    } catch (e: Exception) {
      log.error("Failed to process edit", e)
      UITools.showErrorDialog(
        "Failed to process edit: ${e.message}",
        "Edit Error"
      )
      state.selectedText ?: ""
    }
  }
}