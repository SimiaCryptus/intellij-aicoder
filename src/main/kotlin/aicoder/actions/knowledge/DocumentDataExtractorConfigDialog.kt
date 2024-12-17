package aicoder.actions.knowledge

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.simiacryptus.skyenet.apps.parse.DocumentParserApp
import com.simiacryptus.skyenet.apps.parse.ParsingModelType
import javax.swing.JComponent

class DocumentDataExtractorConfigDialog(
  project: Project?,
  var settings: DocumentParserApp.Settings,
  var modelType: ParsingModelType<*>
) : DialogWrapper(project, true) {
  companion object {
    private const val DEFAULT_DPI = 300f
    private const val DEFAULT_MAX_PAGES = 100
    private const val DEFAULT_PAGES_PER_BATCH = 10
  }

  private var dpiValue = settings.dpi.toString()
  private var maxPagesValue = settings.maxPages.toString()
  private var outputFormatValue = settings.outputFormat
  private var pagesPerBatchValue = settings.pagesPerBatch.toString()
  private var showImagesValue = settings.showImages
  private var saveImageFilesValue = settings.saveImageFiles
  private var saveTextFilesValue = settings.saveTextFiles
  private var saveFinalJsonValue = settings.saveFinalJson
  private var fastModeValue = settings.fastMode
  private var addLineNumbersValue = settings.addLineNumbers
  private var selectedModelType = modelType

  init {
    init()
    title = "Configure Document Data Extractor"
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row("Parsing Model:") {
        comboBox(ParsingModelType.values().toList())
          .bindItem({ selectedModelType }, { selectedModelType = it ?: modelType })
      }
      row("DPI:") {
        textField()
          .bindText({ dpiValue }, { dpiValue = it })
          .validationOnInput {
            validateFloatField(it.text, "DPI")
          }
      }
      row("Max Pages:") {
        textField()
          .bindText({ maxPagesValue }, { maxPagesValue = it })
          .validationOnInput {
            validateIntField(it.text, "Max pages")
          }
      }
      row("Output Format:") {
        textField()
          .bindText({ outputFormatValue }, { outputFormatValue = it })
      }
      row("Pages Per Batch:") {
        textField()
          .bindText({ pagesPerBatchValue }, { pagesPerBatchValue = it })
          .validationOnInput {
            validateIntField(it.text, "Pages per batch")
          }
      }
      row {
        checkBox("Show Images")
          .bindSelected({ showImagesValue }, { showImagesValue = it })
        checkBox("Save Image Files")
          .bindSelected({ saveImageFilesValue }, { saveImageFilesValue = it })
      }
      row {
        checkBox("Save Text Files")
          .bindSelected({ saveTextFilesValue }, { saveTextFilesValue = it })
        checkBox("Save Final JSON")
          .bindSelected({ saveFinalJsonValue }, { saveFinalJsonValue = it })
      }
      row {
        checkBox("Fast Mode")
          .bindSelected({ fastModeValue }, { fastModeValue = it })
        checkBox("Add Line Numbers")
          .bindSelected({ addLineNumbersValue }, { addLineNumbersValue = it })
      }
    }
  }

  private fun validateFloatField(text: String, fieldName: String): ValidationInfo? {
    try {
      text.toFloat().also {
        if (it <= 0) return ValidationInfo("$fieldName must be positive")
      }
    } catch (e: NumberFormatException) {
      return ValidationInfo("Invalid $fieldName value")
    }
    return null
  }

  private fun validateIntField(text: String, fieldName: String): ValidationInfo? {
    try {
      text.toInt().also {
        if (it <= 0) return ValidationInfo("$fieldName must be positive")
      }
    } catch (e: NumberFormatException) {
      return ValidationInfo("Invalid $fieldName value")
    }
    return null
  }

  override fun doValidate(): ValidationInfo? {
    return validateFloatField(dpiValue, "DPI")
      ?: validateIntField(maxPagesValue, "Max pages")
      ?: validateIntField(pagesPerBatchValue, "Pages per batch")
  }


  override fun doOKAction() {
    if (doValidate() != null) return

    settings = DocumentParserApp.Settings(
      dpi = dpiValue.toFloatOrNull() ?: DEFAULT_DPI,
      maxPages = maxPagesValue.toIntOrNull() ?: DEFAULT_MAX_PAGES,
      outputFormat = outputFormatValue,
      pagesPerBatch = pagesPerBatchValue.toIntOrNull() ?: DEFAULT_PAGES_PER_BATCH,
      showImages = showImagesValue,
      saveImageFiles = saveImageFilesValue,
      saveTextFiles = saveTextFilesValue,
      saveFinalJson = saveFinalJsonValue,
      fastMode = fastModeValue,
      addLineNumbers = addLineNumbersValue
    )
    modelType = selectedModelType
    super.doOKAction()
  }
}