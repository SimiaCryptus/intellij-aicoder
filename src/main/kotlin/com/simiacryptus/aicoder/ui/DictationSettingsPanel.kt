package com.simiacryptus.aicoder.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.event.ChangeListener

class DictationSettingsPanel(
  val project: Project,
  val settings: DictationSettings = DictationSettings.Companion,
) : JPanel(), AutoCloseable {
  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(DictationSettingsPanel::class.java)
  }

  private val micLineComboBoxListener: (Any) -> Unit = {
    val selectedIndex = micLineComboBox.selectedIndex
    settings.setSelectedMicLine(if (selectedIndex == 0) null else micLineComboBox.getItemAt(selectedIndex))
  }
  private val updateParamsListener: () -> Unit = { updateParams() }
  private val rmsSliderListener: ChangeListener = ChangeListener { settings.setRmsLevel(rmsSlider.value) }
  private val iec61672SliderListener: ChangeListener = ChangeListener { settings.setIec61672Level(iec61672Slider.value) }
  private val rmsThresholdSliderListener: ChangeListener = ChangeListener { settings.setRmsThreshold(rmsThresholdSlider.value) }
  private val iec61672ThresholdSliderListener: ChangeListener =
    ChangeListener { settings.setIec61672Threshold(iec61672ThresholdSlider.value) }
  private val sampleRateComboBoxListener: (Any) -> Unit = { settings.setSampleRate(sampleRateComboBox.selectedItem as Int) }
  private val sampleSizeComboBoxListener: (Any) -> Unit = { settings.setSampleSize(sampleSizeComboBox.selectedItem as Int) }
  private val channelsComboBoxListener: (Any) -> Unit = { settings.setChannels(channelsComboBox.selectedItem as Int) }
  private val rmsLabel = JBLabel("RMS: 0%")
  private val iec61672Label = JBLabel("IEC61672: 0%")
  private val rmsSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false // Consider enabling the slider if needed
    majorTickSpacing = 20
    minorTickSpacing = 10
    border = JBUI.Borders.emptyRight(5)
    addChangeListener(rmsSliderListener)
  }
  private val iec61672Slider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false
    majorTickSpacing = 20
    minorTickSpacing = 5
    border = JBUI.Borders.emptyRight(5)
    addChangeListener(iec61672SliderListener)
  }
  private val rmsThresholdSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 50).apply {
    paintTicks = true
    paintLabels = true
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    addChangeListener(rmsThresholdSliderListener)
  }
  private val iec61672ThresholdSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 25).apply {
    paintTicks = true
    paintLabels = true
    border = JBUI.Borders.emptyRight(5)
    addChangeListener(iec61672ThresholdSliderListener)
  }
  private val sampleRateComboBox = ComboBox(arrayOf(8000, 16000, 22050, 44100, 48000)).apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    addActionListener(sampleRateComboBoxListener)
  }
  private val sampleSizeComboBox = ComboBox(arrayOf(8, 16, 24, 32)).apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    addActionListener(sampleSizeComboBoxListener)
  }
  private val channelsComboBox = ComboBox(arrayOf(1, 2)).apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    addActionListener(channelsComboBoxListener)
  }
  private val micLineComboBox = ComboBox<String>().apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    addActionListener(micLineComboBoxListener)
  }
  private val dictationButton = JButton("Start Dictation")

  init {
    micLineComboBox.addItem("Default")
    SpeechRecognitionManager.availableMicLines.forEach { micLineComboBox.addItem(it) }
    micLineComboBox.selectedIndex = 0
    layout = GridBagLayout().apply {
      columnWidths = intArrayOf(100, 200, 100, 200)
    }
    add(JBLabel("Microphone Line:"), GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = 1
      gridx = 0
      gridy = 0
    })
    add(micLineComboBox, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = 3
      gridx = 1
      gridy = 0
    })
    add(JBLabel("Sample Rate:"), GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = 1
      gridx = 0
      gridy = 1
    })
    add(sampleRateComboBox, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = GridBagConstraints.REMAINDER
      gridx = 1
      gridy = 1
    })
    add(dictationButton, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = 2
      gridx = 2
      gridy = 1
    })
    add(JBLabel("Sample Size:"), GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 2
    })
    add(sampleSizeComboBox, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 2
      fill = GridBagConstraints.HORIZONTAL
    })
    add(JBLabel("Channels:"), GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 3
    })
    add(channelsComboBox, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 3
      fill = GridBagConstraints.HORIZONTAL
    })
    add(JBLabel("RMS Percentile Threshold:"), GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 4
    })
    add(rmsThresholdSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 4
      fill = GridBagConstraints.HORIZONTAL
    })
    add(rmsLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 2
      gridy = 4
    })
    add(rmsSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 3
      gridy = 4
      fill = GridBagConstraints.HORIZONTAL
    })
    add(JBLabel("IEC61672 Percentile Threshold:"), GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 5
    })
    add(iec61672ThresholdSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 5
      fill = GridBagConstraints.HORIZONTAL
    })
    add(iec61672Label, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 2
      gridy = 5
    })
    add(iec61672Slider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 3
      gridy = 5
      fill = GridBagConstraints.HORIZONTAL
    })
    dictationButton.addActionListener { toggleDictation() }
    updateButtonStates()
    revalidate()
    updateParams()
    settings.addListener(updateParamsListener)
    settings.addListener(::updateButtonStates)
  }

  private fun updateButtonStates() {
    dictationButton.text = if (settings.isRecording) "Stop Dictation" else "Start Dictation"
  }

  private fun updateParams() {
    rmsLabel.text = "RMS: ${settings.rmsLevel}%"
    iec61672Label.text = "IEC61672: ${settings.iec61672Level}%"
    rmsSlider.value = settings.rmsLevel
    iec61672Slider.value = settings.iec61672Level
    rmsThresholdSlider.value = settings.rmsThreshold
    iec61672ThresholdSlider.value = settings.iec61672Threshold
    sampleRateComboBox.selectedItem = settings.sampleRate
    sampleSizeComboBox.selectedItem = settings.sampleSize
    channelsComboBox.selectedItem = settings.channels
    micLineComboBox.selectedItem = settings.selectedMicLine ?: "Default"
  }

  private fun toggleDictation() = if (settings.isRecording) {
    settings.setRecordingState(false)
    dictationButton.text = "Start Dictation"
    SpeechToTextWidget.statusBar?.updateWidget(SpeechToTextWidget.ID)
    Thread { SpeechRecognitionManager.stopRecording() }.start()
  } else {
    settings.setRecordingState(true)
    dictationButton.text = "Stop Dictation"
    Thread {
      SpeechRecognitionManager.startRecording(
        onTranscriptionUpdate = {
          log.info("Transcription: $it")
          WriteCommandAction.runWriteCommandAction(project) {
            val currentEditor = project.currentEditor() ?: return@runWriteCommandAction
            currentEditor.document.insertString(currentEditor.caretModel.offset, it)
          }
        }
      )
    }.start()
    SpeechToTextWidget.statusBar?.updateWidget(SpeechToTextWidget.ID)
  }


  override fun close() {
    settings.removeListener(updateParamsListener)
    settings.removeListener(::updateButtonStates)
  }
}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor