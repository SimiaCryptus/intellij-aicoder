package com.simiacryptus.aicoder.dictation

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

class SettingsPanel(
  val project: Project,
  val settings: DictationSettings = DictationSettings,
) : JPanel(), AutoCloseable {
  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(SettingsPanel::class.java)
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
      columnWidths = intArrayOf(150, 250, 150, 250)
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
    add(dictationButton, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridwidth = 2
      gridx = 0
      gridy = 2
    })

    add(rmsLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 3
    })
    add(rmsSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 3
      fill = GridBagConstraints.HORIZONTAL
    })

    add(iec61672Label, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 4
    })
    add(iec61672Slider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 4
      fill = GridBagConstraints.HORIZONTAL
    })

    add(talkTimeLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 5
    })
    add(talkTimeSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 5
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
    updateTalkTimeLabel()
    val currentFormat = "${settings.sampleRate}Hz ${settings.sampleSize}-bit ${if(settings.channels == 1) "Mono" else "Stereo"}"
    formatComboBox.selectedItem = formatComboBox.items.firstOrNull { it == currentFormat } ?: formatComboBox.items[1]
    micLineComboBox.selectedItem = settings.selectedMicLine ?: "Default"
    talkTimeLabel.text = "Talk Time: ${settings.talkTime.format("%.3f")}s"
    talkTimeSlider.value = (settings.talkTime * 1000.0).toInt()
  }

  private fun updateTalkTimeLabel() {
    talkTimeLabel.text = "Talk Time: ${settings.talkTime.format("%.3f")}s"
  }

  private fun toggleDictation() = if (settings.isRecording) {
    settings.setRecordingState(false)
    dictationButton.text = "Start Dictation"
    SpeechToTextWidget.statusBar?.updateWidget(SpeechToTextWidget.ID)
    Thread(DictationManager.Companion::stopRecording).start()
  } else {
    settings.setRecordingState(true)
    dictationButton.text = "Stop Dictation"
    Thread {
      DictationManager.startRecording(
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

private val <E> ComboBox<E>.items: List<E>
  get() {
    val items = mutableListOf<E>()
    for (i in 0 until itemCount) {
      items.add(getItemAt(i))
    }
    return items
  }