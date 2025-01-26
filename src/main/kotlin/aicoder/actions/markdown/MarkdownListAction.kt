package aicoder.actions.markdown

import aicoder.actions.BaseAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.aicoder.util.UITools.getIndent
import com.simiacryptus.aicoder.util.UITools.insertString
import com.simiacryptus.aicoder.util.psi.PsiUtil.getAll
import com.simiacryptus.aicoder.util.psi.PsiUtil.getSmallestIntersecting
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil
import java.awt.Component
import javax.swing.JOptionPane

/**
 * Action that extends markdown lists by generating additional items using AI.
 * Supports bullet lists and checkbox lists.
 */
class MarkdownListAction : BaseAction() {
  private val log = Logger.getInstance(MarkdownListAction::class.java)
  private lateinit var progress: ProgressIndicator

  data class ListConfig(
    val itemCount: Int = 0,
    val temperature: Double = AppSettingsState.instance.temperature
  )

  /**
   * Gets configuration for list generation
   */
  private fun getConfig(project: Project?): ListConfig? {
    return try {
      ListConfig(
        itemCount = UITools.showInputDialog(
          project as? Component,
          "How many new items to generate?",
          "Generate List Items",
          JOptionPane.QUESTION_MESSAGE
        )?.let { Integer.parseInt(it.toString()) } ?: return null
      )
    } catch (e: Exception) {
      log.warn("Failed to get configuration", e)
      null
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  interface ListAPI {
    fun newListItems(
      items: List<String?>?,
      count: Int,
    ): Items

    data class Items(
      val items: List<String?>? = null,
    )
  }

  val proxy: ListAPI
    get() {
      val chatProxy = ChatProxy(
        clazz = ListAPI::class.java,
        api = api,
        model = AppSettingsState.instance.smartModel.chatModel(),
        temperature = AppSettingsState.instance.temperature,
        deserializerRetries = 5
      )
      chatProxy.addExample(
        returnValue = ListAPI.Items(
          items = listOf("Item 4", "Item 5", "Item 6")
        )
      ) {
        it.newListItems(
          items = listOf("Item 1", "Item 2", "Item 3"),
          count = 6
        )
      }
      return chatProxy.create()
    }

  override fun handle(e: AnActionEvent) {
    try {
      val caret = e.getData(CommonDataKeys.CARET) ?: return
      val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
      val project = e.project ?: return
      val config = getConfig(project) ?: return
      val list =
        getSmallestIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, "MarkdownListImpl") ?: return
      val items = StringUtil.trim(
        getAll(list, "MarkdownListItemImpl")
          .map {
            val all = getAll(it, "MarkdownParagraphImpl")
            if (all.isEmpty()) it.text else all[0].text
          }.toList(), 10, false
      )
      progress.fraction = 0.4
      progress.text = "Generating new items..."
      val indent = getIndent(caret)
      val endOffset = list.textRange.endOffset
      val bulletTypes = listOf("- [ ] ", "- ", "* ")
      val document = (e.getData(CommonDataKeys.EDITOR) ?: return).document
      val rawItems = items.map(CharSequence::trim).map {
        val bulletType = bulletTypes.find(it::startsWith)
        if (null != bulletType) StringUtil.stripPrefix(it, bulletType).toString()
        else it.toString()
      }

      UITools.redoableTask(e) {
        var newItems: List<String?>? = null
        progress.isIndeterminate = false
        progress.fraction = 0.2
        progress.text = "Analyzing existing items..."
        UITools.run(
          e.project, "Generating New Items", true
        ) {
          newItems = proxy.newListItems(
            rawItems,
            config.itemCount
          ).items
          progress.fraction = 0.8
          progress.text = "Formatting results..."
        }
        var newList = ""
        ApplicationManager.getApplication().runReadAction {
          val strippedList = list.text.split("\n")
            .map(String::trim).filter(String::isNotEmpty)
            .joinToString("\n")
          val bulletString = bulletTypes.find(strippedList::startsWith) ?: "1. "
          newList = newItems?.joinToString("\n") { indent.toString() + bulletString + it } ?: ""
        }
        UITools.writeableFn(e) {
          insertString(document, endOffset, "\n" + newList)
        }
      }

    } catch (ex: Exception) {
      log.error("Failed to generate list items", ex)
      UITools.showErrorDialog(
        "Failed to generate list items: ${ex.message}",
        "Error"
      )
    }
  }

  override fun isEnabled(e: AnActionEvent): Boolean {
    val caret = e.getData(CommonDataKeys.CARET) ?: return false
    val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return false
    getSmallestIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, "MarkdownListImpl") ?: return false
    return super.isEnabled(e)
  }

}
