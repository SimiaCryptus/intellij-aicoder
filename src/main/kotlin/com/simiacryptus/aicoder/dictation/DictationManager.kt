package com.simiacryptus.aicoder.dictation

import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.util.EventDispatcher
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.audio.*
import com.simiacryptus.jopenai.audio.TranscriptionProcessor.TranscriptionResult
import org.slf4j.LoggerFactory
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.swing.JOptionPane

open class DictationManager : Disposable {
  companion object : DictationManager() {
    private val log = LoggerFactory.getLogger(DictationManager::class.java)
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
    DictationSettings.setIec61672Level(((it.iec61672 / iec61672Max) * 100).toInt())
    DictationSettings.setRmsLevel(((it.rms / rmsMax) * 100).toInt())
  }

  private fun handlePacket(it: AudioPacket) {
    DictationSettings.setRmsLevel(((it.rms / rmsMax) * 100).toInt())
    DictationSettings.setIec61672Level(((it.iec61672 / iec61672Max) * 100).toInt())
    DictationSettings.setTalkTime(loudnessStrategy.talkTime)
    onPacket(it)
  }

  var selectedMicLine: String? = null
  var transcriptionProcessor: TranscriptionProcessor? = null

  var audioFormat: AudioFormat = AudioFormat(16000f, 16, 1, true, false)
    set(value) {
      field = value
    }
  private var iec61672Max = 0.0
  private var rmsMax = 0.0
  private var isRecording = false
  private var recordingStartTime: Long = 0
  private val audioBuffer: Queue<AudioPacket> = LinkedList()
  private val processedBuffer: Queue<AudioPacket> = LinkedList()
  private var recorder: Thread? = null
  private var processor: Thread? = null
  private var windowBuffer: Thread? = null
  var project: Project? = null
  var loudnessStrategy = TrainedSilenceDiscriminator(
    inputBuffer = audioBuffer,
    outputBuffer = processedBuffer,
    onPacket = { handlePacket(it) },
    continueFn = { isRecording },)
    private set

  var recentTranscriptionResult: TranscriptionResult? = null
    private set
  val transctiption = EventDispatcher()
  private val onTranscriptionUpdate: (TranscriptionResult) -> Unit = {
    recentTranscriptionResult = it
    transctiption.notifyListeners()
    WriteCommandAction.runWriteCommandAction(project) {
      val project = project
      if (project == null) {
        log.info("Dictation Ignored - No project")
        return@runWriteCommandAction
      }
      val currentEditor = project.currentEditor()
      if (currentEditor == null) {
        log.info("Dictation Ignored - No current editor")
        return@runWriteCommandAction
      }
      log.info("Dictated Insertion: ${it.text}")
      currentEditor.document.insertString(currentEditor.caretModel.offset, it.text)
      currentEditor.caretModel.moveToOffset(currentEditor.caretModel.offset + it.text.length)
    }
  }
  var onException: (java.lang.Exception) -> Unit = { log.error("Error during recording", it) }

  @Suppress("LongParameterList")
  fun startRecording() {
    resetState()
    try {
      isRecording = true
      audioBuffer.clear()
      processedBuffer.clear()
      recordingStartTime = System.currentTimeMillis()
      DictationSettings.setRmsLevel(0)
      DictationSettings.setIec61672Level(0)
      recorder = Thread {
        try {
          AudioRecorder(audioBuffer, 100, { isRecording }, this.selectedMicLine, audioFormat).run()
        } catch (e: Exception) {
          onException(e)
        }
      }.apply { start() }
      windowBuffer = Thread {
        log.info("Starting WindowBuffer processing loop.")
        while (loudnessStrategy.let { it.shouldContinue(it.inputBuffer) } == true) {
          loudnessStrategy.let { it.poll(it.inputBuffer) }
          }
        log.info("WindowBuffer processing loop ended.")
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

  private fun resetState() {
    rmsMax = 0.0
    iec61672Max = 0.0
    audioFormat = AudioFormat(
      /* sampleRate = */ DictationSettings.sampleRate.toFloat(),
      /* sampleSizeInBits = */ DictationSettings.sampleSize,
      /* channels = */ DictationSettings.channels,
      /* signed = */ true,
      /* bigEndian = */ false
      )
    }

  fun stopRecording() {
    isRecording = false
    recorder?.join()
    windowBuffer?.join()
    processor?.join()
    recorder = null
    recordingStartTime = 0
    windowBuffer = null
    processor = null
    log.info("Recording stopped")
  }

  override fun dispose() {
    stopRecording()
    audioBuffer.clear()
    processedBuffer.clear()
  }

}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor