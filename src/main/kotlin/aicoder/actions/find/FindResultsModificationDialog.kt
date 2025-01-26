package aicoder.actions.find

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class FindResultsModificationDialog(
  project: Project,
  matchCount: Int
) : DialogWrapper(project) {

  private var replacementText = "Please modify this code to: "
  private var autoApply = false

  init {
    title = "AI-Based Find Results Modification"
    setOKButtonText("Modify Code")
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row("Modification Instructions:") {
        textArea()
          .bindText({ replacementText }, { replacementText = it })
          .rows(5)
          .align(Align.FILL)
          .comment("Enter instructions for how you want the code to be modified")
          .focused()
          .apply {
            component.lineWrap = true
            component.wrapStyleWord = true
            component.selectAll()
          }
      }.resizableRow()
      row {
        checkBox("Auto-apply changes")
          .bindSelected({ autoApply }, { autoApply = it })
          .comment("Automatically apply changes without manual confirmation")
      }
    }
  }

  override fun doValidate(): ValidationInfo? {
    if (replacementText.isBlank()) {
      return ValidationInfo("Please enter instructions for code modification")
    }
    if (replacementText.length < 10) {
      return ValidationInfo("Please provide more detailed instructions")
    }
    return null
  }

  data class ConfigData(
    val replacementText: String?,
    val autoApply: Boolean
  )

  fun showAndGetConfig(): ConfigData? {
    if (showAndGet()) {
      return ConfigData(
        replacementText = replacementText,
        autoApply = autoApply
      )
    }
    return null
  }

}