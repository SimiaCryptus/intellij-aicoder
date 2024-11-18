package com.github.simiacryptus.aicoder.actions.knowledge

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.panel
import com.simiacryptus.skyenet.apps.parse.DocumentParserApp
import com.simiacryptus.skyenet.apps.parse.ParsingModelType
import javax.swing.JComboBox
import javax.swing.JComponent

class DocumentDataExtractorConfigDialog(
    project: Project?,
    var settings: DocumentParserApp.Settings,
    var modelType: ParsingModelType<*>
) : DialogWrapper(project, true) {  // Make dialog modal for better UX
    companion object {
        private const val DEFAULT_DPI = 300f
        private const val DEFAULT_MAX_PAGES = 100
        private const val DEFAULT_PAGES_PER_BATCH = 10
    }

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

        return panel {
            row("Parsing Model:") { modelTypeComboBox(CCFlags.growX) }
            row("DPI:") { dpiField(CCFlags.growX) }
            row("Max Pages:") { maxPagesField(CCFlags.growX) }
            row("Output Format:") { outputFormatField(CCFlags.growX) }
            row("Pages Per Batch:") { pagesPerBatchField(CCFlags.growX) }
            row { showImagesCheckbox() }
            row { saveImageFilesCheckbox() }
            row { saveTextFilesCheckbox() }
            row { saveFinalJsonCheckbox() }
            row { fastModeCheckbox() }
            row { addLineNumbersCheckbox() }
        }
    }

    override fun doValidate(): ValidationInfo? {
        try {
            dpiField.text.toFloat().also {
                if (it <= 0) return ValidationInfo("DPI must be positive", dpiField)
            }
        } catch (e: NumberFormatException) {
            return ValidationInfo("Invalid DPI value", dpiField)
        }
        try {
            maxPagesField.text.toInt().also {
                if (it <= 0) return ValidationInfo("Max pages must be positive", maxPagesField)
            }
        } catch (e: NumberFormatException) {
            return ValidationInfo("Invalid max pages value", maxPagesField)
        }
        try {
            pagesPerBatchField.text.toInt().also {
                if (it <= 0) return ValidationInfo("Pages per batch must be positive", pagesPerBatchField)
            }
        } catch (e: NumberFormatException) {
            return ValidationInfo("Invalid pages per batch value", pagesPerBatchField)
        }
        return null
    }

    override fun doOKAction() {
        if (doValidate() != null) return

        settings = DocumentParserApp.Settings(
            dpi = dpiField.text.toFloatOrNull() ?: DEFAULT_DPI,
            maxPages = maxPagesField.text.toIntOrNull() ?: DEFAULT_MAX_PAGES,
            outputFormat = outputFormatField.text,
            pagesPerBatch = pagesPerBatchField.text.toIntOrNull() ?: DEFAULT_PAGES_PER_BATCH,
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