package com.simiacryptus.aicoder.ui

import com.intellij.openapi.Disposable
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.audio.*
import org.slf4j.LoggerFactory
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.swing.JOptionPane
import kotlin.collections.ArrayDeque

open class SpeechRecognitionManager : Disposable {
  companion object : SpeechRecognitionManager() {
    val log = LoggerFactory.getLogger(SpeechRecognitionManager::class.java)
  }

  var audioFormat = AudioFormat(16000f, 16, 1, true, false)
    set(value) {
      field = value
      loudnessStrategy = null
    }
  private val recentPacketBuffer = ArrayDeque<AudioPacket>()
  val audioBuffer = LinkedList<ByteArray>()
  val processedBuffer = LinkedList<ByteArray>()
  var isRecording = false
  var recordingStartTime: Long = 0
  private var recordingDuration: Long = 0
  private var recorder: Thread? = null
  private var processor: Thread? = null
  private var windowBuffer: Thread? = null
  var loudnessStrategy: LoudnessWindowBuffer? = null
  private var monitoringThread: Thread? = null
  var selectedMicLine: String? = null
    set(value) {
      field = value
    }
  private var onTranscriptionUpdate: (String) -> Unit = {}

  val availableMicLines: List<String>
    get() {
      return AudioSystem.getMixerInfo().filter {
        val mixer = AudioSystem.getMixer(it)
        !mixer.targetLineInfo.isNullOrEmpty()
      }.map { it.toString() }.toList()
    }

  @Suppress("LongParameterList")
  fun startRecording(
    onRmsUpdate: (AudioPacket) -> Unit,
    onIec61672Update: (AudioPacket) -> Unit,
    onSpectralEntropyUpdate: (AudioPacket) -> Unit,
    onTranscriptionUpdate: (String) -> Unit,
    onRecordingStarted: (Long) -> Unit,
    onRecordingDurationUpdate: (Long) -> Unit,
    onRecordingStateChanged: (Boolean) -> Unit,
    onRecordingStopped: () -> Unit = {},
    onException: (java.lang.Exception) -> Unit = {}
  ) {
    this.onTranscriptionUpdate = onTranscriptionUpdate
    try {
      isRecording = true
      audioBuffer.clear()
      processedBuffer.clear()
      recentPacketBuffer.clear()
      recordingStartTime = System.currentTimeMillis()
      recordingDuration = 0
      onRecordingStarted(recordingStartTime)

      // Start audio recorder
      recorder = Thread {
        try {
          AudioRecorder(audioBuffer, 0.5, { isRecording }, this.selectedMicLine, audioFormat).run()
        } catch (e: Exception) {
          onException(e)
        } finally {
          onRecordingStopped()
        }
      }.apply { start() }

      // Start window buffer
      windowBuffer = Thread {
        windowBuffer(onRmsUpdate, onIec61672Update).run()
      }.apply { start() }

      // Start transcription processor
      processor = Thread {
        TranscriptionProcessor(
          OpenAIClient(),
          processedBuffer,
          { isRecording },
          prompt = "",
          onTranscriptionUpdate = onTranscriptionUpdate
        ).run()
      }.apply { start() }

      // Start monitoring thread
      monitoringThread = Thread {
        while (isRecording) {
          val packet = processedBuffer.poll()
          if (packet != null) {
            val audioPacket = AudioPacket.fromByteArray(packet, audioFormat)
            recentPacketBuffer.add(audioPacket)
            recentPacketBuffer.removeIf { it.duration > 5.0 }
            recordingDuration = System.currentTimeMillis() - recordingStartTime
            onSpectralEntropyUpdate(audioPacket)
            onSpectralEntropyUpdate(audioPacket)
          }
          Thread.sleep(100)
        }
        onRecordingDurationUpdate(recordingDuration)
      }.apply { start() }

      onRecordingStateChanged(true)
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

  private fun windowBuffer(
    onRmsUpdate: (AudioPacket) -> Unit,
    onIec61672Update: (AudioPacket) -> Unit
  ) = LookbackLoudnessWindowBuffer(
    audioBuffer,
    processedBuffer,
    onRmsUpdate,
    onIec61672Update,
    { isRecording },
    audioFormat
  )

  fun stopRecording() {
    isRecording = false
    recorder?.join()
    windowBuffer?.join()
    processor?.join()
    recorder = null
    recordingStartTime = 0
    recordingDuration = 0
    loudnessStrategy = null
    windowBuffer = null
    processor = null
    recentPacketBuffer.clear()
    loudnessStrategy = null
  }

  override fun dispose() {
    stopRecording()
    monitoringThread?.interrupt()
    recentPacketBuffer.clear()
    loudnessStrategy = null
    recordingDuration = 0
    audioBuffer.clear()
    processedBuffer.clear()
  }

}