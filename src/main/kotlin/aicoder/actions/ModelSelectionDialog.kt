package aicoder.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.simiacryptus.jopenai.models.ChatModel
import javax.swing.JComponent

class ModelSelectionDialog(
  project: Project?,
  private val availableModels: List<Pair<String, ChatModel>>,
  private val initialSelection: ChatModel? = null
) : DialogWrapper(project, true) {

  var selectedModel: ChatModel? = null

  init {
    init()
    title = "Select Model"
  }

  override fun createCenterPanel(): JComponent = panel {
    row("Model:") {
      comboBox(availableModels.map { it.second.modelName })
        .bindItem({ initialSelection?.modelName }, { selectedItem ->
          selectedModel = availableModels.find { it.second.modelName == selectedItem }?.second
        })
        .focused()
        .validationOnApply {
          if (it.selectedItem == null) error("Please select a model")
          null
        }
    }
  }
}