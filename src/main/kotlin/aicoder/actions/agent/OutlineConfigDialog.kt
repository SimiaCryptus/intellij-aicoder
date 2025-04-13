package aicoder.actions.agent

import aicoder.actions.ModelSelectionDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.chatModel
import javax.swing.*

class OutlineConfigDialog(
  val project: Project?,
  var settings: OutlineSettings
) : DialogWrapper(project, true) {

  private var temperature = (settings.temperature * 100).toInt()
  private var minTokens = settings.minTokensForExpansion
  private var showProjector = settings.showProjector
  private var writeFinalEssay = settings.writeFinalEssay
  private var budget = settings.budget
  private var parsingModel = settings.parsingModel
  private val expansionSteps = DefaultListModel<ExpansionStep>().apply {
    settings.expansionSteps.forEach { addElement(it) }
  }
  private var selectedIndex = -1
  private val availableModels = ChatModel.values().filter { isVisible(it.value) }
    .toList()

  init {
    init()
    title = "Configure Outline Tool"
  }

  override fun createCenterPanel(): JComponent = panel {
    group("Outline Generation Steps") {
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
            selectedIndex = this@apply.selectedIndex
          }
        }
      }
      row {
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
                listComponent.clearSelection()
                expansionSteps.addElement(ExpansionStep(model))
                selectedIndex = expansionSteps.size() - 1
                listComponent.selectedIndex = selectedIndex
              }
            }
          }
          this@row.button("Remove Step") {
            val currentIndex = listComponent.selectedIndex
            if (currentIndex >= 0) {
              expansionSteps.remove(currentIndex)
              selectedIndex = when {
                currentIndex > 0 -> currentIndex - 1
                expansionSteps.size() > 0 -> 0
                else -> -1
              }
              listComponent.selectedIndex = selectedIndex
            }
          }
          this@row.button("Edit Step") {
            val currentIndex = listComponent.selectedIndex
            if (currentIndex >= 0) {
              val currentStep = expansionSteps.get(currentIndex)
              val dialog = ModelSelectionDialog(project, availableModels, currentStep.model)
              if (dialog.showAndGet()) {
                dialog.selectedModel?.let { model ->
                  expansionSteps.set(currentIndex, ExpansionStep(model))
                  listComponent.repaint()
                }
              }
            }
          }
        }
      }
    }
    group("Model Settings") {
      row("Parsing Model:") {
        comboBox(availableModels).apply {
            component.selectedItem = parsingModel
            component.addActionListener {
              parsingModel = component.selectedItem as ChatModel
            }
          }
          .align(Align.FILL)
          .comment("Model used for parsing outline structure")
      }
      row("Min Tokens for Expansion:") {
        intTextField().apply {
            component.text = minTokens.toString()
            component.addActionListener {
              minTokens = component.text.toIntOrNull() ?: minTokens
            }
          }
          .focused()
          .comment("Minimum number of tokens required for section expansion")
      }
    }
    row("Global Temperature:") {
      slider(
        min = 0,
        max = 100,
        minorTickSpacing = 1,
        majorTickSpacing = 10,
      )
        .apply {
          component.value = temperature
          component.addChangeListener {
            temperature = component.value
          }
        }
        .focused()
        .comment("Adjust the temperature value (0-100)")
    }
    group("Output Settings") {
      row {
        checkBox("Show Projector")
          .bindSelected({ showProjector }, { showProjector = it })
          .comment("Enable visualization of concept relationships")
      }
      row {
        checkBox("Write Final Essay")
          .bindSelected({ writeFinalEssay }, { writeFinalEssay = it })
          .comment("Generate a final essay from the outline")
      }
    }
    group("Resource Settings") {
      row("Budget:") {
        cell(JSpinner(SpinnerNumberModel(budget, 0.1, 10.0, 0.1)))
          .apply {
            component.addChangeListener {
              budget = component.value as Double
            }
          }
          .align(Align.FILL)
          .comment("Maximum budget in dollars")
      }
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
      minTokensForExpansion = minTokens,
      showProjector = showProjector,
      writeFinalEssay = writeFinalEssay,
      budget = budget,
      parsingModel = parsingModel
    )
    super.doOKAction()
  }

  companion object {
    fun isVisible(model: ChatModel): Boolean {
      val hasApiKey = AppSettingsState.instance.apiKeys
        ?.filter { it.value.isNotBlank() }
        ?.keys
        ?.contains(model.provider.name)
      return false != hasApiKey
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
    val minTokensForExpansion: Int = 16,
    val showProjector: Boolean = true,
    val writeFinalEssay: Boolean = true,
    val budget: Double = 2.0,
    val parsingModel: ChatModel = AppSettingsState.instance.smartModel.chatModel()
  )
}