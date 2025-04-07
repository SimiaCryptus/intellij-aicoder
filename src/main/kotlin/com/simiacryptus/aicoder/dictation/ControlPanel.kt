package com.simiacryptus.aicoder.dictation

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.dictation.DictationWidgetFactory.SpeechToTextWidget.Companion.toggleRecording
import com.simiacryptus.jopenai.audio.DictationManager
import com.simiacryptus.jopenai.models.AudioModels
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JSlider // Keep JSlider import if needed elsewhere, otherwise remove if unused.

class ControlPanel(
  val project: Project,
  val settings: DictationState = DictationState,
) : JPanel(), AutoCloseable {
  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(ControlPanel::class.java)
  }

  private val rmsLabel = JBLabel("RMS: ")
  private val iec61672Label = JBLabel("IEC61672: ")
  private val micLineComboBox = ComboBox<String>().apply {
    border = JBUI.Borders.emptyRight(5) // Consider adding a left border as well
    addItem("Default")
    DictationManager.availableMicLines.forEach(::addItem)
    (AppSettingsState.instance.selectedMicLine ?: settings.selectedMicLine)?.let {
      selectedItem = it
      DictationManager.selectedMicLine = it
      settings.setSelectedMicLine(it)
    }
    addActionListener({
      settings.setSelectedMicLine(selectedItem as String)
      DictationManager.selectedMicLine = selectedItem as String
      AppSettingsState.instance.selectedMicLine = selectedItem as String
    })
  }
  private val rmsProgressBar = JProgressBar(0, 100).apply {
    isStringPainted = true
    border = JBUI.Borders.emptyRight(5)
  }
  private val iec61672ProgressBar = JProgressBar(0, 100).apply {
    isStringPainted = true
    border = JBUI.Borders.emptyRight(5)
  }
  // Assuming max talk time display is around 10 seconds for the progress bar scale
  private val maxTalkTimeDisplayMs = 10000
  private val talkTimeProgressBar = JProgressBar(0, maxTalkTimeDisplayMs).apply {
    // Display format can be customized if needed, e.g., showing seconds
    // isStringPainted = true
    // string = "0.0s" // Initial value
    toolTipText = "Current consecutive talk duration"
    border = JBUI.Borders.emptyRight(5)
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
      // ... other formats ...
      "22050Hz 16-bit Mono",
      // Standard audio quality
      "32000Hz 16-bit Mono",
      "32000Hz 16-bit Stereo",
      // CD quality
      "44100Hz 16-bit Mono",
      "44100Hz 16-bit Stereo",
      "44100Hz 24-bit Mono",
      "44100Hz 24-bit Stereo",
      // Professional audio quality
      "48000Hz 16-bit Mono",
      "48000Hz 24-bit Mono",
      "48000Hz 16-bit Stereo",
      "48000Hz 24-bit Stereo",
      // High-resolution audio
      "96000Hz 24-bit Stereo",
      "192000Hz 24-bit Stereo"
    )
    formats.forEach(::addItem)
    // Set initial selection based on current settings
    val currentFormat =
      "${settings.sampleRate}Hz ${settings.sampleSize}-bit ${if (settings.channels == 1) "Mono" else "Stereo"}"
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
  private val transcriptionModelComboBox = ComboBox<AudioModels>().apply {
    border = JBUI.Borders.emptyRight(5)
    AudioModels.values().filter { it.type == AudioModels.AudioModelType.Transcription }.forEach(::addItem)
    selectedItem = settings.transcriptionModel
    setRenderer { _, value, _, _, _ -> JBLabel(value?.modelName ?: "N/A") }
    addActionListener {
      val selected = selectedItem as? AudioModels ?: return@addActionListener
      settings.setTranscriptionModel(selected)
      AppSettingsState.instance.transcriptionModel = selected.modelName
    }
  }

  private val dictationButton = JButton("Start Dictation")

  init {
    layout = GridBagLayout()
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
      gridwidth = 1
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
      gridwidth = 1
      gridx = 1
      gridy = 1
    })
    add(JBLabel("Transcription Model:"), GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = 1
      gridx = 0
      gridy = 2
    })
    add(transcriptionModelComboBox, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = 1
      gridx = 1
      gridy = 2
    })

    add(JPanel(GridBagLayout()).apply {
      add(dictationButton, GridBagConstraints().apply {
        anchor = GridBagConstraints.WEST
     insets = JBUI.insets(10)
        gridx = 0
        gridy = 0
      })
      add(JButton("Train Quiet").apply {
        addMouseListener(object : MouseAdapter() {
          override fun mousePressed(e: MouseEvent?) {
            DictationManager.discriminator.isTraining = false
            text = "Training..."
          }

          override fun mouseReleased(e: MouseEvent?) {
            DictationManager.discriminator.isTraining = null
            DictationManager.discriminator.clearMemory()
            text = "Train Quiet"
          }
        })
      }, GridBagConstraints().apply {
        anchor = GridBagConstraints.WEST
        insets = JBUI.insets(5)
        gridx = 1
        gridy = 0
      })
      add(JButton("Train Talk").apply {
        addMouseListener(object : MouseAdapter() {
          override fun mousePressed(e: MouseEvent?) {
            DictationManager.discriminator.isTraining = true
            text = "Training..."
          }

          override fun mouseReleased(e: MouseEvent?) {
            DictationManager.discriminator.isTraining = null
            DictationManager.discriminator.clearMemory()
            text = "Train Talk"
          }
        })
      }, GridBagConstraints().apply {
        anchor = GridBagConstraints.WEST
        insets = JBUI.insets(5)
        gridx = 2
        gridy = 0
      })
      add(JButton("Reset").apply {
        addActionListener {
          DictationManager.discriminator.reset()
        }
      }, GridBagConstraints().apply {
        anchor = GridBagConstraints.WEST
        insets = JBUI.insets(5)
        gridx = 3
        gridy = 0
      })
    }, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = 2
      gridx = 0
      gridy = 3
      fill = GridBagConstraints.HORIZONTAL
    })

    add(rmsLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 4
    })
    add(rmsProgressBar, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 4
      fill = GridBagConstraints.HORIZONTAL
    })

    add(iec61672Label, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 5
    })
    add(iec61672ProgressBar, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 5
      fill = GridBagConstraints.HORIZONTAL
    })

    add(talkTimeLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 6
    })
    add(talkTimeProgressBar, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 6
      fill = GridBagConstraints.HORIZONTAL
    })
    // Filler component to push everything to the top-left
    add(JPanel(), GridBagConstraints().apply {
      gridx = 0
      gridy = 7 // Next available row
      gridwidth = GridBagConstraints.REMAINDER // Span remaining columns
      weightx = 1.0
      weighty = 1.0
      fill = GridBagConstraints.BOTH
    })


    dictationButton.addActionListener {
      toggleRecording()
      if (settings.isRecording) {
        dictationButton.text = "Stop Dictation"
      } else {
        dictationButton.text = "Start Dictation"
      }
    }
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
    val rmsValue = settings.rmsLevel.coerceIn(rmsProgressBar.minimum, rmsProgressBar.maximum)
    rmsProgressBar.value = rmsValue
    rmsLabel.text = "RMS: $rmsValue%" // Update label as well
    val iecValue = settings.iec61672Level.coerceIn(iec61672ProgressBar.minimum, iec61672ProgressBar.maximum)
    iec61672ProgressBar.value = iecValue
    iec61672Label.text = "IEC61672: $iecValue%" // Update label as well
    talkTimeLabel.text = "Talk Time: ${settings.talkTime.format("%.3f")}s"
    val currentFormat =
      "${settings.sampleRate}Hz ${settings.sampleSize}-bit ${if (settings.channels == 1) "Mono" else "Stereo"}"
    formatComboBox.selectedItem = formatComboBox.items.firstOrNull { it == currentFormat } ?: formatComboBox.items[1]
    micLineComboBox.selectedItem = settings.selectedMicLine ?: "Default"
    transcriptionModelComboBox.selectedItem = settings.transcriptionModel
    val talkTimeValue = (settings.talkTime * 1000.0).toInt()
    talkTimeProgressBar.value = talkTimeValue.coerceIn(talkTimeProgressBar.minimum, talkTimeProgressBar.maximum)
  }

  override fun close() {
    settings.configuration.removeListener(::updateParams)
    settings.configuration.removeListener(::updateButtonStates)
  }
}

private fun Double.format(s: String): String {
  return String.format(s, this)
}

private val <E> ComboBox<E>.items: List<E>
  get() {
    val items = mutableListOf<E>()
    for (i in 0 until itemCount) {
      items.add(getItemAt(i))
    }
    return items
  }