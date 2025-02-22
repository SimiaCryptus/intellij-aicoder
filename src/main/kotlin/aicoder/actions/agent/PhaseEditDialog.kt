package aicoder.actions.agent

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.simiacryptus.jopenai.models.ChatModel
import javax.swing.JComponent

class PhaseEditDialog(
    project: Project?,
    private val phase: EnhancedOutlineConfigDialog.PhaseSettings,
    private val availableModels: List<Pair<String, ChatModel>>
) : DialogWrapper(project) {
    companion object {
        private const val COLUMNS_LARGE = 50
    }
    
    
    private var model = phase.model
    private var extract = phase.extract
    private var question = phase.question

    init {
        title = "Edit Phase"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Model:") {
            comboBox(availableModels).apply {
                component.selectedItem = model
                component.addActionListener {
                    model = component.selectedItem as ChatModel
                }
            }
        }
        row("Extract:") {
            textField()
                .columns(COLUMNS_LARGE)
                .resizableColumn()
                .apply {
                    component.text = extract
                    component.document.addDocumentListener(SimpleDocumentListener {
                        extract = component.text
                    })
                }
        }
        row("Question:") {
            textField()
                .columns(COLUMNS_LARGE)
                .resizableColumn()
                .apply {
                    component.text = question
                    component.document.addDocumentListener(SimpleDocumentListener {
                        question = component.text
                    })
                }
        }
    }

    fun getPhaseSettings() = EnhancedOutlineConfigDialog.PhaseSettings(
        model = model,
        extract = extract,
        question = question
    )
}

class SimpleDocumentListener(
    private val onChange: () -> Unit
) : javax.swing.event.DocumentListener {
    override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = onChange()
    override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = onChange()
    override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = onChange()
}