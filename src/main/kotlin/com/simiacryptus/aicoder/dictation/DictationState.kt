package com.simiacryptus.aicoder.dictation

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.audio.AudioPacket
import com.simiacryptus.jopenai.audio.DictationManager
import com.simiacryptus.jopenai.audio.TranscriptionProcessor
import com.simiacryptus.jopenai.models.AudioModels
import com.simiacryptus.jopenai.util.EventDispatcher
import javax.sound.sampled.AudioFormat

open class DictationState {
  companion object : DictationState() {
    val log = org.slf4j.LoggerFactory.getLogger(DictationState::class.java)
  }
  
  val configuration = EventDispatcher()
  var talkTime: Double = 1.0
    private set
  var isRecording: Boolean = false
    private set
  var rmsLevel: Int
    private set
  var iec61672Level: Int
    private set
  var sampleRate: Int
    private set
  var sampleSize: Int
    private set
  var channels: Int
    private set
  var selectedMicLine: String?
    private set
  var transcriptionModel: AudioModels
    private set
  var project: Project? = null
  
  private var iec61672Max = 0.0
  private var rmsMax = 0.0
  
  var recentTranscriptionResult: TranscriptionProcessor.TranscriptionResult? = null
    private set
  val transctiption = EventDispatcher()
  val onTranscriptionUpdate: (TranscriptionProcessor.TranscriptionResult) -> Unit = {
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
  
  init {
    rmsLevel = AppSettingsState.instance.rmsLevel
    iec61672Level = AppSettingsState.instance.iec61672Level
    sampleRate = AppSettingsState.instance.sampleRate
    sampleSize = AppSettingsState.instance.sampleSize
    channels = AppSettingsState.instance.channels
    selectedMicLine = AppSettingsState.instance.selectedMicLine
    talkTime = AppSettingsState.instance.talkTime
    transcriptionModel = AudioModels.find(AppSettingsState.instance.transcriptionModel) ?: AudioModels.Whisper
  }
  
  val onPacket: (AudioPacket) -> Unit = {
    rmsMax = it.rms.coerceAtLeast(rmsMax)
    iec61672Max = it.iec61672.coerceAtLeast(iec61672Max)
    iec61672Level = (((it.iec61672 / iec61672Max) * 100).toInt())
    rmsLevel = (((it.rms / rmsMax) * 100).toInt())
    talkTime = DictationManager.discriminator.talkTime
    configuration.notifyListeners()
  }
  
  fun resetState() {
    rmsMax = 0.0
    iec61672Max = 0.0
    DictationManager.audioFormat = AudioFormat(
      /* sampleRate = */ sampleRate.toFloat(),
      /* sampleSizeInBits = */ sampleSize,
      /* channels = */ channels,
      /* signed = */ true,
      /* bigEndian = */ false
    )
    DictationManager.transcriptionModel = transcriptionModel
  }
  
  fun setRecordingState(isRecording: Boolean) {
    if (isRecording == this.isRecording) return
    this.isRecording = isRecording
    configuration.notifyListeners()
  }
  
  fun setSampleRate(value: Int) {
    if (value == sampleRate) return
    sampleRate = value
    AppSettingsState.instance.sampleRate = value
    configuration.notifyListeners()
  }
  
  fun setSampleSize(value: Int) {
    if (value == sampleSize) return
    sampleSize = value
    AppSettingsState.instance.sampleSize = value
    configuration.notifyListeners()
  }
  
  fun setChannels(value: Int) {
    if (value == channels) return
    channels = value
    AppSettingsState.instance.channels = value
    configuration.notifyListeners()
  }
  
  fun setSelectedMicLine(value: String?) {
    if (value == selectedMicLine) return
    selectedMicLine = value
    AppSettingsState.instance.selectedMicLine = value
    DictationManager.selectedMicLine = value
    configuration.notifyListeners()
  }
  
  fun setTranscriptionModel(model: AudioModels) {
    if (model == transcriptionModel) return
    transcriptionModel = model
    AppSettingsState.instance.transcriptionModel = model.modelName
    DictationManager.transcriptionModel = model
    configuration.notifyListeners()
  }
}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor