package com.github.simiacryptus.aicoder.actions.knowledge

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.simiacryptus.skyenet.apps.parse.DocumentParserApp
import com.simiacryptus.skyenet.apps.parse.ParsingModelType
import javax.swing.*

class DocumentDataExtractorConfigDialog(
    project: Project?,
    var settings: DocumentParserApp.Settings,
    var modelType: ParsingModelType<*>
) : DialogWrapper(project) {

    private val dpiField = JBTextField(settings.dpi.toString())
    private val maxPagesField = JBTextField(settings.maxPages.toString())
    private val outputFormatField = JBTextField(settings.outputFormat)
    private val pagesPerBatchField = JBTextField(settings.pagesPerBatch.toString())
    private val showImagesCheckbox = JBCheckBox("Show Images", settings.showImages)
    private val saveImageFilesCheckbox = JBCheckBox("Save Image Files", settings.saveImageFiles)
    private val saveTextFilesCheckbox = JBCheckBox("Save Text Files", settings.saveTextFiles)
    private val saveFinalJsonCheckbox = JBCheckBox("Save Final JSON", settings.saveFinalJson)
    private val fastModeCheckbox = JBCheckBox("Fast Mode", settings.fastMode)
    private val addLineNumbersCheckbox = JBCheckBox("Add Line Numbers", settings.addLineNumbers)
    private val modelTypeComboBox = JComboBox(ParsingModelType.values().toTypedArray()).apply {
        selectedItem = modelType
    }

    init {
        init()
        title = "Configure Document Data Extractor"
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(createLabeledField("Parsing Model:", modelTypeComboBox))
        panel.add(createLabeledField("DPI:", dpiField))
        panel.add(createLabeledField("Max Pages:", maxPagesField))
        panel.add(createLabeledField("Output Format:", outputFormatField))
        panel.add(createLabeledField("Pages Per Batch:", pagesPerBatchField))
        panel.add(showImagesCheckbox)
        panel.add(saveImageFilesCheckbox)
        panel.add(saveTextFilesCheckbox)
        panel.add(saveFinalJsonCheckbox)
        panel.add(fastModeCheckbox)
        panel.add(addLineNumbersCheckbox)

        return panel
    }

    private fun createLabeledField(label: String, field: JComponent): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(JLabel(label))
        panel.add(Box.createHorizontalStrut(10))
        panel.add(field)
        return panel
    }

    override fun doOKAction() {
        settings = DocumentParserApp.Settings(
            dpi = dpiField.text.toFloatOrNull() ?: settings.dpi,
            maxPages = maxPagesField.text.toIntOrNull() ?: settings.maxPages,
            outputFormat = outputFormatField.text,
            pagesPerBatch = pagesPerBatchField.text.toIntOrNull() ?: settings.pagesPerBatch,
            showImages = showImagesCheckbox.isSelected,
            saveImageFiles = saveImageFilesCheckbox.isSelected,
            saveTextFiles = saveTextFilesCheckbox.isSelected,
            saveFinalJson = saveFinalJsonCheckbox.isSelected,
            fastMode = fastModeCheckbox.isSelected,
            addLineNumbers = addLineNumbersCheckbox.isSelected
        )
        modelType = modelTypeComboBox.selectedItem as ParsingModelType<*>
        super.doOKAction()
    }
}