package com.simiacryptus.aicoder.ui

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

  private val rmsLabel = JBLabel("RMS: 0%")
  private val iec61672Label = JBLabel("IEC61672: 0%")
  private val spectralEntropyLabel = JBLabel("Spectral Entropy: 0%")
  private val lookbackLabel = JBLabel("Lookback Time: 2s")
  private val memoryLabel = JBLabel("Memory Time: 60s")
  private val minRmsLabel = JBLabel("RMS Threshold: 0%")
  private val minIec61672Label = JBLabel("IEC61672 Threshold: 0%")
  private val minSpectralEntropyLabel = JBLabel("Spectral Entropy Threshold: 0%")
  private val minTalkTimeLabel = JBLabel("Minimum Talk Time: 1.0s")
  private val enableSpectralEntropy = false

  private val spectralEntropySlider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false
    majorTickSpacing = 20
    minorTickSpacing = 5
    border = JBUI.Borders.emptyRight(5)
    addChangeListener { spectralEntropyLabel.text = "Spectral Entropy: ${value}%" }
    addChangeListener {
      settings.setSpectralEntropyLevel(value)
    }
  }
  private val micLineComboBox = ComboBox<String>().apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    addItem("Default")
    SpeechRecognitionManager.availableMicLines.forEach(::addItem)
    (AppSettingsState.instance.selectedMicLine ?: settings.selectedMicLine)?.let {
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
  private val talkTimeLabel = JBLabel()
  private val formatComboBox = ComboBox<String>().apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    // Add common audio format combinations
    val formats = listOf(
      // Standard telephone quality
      "8000Hz 8-bit Mono",
      "8000Hz 16-bit Mono",
      // Speech recognition optimized
      "16000Hz 16-bit Mono",
      "22050Hz 16-bit Mono",
      // Standard audio quality
      "32000Hz 16-bit Mono",
      "32000Hz 16-bit Stereo",
      // CD quality
      "44100Hz 16-bit Stereo",
      "44100Hz 24-bit Stereo",
      // Professional audio quality
      "48000Hz 16-bit Mono",
      "48000Hz 16-bit Stereo",
      "48000Hz 24-bit Stereo",
      // High-resolution audio
      "96000Hz 24-bit Stereo",
      "192000Hz 24-bit Stereo"
    )
    formats.forEach(::addItem)
    // Set initial selection based on current settings
    val currentFormat = "${settings.sampleRate}Hz ${settings.sampleSize}-bit ${if(settings.channels == 1) "Mono" else "Stereo"}"
    selectedItem = formats.find { it == currentFormat } ?: formats[1]
    
    addActionListener({
      val format = (selectedItem as String).split(" ")
      val sampleRate = format[0].replace("Hz", "").toInt()
      val sampleSize = format[1].replace("-bit", "").toInt()
      val channels = if (format[2] == "Mono") 1 else 2
      settings.setSampleRate(sampleRate)
      settings.setSampleSize(sampleSize)
      settings.setChannels(channels)
      AppSettingsState.instance.sampleSize = sampleSize
      AppSettingsState.instance.channels = channels
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
    add(JBLabel("Audio Format:"), GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = 1
      gridx = 0
      gridy = 1
    })
    add(formatComboBox, GridBagConstraints().apply {
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

    if(enableSpectralEntropy) {
      add(this.minSpectralEntropyLabel, GridBagConstraints().apply {
        anchor = GridBagConstraints.WEST
        insets = JBUI.insets(10)
        gridx = 0
        gridy = 6
      })
      add(JSlider(JSlider.HORIZONTAL, 0, 100, (settings.minSpectralEntropy * 100).toInt()).apply {
        paintTicks = true
        paintLabels = true
        majorTickSpacing = 20
        minorTickSpacing = 5
        border = JBUI.Borders.emptyRight(5)
        value = (AppSettingsState.instance.minSpectralEntropy * 100).toInt()
        addChangeListener {
          settings.setMinSpectralEntropy(value.toDouble() / 100)
          AppSettingsState.instance.minSpectralEntropy = value.toDouble() / 100
          this@DictationSettingsPanel.minSpectralEntropyLabel.text =
            "Spectral Entropy Threshold: ${value}%"
        }
      }, GridBagConstraints().apply {
        anchor = GridBagConstraints.WEST
        insets = JBUI.insets(10)
        gridx = 1
        gridy = 6
        fill = GridBagConstraints.HORIZONTAL
      })
      add(spectralEntropyLabel, GridBagConstraints().apply {
        anchor = GridBagConstraints.WEST
        insets = JBUI.insets(10)
        gridx = 2
        gridy = 6
      })
      add(spectralEntropySlider, GridBagConstraints().apply {
        anchor = GridBagConstraints.WEST
        insets = JBUI.insets(10)
        gridx = 3
        gridy = 6
        fill = GridBagConstraints.HORIZONTAL
      })
    }

    add(minTalkTimeLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 7
    })
    add(JSlider(JSlider.HORIZONTAL, 0, 10000, (SpeechRecognitionManager.loudnessStrategy?.minimumTalkSeconds?.let { it * 1000 } ?: 1000.0).toInt()).apply {
      paintTicks = true
      paintLabels = true
      majorTickSpacing = 2500
      minorTickSpacing = 100
      border = JBUI.Borders.emptyRight(5)
      value = (AppSettingsState.instance.minimumTalkSeconds * 1000).toInt()
      addChangeListener {
        val d = value.toDouble() / 1000.0
        SpeechRecognitionManager.loudnessStrategy?.minimumTalkSeconds = d
        AppSettingsState.instance.minimumTalkSeconds = d
        minTalkTimeLabel.text = "Minimum Talk Time: ${"%.3f".format(d)}s"
      }
    }, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 7
      fill = GridBagConstraints.HORIZONTAL
    })
    add(talkTimeLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 2
      gridy = 7
    })
    add(talkTimeSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 3
      gridy = 7
      fill = GridBagConstraints.HORIZONTAL
    })

    add(lookbackLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 8
    })
    add(
      JSlider(
        JSlider.HORIZONTAL,
        1,
        30,
        (SpeechRecognitionManager.loudnessStrategy?.lookbackPackets?.toDouble()?.div(settings.packetDuration)
          ?: 2.0).toInt()
      ).apply {
        paintTicks = true
        paintLabels = true
        majorTickSpacing = 5
        minorTickSpacing = 1
        border = JBUI.Borders.emptyRight(5)
        addChangeListener {
          val d = value.toDouble()
          val packets = ((d * 1000.0) / settings.packetDuration).toInt()
          SpeechRecognitionManager.loudnessStrategy?.lookbackPackets = packets
          lookbackLabel.text = "Lookback Time: ${d}s"
          settings.setLookbackSeconds(d)
        }
      }, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 8
      fill = GridBagConstraints.HORIZONTAL
    })
    add(memoryLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 2
      gridy = 8
    })
    add(JSlider(
      JSlider.HORIZONTAL,
      1,
      120,
      (SpeechRecognitionManager.loudnessStrategy?.memoryPackets?.toDouble()?.div(settings.packetDuration) ?: 10.0).toInt()
    ).apply {
      paintTicks = true
      paintLabels = true
      majorTickSpacing = 20
      minorTickSpacing = 5
      border = JBUI.Borders.emptyRight(5)
      addChangeListener {
        val d = value.toDouble()
        val packets = ((d * 1000.0) / settings.packetDuration).toInt()
        SpeechRecognitionManager.loudnessStrategy?.memoryPackets = packets
        memoryLabel.text = "Memory Time: ${d}s"
        settings.setMemorySeconds(d)
      }
    }, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 3
      gridy = 8
      fill = GridBagConstraints.HORIZONTAL
    })

    dictationButton.addActionListener { toggleDictation() }
    updateButtonStates()
    revalidate()
    updateParams()
    settings.configuration.addListener(::updateParams)
    settings.configuration.addListener(::updateButtonStates)
  }

  private fun updateButtonStates() {
    dictationButton.text = if (settings.isRecording) "Stop Dictation" else "Start Dictation"
  }

  private fun updateParams() {
    rmsSlider.value = settings.rmsLevel
    iec61672Slider.value = settings.iec61672Level
    spectralEntropySlider.value = settings.spectralEntropyLevel
    minTalkTimeLabel.text = "Minimum Talk Time: ${SpeechRecognitionManager.loudnessStrategy?.minimumTalkSeconds?.format("%.1f")}s"
    updateTalkTimeLabel()
    val currentFormat = "${settings.sampleRate}Hz ${settings.sampleSize}-bit ${if(settings.channels == 1) "Mono" else "Stereo"}"
    formatComboBox.selectedItem = formatComboBox.items.firstOrNull { it == currentFormat } ?: formatComboBox.items[1]
    micLineComboBox.selectedItem = settings.selectedMicLine ?: "Default"
    talkTimeLabel.text = "Talk Time: ${settings.talkTime.format("%.3f")}s"
    talkTimeSlider.value = (settings.talkTime * 1000.0).toInt()
    this.minRmsLabel.text = "RMS Threshold: ${(settings.minRMS * 100).toInt()}%"
    this.minIec61672Label.text = "IEC61672 Threshold: ${(settings.minIEC61672 * 100).toInt()}%"
    this.minSpectralEntropyLabel.text = "Spectral Entropy Threshold: ${(settings.minSpectralEntropy * 100).toInt()}%"

  }

  private fun updateTalkTimeLabel() {
    talkTimeLabel.text = "Talk Time: ${settings.talkTime.format("%.3f")}s"
  }

  private fun toggleDictation() = if (settings.isRecording) {
    settings.setRecordingState(false)
    dictationButton.text = "Start Dictation"
    SpeechToTextWidget.statusBar?.updateWidget(SpeechToTextWidget.ID)
    Thread(SpeechRecognitionManager.Companion::stopRecording).start()
  } else {
    settings.setRecordingState(true)
    dictationButton.text = "Stop Dictation"
    Thread {
      SpeechRecognitionManager.startRecording(
      )
    }.start()
    SpeechToTextWidget.statusBar?.updateWidget(SpeechToTextWidget.ID)
  }


  override fun close() {
    settings.configuration.removeListener(::updateParams)
    settings.configuration.removeListener(::updateButtonStates)
  }
}

private fun Double.format(s: String): String {
  return String.format(s, this)
}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor


private val <E> ComboBox<E>.items: List<E>
  get() {
    val items = mutableListOf<E>()
    for (i in 0 until itemCount) {
      items.add(getItemAt(i))
    }
    return items
  }