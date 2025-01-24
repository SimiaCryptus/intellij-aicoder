package com.simiacryptus.aicoder.ui

import com.intellij.openapi.ui.ComboBox
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridLayout
import javax.sound.sampled.AudioFormat
import javax.swing.*

class DictationSettingsDialog : JDialog() {
  val rmsLabel = JLabel("RMS: 0%")
  val iec61672Label = JLabel("IEC61672: 0%")
  val spectralEntropyLabel = JLabel("Spectral Entropy: 0.0")
  val rmsSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false
    majorTickSpacing = 20
    minorTickSpacing = 5
  }
  val iec61672Slider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false
    majorTickSpacing = 20
    minorTickSpacing = 5
  }

  private val secondsPerPacketSlider = JSlider(JSlider.HORIZONTAL, 0, 10, 5).apply {
    this.paintLabels = true
  }
  val loudnessStrategyComboBox = ComboBox(arrayOf("Lookback", "Percentile"))
  private val loudnessStrategyLabel = JLabel("Loudness Strategy:")
  private val minimumOutputTimeSlider = JSlider(JSlider.HORIZONTAL, 1, 30, 5).apply {
    this.paintLabels = true
  }
  private val rmsPercentileThresholdSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 50).apply {
    this.paintLabels = true
  }
  private val iec61672PercentileThresholdSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 25).apply {
    this.paintLabels = true
  }
  private val quietWindowMaxSlider = JSlider(JSlider.HORIZONTAL, 1, 10, 3).apply {
    this.paintLabels = true
  }
  private val quietThresholdSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 25).apply {
    this.paintLabels = true
  }
  private val flushSecondsSlider = JSlider(JSlider.HORIZONTAL, 10, 120, 60).apply {
    this.paintLabels = true
  }
  private val minSecondsSlider = JSlider(JSlider.HORIZONTAL, 1, 10, 1).apply {
    this.paintLabels = true
  }
  val sampleRateComboBox = ComboBox(arrayOf(8000, 16000, 22050, 44100, 48000))
  val sampleSizeComboBox = ComboBox(arrayOf(8, 16, 24, 32))
  val channelsComboBox = ComboBox(arrayOf(1, 2))
  private val micLineComboBox = ComboBox<String>()
  private val okButton = JButton("OK")
  private val cancelButton = JButton("Cancel")
  private var contentPane = JPanel()
  var selectedMicLine: String?
    get() = SpeechRecognitionManager.selectedMicLine
    set(value) {
      SpeechRecognitionManager.selectedMicLine = value
    }


  init {
    title = "Speech-to-Text Settings"
    defaultCloseOperation = HIDE_ON_CLOSE
    contentPane = JPanel().apply {
      layout = BorderLayout()
      add(JPanel().apply({
        layout = GridLayout(0, 2, 10, 10)

        add(JLabel("Microphone Line:"))
        add(micLineComboBox)

        add(JLabel("Sample Rate:"))
        add(sampleRateComboBox)
        add(JLabel("Sample Size:"))
        add(sampleSizeComboBox)
        add(JLabel("Channels:"))
        add(channelsComboBox)

        add(loudnessStrategyLabel)
        add(loudnessStrategyComboBox as Component)

        add(JLabel("RMS Percentile Threshold:"))
        add(rmsPercentileThresholdSlider)

        add(rmsLabel)
        add(rmsSlider)

        add(JLabel("IEC61672 Percentile Threshold:"))
        add(iec61672PercentileThresholdSlider)

        add(iec61672Label)
        add(iec61672Slider)

        add(JLabel("Seconds per Packet:"))
        add(secondsPerPacketSlider)

        add(JLabel("Minimum Output Time (s):"))
        add(minimumOutputTimeSlider)

        add(JLabel("Quiet Window Max:"))
        add(quietWindowMaxSlider)

        add(JLabel("Quiet Threshold:"))
        add(quietThresholdSlider)

        add(JLabel("Flush Seconds:"))
        add(flushSecondsSlider)

        add(JLabel("Min Seconds:"))
        add(minSecondsSlider)


      }), BorderLayout.CENTER)
      add(JPanel().apply(fun JPanel.() {
        add(okButton)
        add(cancelButton)
      }), BorderLayout.SOUTH)
    }
    setContentPane(contentPane)
    pack()
    micLineComboBox.addItem("Default")
    SpeechRecognitionManager.availableMicLines.forEach { micLineComboBox.addItem(it) }
    micLineComboBox.selectedIndex = 0

    okButton.addActionListener { dispose() }
    cancelButton.addActionListener { dispose() }
    setLocationRelativeTo(null)
    loudnessStrategyComboBox.addActionListener {
      loudnessStrategyLabel.text = "Loudness Strategy: ${loudnessStrategyComboBox.selectedItem}"
    }
    micLineComboBox.addActionListener {
      selectedMicLine = if (micLineComboBox.selectedIndex == 0) {
        null
      } else {
        micLineComboBox.selectedItem as String
      }
    }
    okButton.addActionListener {
      SpeechRecognitionManager.audioFormat = AudioFormat(
        (sampleRateComboBox.selectedItem as Int).toFloat(),
        sampleSizeComboBox.selectedItem as Int,
        channelsComboBox.selectedItem as Int,
        true, false
      )
      dispose()
    }
  }

  fun showDialog() {
    isVisible = true
  }
}