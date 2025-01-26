package com.simiacryptus.aicoder.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.simiacryptus.aicoder.config.AppSettingsState
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSlider

class DictationSettingsPanel(
  val project: Project,
  val settings: DictationSettings = DictationSettings.Companion,
) : JPanel(), AutoCloseable {
  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(DictationSettingsPanel::class.java)
  }

  private val rmsLabel = JBLabel()
  private val iec61672Label = JBLabel()
  private val lookbackLabel = JBLabel("Lookback Time: 2s")
  private val memoryLabel = JBLabel("Memory Time: 60s")
  private val minRmsLabel = JBLabel("RMS Threshold: 0%")
  private val minIec61672Label = JBLabel("IEC61672 Threshold: 0%")
  private val minTalkTimeLabel = JBLabel("Minimum Talk Time: 1.0s")
  private val micLineComboBox = ComboBox<String>().apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    addItem("Default")
    SpeechRecognitionManager.availableMicLines.forEach { addItem(it) }
    AppSettingsState.instance.selectedMicLine?.let {
      selectedItem = it
      SpeechRecognitionManager.selectedMicLine = it
      settings.setSelectedMicLine(it)
    }
    addActionListener({
      settings.setSelectedMicLine(selectedItem as String)
      SpeechRecognitionManager.selectedMicLine = selectedItem as String
      AppSettingsState.instance.selectedMicLine = selectedItem as String
    })
  }
  private val rmsSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false // Consider enabling the slider if needed
    majorTickSpacing = 20
    minorTickSpacing = 10
    border = JBUI.Borders.emptyRight(5)
    addChangeListener { rmsLabel.text = "RMS: ${value}%" }
    addChangeListener {
      settings.setRmsLevel(value)
    }
  }
  private val iec61672Slider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false
    majorTickSpacing = 20
    minorTickSpacing = 5
    border = JBUI.Borders.emptyRight(5)
    addChangeListener { iec61672Label.text = "IEC61672: ${value}%" }
    addChangeListener {
      settings.setIec61672Level(value)
    }
  }
  private val talkTimeSlider = JSlider(JSlider.HORIZONTAL, 0, 10000, (settings.talkTime * 1000).toInt()).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false
    majorTickSpacing = 2500
    minorTickSpacing = 100
    border = JBUI.Borders.emptyRight(5)
    addChangeListener {
      settings.setTalkTime(value.toDouble() / 1000.0)
      updateTalkTimeLabel()
    }
  }
  private val lookbackSecondsSlider = JSlider(
    JSlider.HORIZONTAL,
    1,
    30,
    (SpeechRecognitionManager.loudnessStrategy?.lookbackPackets?.toDouble()?.div(settings.packetDuration) ?: 2.0).toInt()
  ).apply {
    paintTicks = true
    paintLabels = true
    majorTickSpacing = 5
    minorTickSpacing = 1
    border = JBUI.Borders.emptyRight(5)
    addChangeListener {
      SpeechRecognitionManager.loudnessStrategy?.lookbackPackets = (value * settings.packetDuration).toInt()
      lookbackLabel.text = "Lookback Time: ${value}s"
    }
  }
  private val talkTimeLabel = JBLabel()
  private val sampleRateComboBox = ComboBox(arrayOf(8000, 16000, 22050, 44100, 48000)).apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    addActionListener({
      settings.setSampleRate(selectedItem as Int)
    })
  }
  private val sampleSizeComboBox = ComboBox(arrayOf(8, 16, 24, 32)).apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    AppSettingsState.instance.sampleSize.let {
      selectedItem = it
    }
    addActionListener({
      settings.setSampleSize(selectedItem as Int)
      AppSettingsState.instance.sampleSize = selectedItem as Int
    })
  }
  private val channelsComboBox = ComboBox(arrayOf(1, 2)).apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    AppSettingsState.instance.channels.let {
      selectedItem = it
    }
    addActionListener({
      settings.setChannels(selectedItem as Int)
      AppSettingsState.instance.channels = selectedItem as Int
    })
  }
  private val dictationButton = JButton("Start Dictation")

  init {
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

    add(this.minRmsLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 4
    })
    add(JSlider(JSlider.HORIZONTAL, 0, 100, (settings.minRMS * 100).toInt()).apply {
      paintTicks = true
      paintLabels = true
      majorTickSpacing = 20
      minorTickSpacing = 5
      border = JBUI.Borders.emptyRight(5)
      value = (AppSettingsState.instance.minRMS * 100).toInt()
      addChangeListener {
        settings.setMinRMS(value.toDouble() / 100)
        AppSettingsState.instance.minRMS = value.toDouble() / 100
        this@DictationSettingsPanel.minRmsLabel.text = "RMS Threshold: ${value}%"
      }
    }, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 4
      fill = GridBagConstraints.HORIZONTAL
    })
    add(this.rmsLabel, GridBagConstraints().apply {
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

    add(this.minIec61672Label, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 5
    })
    add(JSlider(JSlider.HORIZONTAL, 0, 100, (settings.minIEC61672 * 100).toInt()).apply {
      paintTicks = true
      paintLabels = true
      majorTickSpacing = 20
      minorTickSpacing = 5
      border = JBUI.Borders.emptyRight(5)
      value = (AppSettingsState.instance.minIEC61672 * 100).toInt()
      addChangeListener {
        settings.setMinIEC61672(value.toDouble() / 100)
        AppSettingsState.instance.minIEC61672 = value.toDouble() / 100
      }
    }, GridBagConstraints().apply {
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
    add(minTalkTimeLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 6
    })
    add(JSlider(JSlider.HORIZONTAL, 0, 10000, (SpeechRecognitionManager.loudnessStrategy?.minimumTalkSeconds?.let { it * 1000 } ?: 1000.0).toInt()).apply {
      paintTicks = true
      paintLabels = true
      majorTickSpacing = 2500
      minorTickSpacing = 100
      border = JBUI.Borders.emptyRight(5)
      value = (AppSettingsState.instance.minimumTalkSeconds * 1000).toInt()
      addChangeListener {
        SpeechRecognitionManager.loudnessStrategy?.minimumTalkSeconds = value.toDouble() / 1000.0
        AppSettingsState.instance.minimumTalkSeconds = value.toDouble() / 1000.0
        minTalkTimeLabel.text = "Minimum Talk Time: ${"%.3f".format(value.toDouble() / 1000.0)}s"
      }
    }, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 6
      fill = GridBagConstraints.HORIZONTAL
    })
    add(talkTimeLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 2
      gridy = 6
    })
    add(talkTimeSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 3
      gridy = 6
      fill = GridBagConstraints.HORIZONTAL
    })
    add(lookbackLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 7
    })
    add(lookbackSecondsSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 7
      fill = GridBagConstraints.HORIZONTAL
    })
    add(memoryLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 2
      gridy = 7
    })
    add(JSlider(
      JSlider.HORIZONTAL,
      1,
      120,
      (SpeechRecognitionManager.loudnessStrategy?.memoryPackets?.toDouble()?.div(settings.packetDuration) ?: 60.0).toInt()
    ).apply {
      paintTicks = true
      paintLabels = true
      majorTickSpacing = 20
      minorTickSpacing = 5
      border = JBUI.Borders.emptyRight(5)
      addChangeListener {
        SpeechRecognitionManager.loudnessStrategy?.memoryPackets = ((value * 1.0) / settings.packetDuration).toInt()
        memoryLabel.text = "Memory Time: ${value}s"
      }
    }, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 3
      gridy = 7
      fill = GridBagConstraints.HORIZONTAL
    })

    dictationButton.addActionListener { toggleDictation() }
    updateButtonStates()
    revalidate()
    updateParams()
    settings.addListener({ updateParams() })
    settings.addListener(::updateButtonStates)
  }

  private fun updateButtonStates() {
    dictationButton.text = if (settings.isRecording) "Stop Dictation" else "Start Dictation"
  }

  private fun updateParams() {
    rmsSlider.value = settings.rmsLevel
    iec61672Slider.value = settings.iec61672Level
    minTalkTimeLabel.text = "Minimum Talk Time: ${SpeechRecognitionManager.loudnessStrategy?.minimumTalkSeconds?.format("%.1f")}s"
    updateTalkTimeLabel()
    sampleRateComboBox.selectedItem = settings.sampleRate
    sampleSizeComboBox.selectedItem = settings.sampleSize
    channelsComboBox.selectedItem = settings.channels
    micLineComboBox.selectedItem = settings.selectedMicLine ?: "Default"
    talkTimeLabel.text = "Talk Time: ${settings.talkTime.format("%.3f")}s"
    talkTimeSlider.value = (settings.talkTime * 1000.0).toInt()
    lookbackSecondsSlider.value = (SpeechRecognitionManager.loudnessStrategy?.lookbackPackets?.toDouble()?.div(settings.packetDuration) ?: 2.0).toInt()
    lookbackLabel.text = "Lookback Time: ${lookbackSecondsSlider.value}s"
    this.minRmsLabel.text = "RMS Threshold: ${(settings.minRMS * 100).toInt()}%"
    this.minIec61672Label.text = "IEC61672 Threshold: ${(settings.minIEC61672 * 100).toInt()}%"

  }

  private fun updateTalkTimeLabel() {
    talkTimeLabel.text = "Talk Time: ${settings.talkTime.format("%.3f")}s"
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
    settings.removeListener({ updateParams() })
    settings.removeListener(::updateButtonStates)
  }
}

private fun Double.format(s: String): String {
  return String.format(s, this)
}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor