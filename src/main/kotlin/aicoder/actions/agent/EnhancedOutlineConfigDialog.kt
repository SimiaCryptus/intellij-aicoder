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

class EnhancedOutlineConfigDialog(
  val project: Project?,
  var settings: EnhancedOutlineSettings
) : DialogWrapper(project, true) {
  
  private var temperature = (settings.temperature * 100).toInt()
  private var minTokens = settings.minTokensForExpansion
  private var showProjector = settings.showProjector
  private var writeFinalEssay = settings.writeFinalEssay
  private var budget = settings.budget
  private var parsingModel = settings.parsingModel
  private val phases = DefaultListModel<PhaseSettings>().apply {
    settings.phases.forEach { addElement(it) }
  }
  private var selectedIndex = -1
  private val availableModels = ChatModel.values().toList()
  
  private fun addPhase(listComponent: JBList<PhaseSettings>) {
    val dialog = ModelSelectionDialog(project, availableModels)
    if (dialog.showAndGet()) {
      dialog.selectedModel?.let { model ->
        listComponent.clearSelection()
        phases.addElement(
          PhaseSettings(
            model = model,
            extract = "Extract key points",
            question = "How can we expand this section?"
          )
        )
        selectedIndex = phases.size() - 1
        listComponent.selectedIndex = selectedIndex
      }
    }
  }
  
  private fun removePhase(listComponent: JBList<PhaseSettings>) {
    val currentIndex = listComponent.selectedIndex
    if (currentIndex >= 0) {
      phases.remove(currentIndex)
      selectedIndex = when {
        currentIndex > 0 -> currentIndex - 1
        phases.size() > 0 -> 0
        else -> -1
      }
      listComponent.selectedIndex = selectedIndex
    }
  }
  
  init {
    init()
    title = "Configure Enhanced Outline Tool"
  }
  
  override fun createCenterPanel(): JComponent = panel {
    group("Basic Settings") {
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
          .comment("Adjust the creativity level (0-100)")
      }
    }
    group("Model Configuration") {
      group("Phase Configuration") {
        val listComponent = JBList(phases).apply {
          cellRenderer = ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
            JLabel("Phase ${index + 1}: ${value?.model?.modelName ?: "Unknown Model"}").apply {
              if (isSelected) {
                background = list.selectionBackground
                foreground = list.selectionForeground
              } else {
                background = list.background
                foreground = list.foreground
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
            .comment("Phases for outline generation")
        }
        row {
          button("Add Phase") { addPhase(listComponent) }
          button("Remove Phase") { removePhase(listComponent) }
          button("Edit Phase") {
            val currentIndex = listComponent.selectedIndex
            if (currentIndex >= 0) {
              val currentPhase = phases.get(currentIndex)
              val dialog = PhaseEditDialog(project, currentPhase, availableModels)
              if (dialog.showAndGet()) {
                phases.set(currentIndex, dialog.getPhaseSettings())
                listComponent.repaint()
              }
            }
          }
        }
      }
    }
    
    group("Advanced Settings") {
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
      row("Min Tokens:") {
        intTextField().apply {
          component.text = minTokens.toString()
          component.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = update()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = update()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = update()
            private fun update() {
              minTokens = component.text.toIntOrNull() ?: minTokens
            }
          })
        }
          .focused()
          .comment("Minimum tokens needed before expanding a section")
      }
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
    fun isVisible(model: ChatModel): Boolean {
      val hasApiKey = AppSettingsState.instance.apiKeys
        ?.filter { it.value.isNotBlank() }
        ?.keys
        ?.contains(model.provider.name)
      return hasApiKey == true
    }
    if (phases.size() == 0) {
      return ValidationInfo("At least one phase is required")
    }
    if (minTokens <= 0) {
      return ValidationInfo("Minimum tokens for expansion must be greater than 0")
    }
    return null
  }
  
  override fun doOKAction() {
    settings = EnhancedOutlineSettings(
      phases = List(phases.size()) { phases.getElementAt(it) },
      temperature = temperature / 100.0,
      minTokensForExpansion = minTokens,
      showProjector = showProjector,
      writeFinalEssay = writeFinalEssay,
      budget = budget,
      parsingModel = parsingModel,
    )
    close(OK_EXIT_CODE)
  }
  
  data class EnhancedOutlineSettings(
    val temperature: Double = AppSettingsState.instance.temperature,
    val minTokensForExpansion: Int = 16,
    val showProjector: Boolean = true,
    val writeFinalEssay: Boolean = true,
    val budget: Double = 2.0,
    val parsingModel: ChatModel = AppSettingsState.instance.smartModel.chatModel(),
    val phases: List<PhaseSettings> = listOf(
      PhaseSettings(
        model = AppSettingsState.instance.smartModel.chatModel(),
        extract = "Extract the core concepts and key ideas",
        question = "What are the main topics and themes to explore?"
      ),
      PhaseSettings(
        model = AppSettingsState.instance.smartModel.chatModel(),
        extract = "Extract detailed insights and supporting points",
        question = "How can we elaborate on this concept further?"
      ),
      PhaseSettings(
        model = AppSettingsState.instance.smartModel.chatModel(),
        extract = "Extract comprehensive summary and connections",
        question = "How do all these ideas connect into a cohesive whole?"
      )
    )
  )
  
  data class PhaseSettings(
    val model: ChatModel,
    val extract: String,
    val question: String
  )
  
  companion object {
  }
}