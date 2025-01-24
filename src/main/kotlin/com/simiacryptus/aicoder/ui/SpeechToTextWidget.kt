package com.simiacryptus.aicoder.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.audio.AudioPacket
import com.simiacryptus.jopenai.audio.AudioRecorder
import com.simiacryptus.jopenai.audio.LookbackLoudnessWindowBuffer
import com.simiacryptus.jopenai.audio.TranscriptionProcessor
import icons.MyIcons
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridLayout
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import javax.sound.sampled.AudioSystem
import javax.swing.*

class SpeechToTextWidgetFactory : StatusBarWidgetFactory {
  override fun getId(): String = "AICodingAssistant.SpeechToTextWidget"
  override fun getDisplayName(): String = "AI Speech-to-Text"
  override fun isAvailable(project: Project) = true
  override fun createWidget(project: Project) = SpeechToTextWidget(project)
  override fun createWidget(project: Project, scope: CoroutineScope) = SpeechToTextWidget(project)
  override fun canBeEnabledOn(statusBar: StatusBar) = true
}

class SpeechToTextWidget(private val project: Project) : StatusBarWidget,
  StatusBarWidget.IconPresentation {
  private val recentPacketBuffer = ConcurrentLinkedDeque<AudioPacket>()
  private val rmsLabel = JLabel("RMS: 0%")
  private val iec61672Label = JLabel("IEC61672: 0%")
  private val spectralEntropyLabel = JLabel("Spectral Entropy: 0.0")
  private val audioBuffer = LinkedList<ByteArray>()
  private val processedBuffer = LinkedList<ByteArray>()
  private var _icon = MyIcons.micInactive
  private var isRecording = false
  private var recorder: Thread? = null
  private var processor: Thread? = null
  private var windowBuffer: Thread? = null
  private var monitoringThread: Thread? = null
  private var lastPacket: AudioPacket? = null
  private var statusBar: StatusBar? = null
  val rmsPercentileThresholdLabel = JLabel("")
  val iec61672PercentileThresholdLabel = JLabel("")
  val spectralEntropyThresholdLabel = JLabel("")

  private val rmsSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false
    majorTickSpacing = 20
    minorTickSpacing = 5
    addChangeListener {
      rmsLabel.text = "RMS: ${value}%"
    }
  }
  private val iec61672Slider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
    paintTicks = true
    paintLabels = true
    isEnabled = false
    majorTickSpacing = 20
    minorTickSpacing = 5
    addChangeListener {
      iec61672Label.text = "IEC61672: ${value}%"
    }
  }

  private val settingsDialog = SettingsDialog()

  override fun ID(): String = "AICodingAssistant.SpeechToTextWidget"

  override fun install(statusBar: StatusBar) {
    this.statusBar = statusBar
  }

  override fun dispose() {
    stopRecording()
    monitoringThread?.interrupt()
    recentPacketBuffer.clear()
  }

  override fun getPresentation() = this

  override fun getTooltipText(): String = when {
    isRecording -> "Click to stop recording"
    else -> "Click to start recording"
  } + "<br/>(Shift-click to open settings)"

  override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
    when {
      it.isShiftDown -> settingsDialog.showDialog()
      else -> toggleRecording()
    }
  }

  override fun getIcon() = _icon

  private fun toggleRecording() {
    if (isRecording) {
      stopRecording()
    } else {
      startRecording()
      _icon = MyIcons.micActive
    }
    statusBar?.updateWidget(ID())
  }

  private fun startRecording() {
    try {
      isRecording = true
      audioBuffer.clear()
      processedBuffer.clear()
      recentPacketBuffer.clear()
      val selectedMicLine = settingsDialog.selectedMicLine


      // Start audio recorder
      recorder = Thread {
        AudioRecorder(audioBuffer, 0.5, { isRecording }, selectedMicLine).run()
      }.apply { start() }

      // Start window buffer
      windowBuffer = Thread {
        LookbackLoudnessWindowBuffer(
          audioBuffer,
          processedBuffer,
          {
            val v = (it.rms * 100).toInt()
            rmsSlider.value = v
            rmsSlider.updateUI()
          },
          {
            val v = (it.iec61672 * 100).toInt()
            iec61672Slider.value = v
            iec61672Slider.updateUI()
          },
        ) { isRecording }.run()
      }.apply { start() }

      // Start transcription processor
      processor = Thread {
        TranscriptionProcessor(
          OpenAIClient(),
          processedBuffer,
          { isRecording },
          onText = { text ->
            // Insert text at current editor caret position
            project.currentEditor()?.document?.insertString(
              project.currentEditor()?.caretModel?.offset ?: 0,
              text
            )
          }
        ).run()
      }.apply { start() }
      // Start monitoring thread
      monitoringThread = Thread {
        while (isRecording) {
          val packet = processedBuffer.poll()
          if (packet != null) {
            val audioPacket = AudioPacket(AudioPacket.convertRaw(packet))
            recentPacketBuffer.add(audioPacket)
            recentPacketBuffer.removeIf { it.duration > 5.0 }
            spectralEntropyLabel.text = "Spectral Entropy: ${audioPacket.spectralEntropy}"
            updateThresholdIndicators(audioPacket)
          }
          Thread.sleep(100)
        }
      }.apply { start() }
    } catch (e: Exception) {
      Messages.showErrorDialog(
        project,
        "Failed to start recording: ${e.message}",
        "Speech-to-Text Error"
      )
      stopRecording()
    }
  }

  private fun updateThresholdIndicators(currentPacket: AudioPacket) {
    rmsPercentileThresholdLabel.text = "RMS Percentile: ${percentile(currentPacket.rms, recentPacketBuffer.map { it.rms }.toDoubleArray().sortedArray())}"
    iec61672PercentileThresholdLabel.text =
      "IEC61672 Percentile: ${percentile(currentPacket.iec61672, recentPacketBuffer.map { it.iec61672 }.toDoubleArray().sortedArray())}"
    spectralEntropyThresholdLabel.text =
      "Spectral Entropy Percentile: ${percentile(currentPacket.spectralEntropy, recentPacketBuffer.map { it.spectralEntropy }.toDoubleArray().sortedArray())}"
  }

  private fun percentile(value: Double, values: DoubleArray): Double {
    var index = values.binarySearch(value)
    if (index < 0) index = -index - 1
    return index.toDouble() / values.size
  }

  private fun stopRecording() {
    isRecording = false
    recorder?.join()
    windowBuffer?.join()
    processor?.join()
    recorder = null
    windowBuffer = null
    processor = null
    _icon = MyIcons.micInactive
    rmsSlider.value = 0
    iec61672Slider.value = 0
    spectralEntropyLabel.text = "Spectral Entropy: 0.0"
    rmsPercentileThresholdLabel.text = "RMS Percentile: -"
    iec61672PercentileThresholdLabel.text = "IEC61672 Percentile: -"
    spectralEntropyThresholdLabel.text = "Spectral Entropy Percentile: -"
    rmsPercentileThresholdLabel.text = "RMS Percentile: -"
    iec61672PercentileThresholdLabel.text = "IEC61672 Percentile: -"
    spectralEntropyThresholdLabel.text = "Spectral Entropy Percentile: -"
  }

  inner class SettingsDialog : JDialog() {
    private val secondsPerPacketSlider = JSlider(JSlider.HORIZONTAL, 0, 10, 5).apply {
      this.paintLabels = true
    }
    private val loudnessStrategyComboBox = JComboBox(arrayOf("Lookback", "Percentile"))
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
    private val micLineComboBox = JComboBox<String>()
    private val okButton = JButton("OK")
    private val cancelButton = JButton("Cancel")
    private var contentPane = JPanel()
    var selectedMicLine: String? = null


    init {
      title = "Speech-to-Text Settings"
      defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
      contentPane = JPanel().apply {
        layout = BorderLayout()
        add(JPanel().apply({
          layout = GridLayout(0, 2, 10, 10)

          add(JLabel("Microphone Line:"))
          add(micLineComboBox)

          add(loudnessStrategyLabel)
          add(loudnessStrategyComboBox as Component)

          add(rmsPercentileThresholdLabel)
          add(rmsPercentileThresholdSlider)

          add(JLabel("IEC61672 Percentile Threshold:"))
          add(iec61672PercentileThresholdSlider)

          add(JLabel("Current IEC61672:"))
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
      val mixerInfo = AudioSystem.getMixerInfo()
      mixerInfo.forEach { mixer ->
        mixer.toString().let { micLineComboBox.addItem(it) }
      }
      micLineComboBox.selectedIndex = 0

      okButton.addActionListener { dispose() }
      cancelButton.addActionListener { dispose() }
      setLocationRelativeTo(null)
      loudnessStrategyComboBox.addActionListener {
        loudnessStrategyLabel.text = "Loudness Strategy: ${loudnessStrategyComboBox.selectedItem}"
        updateThresholdIndicators()
      }
      rmsPercentileThresholdSlider.addChangeListener { updateThresholdIndicators() }
      iec61672PercentileThresholdSlider.addChangeListener { updateThresholdIndicators() }
      micLineComboBox.addActionListener {
        selectedMicLine = if (micLineComboBox.selectedIndex == 0) {
          null
        } else {
          micLineComboBox.selectedItem as String
        }
      }
    }

    private fun updateThresholdIndicators() {
      val lastPacket = lastPacket ?: return
      rmsPercentileThresholdLabel.text = "RMS Percentile: ${percentile(lastPacket.rms, recentPacketBuffer.mapNotNull { it.rms }.toDoubleArray().sortedArray())}"
      iec61672PercentileThresholdLabel.text =
        "IEC61672 Percentile: ${percentile(lastPacket.iec61672, recentPacketBuffer.mapNotNull { it.iec61672 }.toDoubleArray().sortedArray())}"
      spectralEntropyThresholdLabel.text =
        "Spectral Entropy Percentile: ${
          percentile(
            lastPacket.spectralEntropy,
            recentPacketBuffer.mapNotNull { it.spectralEntropy }.toDoubleArray().sortedArray()
          )
        }"
    }

    fun showDialog() {
      isVisible = true
    }
  }
}

// Extension function to get current editor
private fun Project.currentEditor() = com.intellij.openapi.fileEditor.FileEditorManager
  .getInstance(this)
  .selectedTextEditor