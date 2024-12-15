package com.github.simiacryptus.aicoder.actions.find

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField

class FindResultsModificationDialog(
  project: Project,
  matchCount: Int
) : DialogWrapper(project) {

  private val replacementTextArea = JBTextArea().apply {
    rows = 5
    lineWrap = true
    wrapStyleWord = true
  }
  private val matchCountLabel = JBLabel("Found $matchCount matches")

  init {
    title = "Configure Find Results Modification"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val formBuilder = FormBuilder.createFormBuilder()
    formBuilder.addComponent(matchCountLabel)
    formBuilder.addLabeledComponent("Instructions:", JBScrollPane(replacementTextArea))
    return formBuilder.panel
  }

  override fun doValidate(): ValidationInfo? {
    if (replacementTextArea.text.isBlank()) {
      return ValidationInfo("Replacement text cannot be empty", replacementTextArea)
    }
    return null
  }

  data class ConfigData(
    val replacementText: String?
  )

  fun showAndGetConfig(): ConfigData? {
    if (showAndGet()) {
      return ConfigData(
        replacementText = replacementTextArea.text
      )
    }
    return null
  }

}

