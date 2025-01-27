package com.simiacryptus.aicoder.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.ui.DictationSettings.Companion.packetDuration
import com.simiacryptus.aicoder.ui.DictationSettingsPanel.Companion
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.audio.AudioPacket
import com.simiacryptus.jopenai.audio.AudioRecorder
import com.simiacryptus.jopenai.audio.LoudnessWindowBuffer
import com.simiacryptus.jopenai.audio.TranscriptionProcessor
import com.simiacryptus.jopenai.audio.TranscriptionProcessor.TranscriptionResult
import org.slf4j.LoggerFactory
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.swing.JOptionPane
import kotlin.collections.ArrayDeque

open class SpeechRecognitionManager : Disposable {
  companion object : SpeechRecognitionManager() {
    private val log = LoggerFactory.getLogger(SpeechRecognitionManager::class.java)
  }

  val availableMicLines: List<String>
    get() {
      return AudioSystem.getMixerInfo().filter {
        val mixer = AudioSystem.getMixer(it)
        !mixer.targetLineInfo.isNullOrEmpty()
      }.map { it.toString() }.toList()
    }
  var onPacket: (AudioPacket) -> Unit = {
    rmsMax = it.rms.coerceAtLeast(rmsMax)
    iec61672Max = it.iec61672.coerceAtLeast(iec61672Max)
    spectralEntropyMax = it.spectralEntropy.coerceAtLeast(spectralEntropyMax)
    DictationSettings.setIec61672Level(((it.iec61672 / iec61672Max) * 100).toInt())
    DictationSettings.setRmsLevel(((it.rms / rmsMax) * 100).toInt())
    DictationSettings.setSpectralEntropyLevel(((it.spectralEntropy / spectralEntropyMax) * 100).toInt())
  }
  var selectedMicLine: String? = null
  var transcriptionProcessor: TranscriptionProcessor? = null

  var audioFormat: AudioFormat = AudioFormat(16000f, 16, 1, true, false)
    set(value) {
      field = value
      loudnessStrategy = null
    }
  private var iec61672Max = 0.0
  private var rmsMax = 0.0
  private var spectralEntropyMax = 0.0
  private var isRecording = false
  private var recordingStartTime: Long = 0
  var loudnessStrategy: LoudnessWindowBuffer? = null
    private set
  private val audioBuffer: Queue<AudioPacket> = LinkedList()
  private val processedBuffer: Queue<AudioPacket> = LinkedList()
  private var recorder: Thread? = null
  private var processor: Thread? = null
  private var windowBuffer: Thread? = null
  private var monitoringThread: Thread? = null
  private var project: Project? = null

  var recentTranscriptionResult: TranscriptionResult? = null
    private set
  val transctiption = EventDispatcher()
  private val onTranscriptionUpdate: (TranscriptionResult) -> Unit = {
    log.info("Transcription: $it")
    recentTranscriptionResult = it
    transctiption.notifyListeners()
    WriteCommandAction.runWriteCommandAction(project) {
      project?.currentEditor()?.apply {
        document.insertString(caretModel.offset, it.text)
      }
    }
  }
  var onException: (java.lang.Exception) -> Unit = { log.error("Error during recording", it) }

  @Suppress("LongParameterList")
  fun startRecording() {
    rmsMax = 0.0
    iec61672Max = 0.0
    audioFormat = AudioFormat(
      /* sampleRate = */ DictationSettings.sampleRate.toFloat(),
      /* sampleSizeInBits = */ DictationSettings.sampleSize,
      /* channels = */ DictationSettings.channels,
      /* signed = */ true,
      /* bigEndian = */ false
    )
    loudnessStrategy = LoudnessWindowBuffer(
      inputBuffer = audioBuffer,
      outputBuffer = processedBuffer,
      continueFn = { isRecording },
      audioFormat = audioFormat,
      onPacket = {
        DictationSettings.setRmsLevel(((it.rms / rmsMax) * 100).toInt())
        DictationSettings.setIec61672Level(((it.iec61672 / iec61672Max) * 100).toInt())
        DictationSettings.setSpectralEntropyLevel(((it.spectralEntropy / spectralEntropyMax) * 100).toInt())
        DictationSettings.setTalkTime(loudnessStrategy?.talkTime)
        onPacket(it)
      },
    )
    DictationSettings.configuration.addListener {
      loudnessStrategy?.minRMS = DictationSettings.minRMS * rmsMax
      loudnessStrategy?.minIEC61672 = DictationSettings.minIEC61672 * iec61672Max
      loudnessStrategy?.minSpectralEntropy = DictationSettings.minSpectralEntropy
    }

    try {
      isRecording = true
      audioBuffer.clear()
      processedBuffer.clear()
      recordingStartTime = System.currentTimeMillis()
      DictationSettings.setRmsLevel(0)
      DictationSettings.setIec61672Level(0)
      recorder = Thread {
        try {
          AudioRecorder(audioBuffer, packetDuration, { isRecording }, this.selectedMicLine, audioFormat).run()
        } catch (e: Exception) {
          onException(e)
        }
      }.apply { start() }
      windowBuffer = Thread {
        loudnessStrategy?.apply {
          minRMS = DictationSettings.minRMS
          minIEC61672 = DictationSettings.minIEC61672
          minSpectralEntropy = DictationSettings.minSpectralEntropy
          lookbackPackets = ((DictationSettings.lookbackSeconds * 1000.0) / packetDuration).toInt()
          memoryPackets = ((DictationSettings.memorySeconds * 1000.0) / packetDuration).toInt()
        }?.run()
      }.apply {
        start()
      }
      processor = Thread {
        transcriptionProcessor = TranscriptionProcessor(
          OpenAIClient(),
          processedBuffer,
          { isRecording },
          prompt = "",
          onTranscriptionUpdate = onTranscriptionUpdate
        ).apply {
          run()
        }
      }.apply { start() }
    } catch (e: Exception) {
      JOptionPane.showMessageDialog(
        null,
        "Failed to start recording: ${e.message}",
        "Speech-to-Text Error",
        JOptionPane.ERROR_MESSAGE
      )
      stopRecording()
    }
  }

  fun stopRecording() {
    isRecording = false
    recorder?.join()
    windowBuffer?.join()
    processor?.join()
    recorder = null
    recordingStartTime = 0
    loudnessStrategy = null
    windowBuffer = null
    processor = null
    loudnessStrategy = null
  }

  override fun dispose() {
    stopRecording()
    monitoringThread?.interrupt()
    loudnessStrategy = null
    audioBuffer.clear()
    processedBuffer.clear()
  }

}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor