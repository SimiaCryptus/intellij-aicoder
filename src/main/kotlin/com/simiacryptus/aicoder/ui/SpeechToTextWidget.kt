package com.simiacryptus.aicoder.ui

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import com.simiacryptus.jopenai.audio.LookbackLoudnessWindowBuffer
import com.simiacryptus.jopenai.audio.PercentileLoudnessWindowBuffer
import icons.MyIcons
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.awt.event.MouseEvent
import javax.sound.sampled.AudioFormat

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
  private var _icon = MyIcons.micInactive
  private var statusBar: StatusBar? = null

  private val settingsDialog = DictationSettingsDialog()

  override fun ID(): String = "AICodingAssistant.SpeechToTextWidget"

  override fun install(statusBar: StatusBar) {
    this.statusBar = statusBar
  }

  override fun getPresentation() = this

  override fun getTooltipText(): String = when {
    SpeechRecognitionManager.isRecording -> "Click to stop recording"
    else -> "Click to start recording"
  } + "<br/>(Shift-click to open settings)"

  override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
    when {
      it.isShiftDown -> settingsDialog.showDialog()
      else -> Thread { toggleRecording() }.start()
    }
  }

  override fun getIcon() = _icon

  private fun toggleRecording() {
    if (SpeechRecognitionManager.isRecording) {
      SpeechRecognitionManager.stopRecording()
    } else {
      SpeechRecognitionManager.audioFormat = AudioFormat(
        /* sampleRate = */ (settingsDialog.sampleRateComboBox.selectedItem as Int).toFloat(),
        /* sampleSizeInBits = */ settingsDialog.sampleSizeComboBox.selectedItem as Int,
        /* channels = */ settingsDialog.channelsComboBox.selectedItem as Int,
        /* signed = */ true,
        /* bigEndian = */ false
      )
      var iec61672Max = 5000.0
      var rmsMax = 2.0
      SpeechRecognitionManager.loudnessStrategy = when (settingsDialog.loudnessStrategyComboBox.selectedItem as String) {
        "Lookback" -> LookbackLoudnessWindowBuffer(
          inputBuffer = SpeechRecognitionManager.audioBuffer,
          outputBuffer = SpeechRecognitionManager.processedBuffer,
          continueFn = { SpeechRecognitionManager.isRecording },
          audioFormat = SpeechRecognitionManager.audioFormat,

          onRmsUpdate = { settingsDialog.rmsSlider.value = ((it.rms / rmsMax) * 100).toInt() },
          onIec61672Update = { settingsDialog.iec61672Slider.value = ((it.iec61672 / iec61672Max) * 100).toInt() }
        )

        "Percentile" -> PercentileLoudnessWindowBuffer(
          inputBuffer = SpeechRecognitionManager.audioBuffer,
          outputBuffer = SpeechRecognitionManager.processedBuffer,
          continueFn = { SpeechRecognitionManager.isRecording },
          audioFormat = SpeechRecognitionManager.audioFormat
        )

        else -> null
      }
      SpeechRecognitionManager.startRecording(
        onRmsUpdate = {
          rmsMax = it.rms.coerceAtLeast(rmsMax)
          settingsDialog.rmsSlider.value = ((it.rms / rmsMax) * 100).toInt()
          settingsDialog.rmsLabel.text = "RMS: ${it.rms}"
        },
        onIec61672Update = {
          iec61672Max = it.iec61672.coerceAtLeast(iec61672Max)
          settingsDialog.iec61672Slider.value = ((it.iec61672 / iec61672Max) * 100).toInt()
          settingsDialog.iec61672Label.text = "IEC61672: ${it.iec61672}%"
        },
        onSpectralEntropyUpdate = {
          settingsDialog.spectralEntropyLabel.text = "Spectral Entropy: ${it.spectralEntropy}"
        },
        onTranscriptionUpdate = {
          log.info("Transcription: $it")
          project.currentEditor()?.document?.insertString(project.currentEditor()?.caretModel?.offset ?: 0, it)
        },
        onRecordingStateChanged = { isRecording ->
          _icon = if (isRecording) MyIcons.micActive else MyIcons.micInactive
          statusBar?.updateWidget(ID())
        },
        onRecordingStarted = { recordingStartTime ->
          settingsDialog.rmsLabel.text = "RMS: 0%"
          rmsMax = 2.0
          settingsDialog.iec61672Label.text = "IEC61672: 0%"
          iec61672Max = 5000.0
          settingsDialog.spectralEntropyLabel.text = "Spectral Entropy: 0.0"
        },
        onRecordingDurationUpdate = { recordingDuration ->
          val duration = recordingDuration - SpeechRecognitionManager.recordingStartTime
          settingsDialog.spectralEntropyLabel.text = "Duration: ${duration / 1000}s"
        },
        onRecordingStopped = {
          _icon = MyIcons.micInactive
          statusBar?.updateWidget(ID())
        },
        onException = {
          log.error("Error during recording", it)
        }
      )
    }
  }


  companion object {
    private val log = LoggerFactory.getLogger(SpeechToTextWidget::class.java)
  }
}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor