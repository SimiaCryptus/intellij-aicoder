package com.github.simiacryptus.aicoder.actions.generic

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.simiacryptus.jopenai.models.ChatModel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class ModelSelectionDialog(
    project: Project?,
    private val availableModels: List<ChatModel>,
    private val initialSelection: ChatModel? = null
) : DialogWrapper(project, true) {

    var selectedModel: ChatModel? = null
    private val modelComboBox = ComboBox(
        availableModels.map { it.modelName }.toTypedArray()
    ).apply {
        selectedItem = initialSelection?.modelName
    }

    init {
        init()
        title = "Select Model"
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Model:") {
            cell(modelComboBox)
        }
    }

    override fun doOKAction() {
        selectedModel = availableModels.find { it.modelName == modelComboBox.selectedItem }
        super.doOKAction()
    }
}