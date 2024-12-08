package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
 import com.intellij.ui.dsl.builder.bindValue
 import com.intellij.ui.dsl.builder.bindSelected
 import com.intellij.ui.dsl.gridLayout.HorizontalAlign
 import com.intellij.ui.components.JBScrollPane
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.chatModel
import javax.swing.*

class OutlineConfigDialog(
    val project: Project?,
    var settings: OutlineSettings
) : DialogWrapper(project, true) {

    private var temperature = (settings.temperature * 100).toInt()
    private val expansionSteps = DefaultListModel<ExpansionStep>().apply {
        settings.expansionSteps.forEach { addElement(it) }
    }
    private var selectedIndex = -1
    private val availableModels = ChatModel.values()
        .map { it.value }
        .filter { isVisible(it) }
        .toList()

    init {
        init()
        title = "Configure Outline Tool"
    }

    override fun createCenterPanel(): JComponent = panel {
        group("Outline Generation Steps") {
            row {
            val listComponent = JBList(expansionSteps).apply {
                cellRenderer = ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
                    JLabel(value?.model?.modelName ?: "Unknown Model").apply {
                        if (isSelected) {
                            background = list.selectionBackground
                            foreground = list.selectionForeground
                        }
                    }
                }
                addListSelectionListener { e ->
                    if (!e.valueIsAdjusting) {
                        selectedIndex = selectedIndex
                    }
                }
            }
            cell(listComponent)
                .align(Align.FILL)
                .comment("List of models to use in sequence for outline generation. At least one model is required.")
            }
            row {
                this@group.buttonsGroup {
                    this@row.button("Add Step") {
                        val dialog = ModelSelectionDialog(project, availableModels)
                        if (dialog.showAndGet()) {
                            dialog.selectedModel?.let { model ->
                                expansionSteps.addElement(ExpansionStep(model))
                                selectedIndex = expansionSteps.size() - 1
                            }
                        }
                    }
                    this@row.button("Remove Step") {
                        if (selectedIndex >= 0) {
                            val newIndex = when {
                              selectedIndex > 0 -> selectedIndex - 1
                              expansionSteps.size() > 1 -> 0
                              else -> -1
                            }
                            expansionSteps.remove(selectedIndex)
                            selectedIndex = newIndex
                        }
                    }
                    this@row.button("Edit Step") {
                        if (selectedIndex >= 0) {
                            val currentStep = expansionSteps.get(selectedIndex)
                            val dialog = ModelSelectionDialog(project, availableModels, currentStep.model)
                            if (dialog.showAndGet()) {
                                dialog.selectedModel?.let { model ->
                                    expansionSteps.set(selectedIndex, ExpansionStep(model))
                                }
                            }
                        }
                    }
                }
            }
        }
        row("Global Temperature:") {
            slider(
                min = 0,
                max = 100,
                minorTickSpacing = 1,
                majorTickSpacing = 10,
            )
                .bindValue({ temperature }, { temperature = it })
                .align(Align.FILL)
                .comment("Adjust the temperature value (0-100)")
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (expansionSteps.size() == 0) {
            return ValidationInfo("At least one expansion step is required")
        }
        return null
    }

    override fun doOKAction() {
        settings = OutlineSettings(
            expansionSteps = List(expansionSteps.size()) { expansionSteps.getElementAt(it) },
            temperature = temperature / 100.0,
        )
        super.doOKAction()
    }

    companion object {
        fun isVisible(model: ChatModel): Boolean {
            val hasApiKey = AppSettingsState.instance.apiKey
                ?.filter { it.value.isNotBlank() }
                ?.keys
                ?.contains(model.provider.name)
            return false != hasApiKey
        }
    }
}
data class ExpansionStep(
    val model: ChatModel,
)


data class OutlineSettings(
    val expansionSteps: List<ExpansionStep> = listOf(
        ExpansionStep(AppSettingsState.instance.smartModel.chatModel()),
        ExpansionStep(AppSettingsState.instance.smartModel.chatModel())
    ),
    val temperature: Double = AppSettingsState.instance.temperature,
)