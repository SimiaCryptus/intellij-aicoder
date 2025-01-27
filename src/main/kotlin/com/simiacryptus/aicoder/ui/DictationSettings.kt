package com.simiacryptus.aicoder.ui

import com.simiacryptus.aicoder.config.AppSettingsState

open class DictationSettings {
  companion object : DictationSettings() {
    val log = org.slf4j.LoggerFactory.getLogger(DictationSettings::class.java)
  }

  val configuration = EventDispatcher()
  var packetDuration: Long = 100
    private set
  var talkTime: Double = 1.0
    private set
  var lookbackSeconds: Double = 5.0
    private set
  var memorySeconds: Double = 10.0
    private set
  var isRecording: Boolean = false
    private set
  var minRMS: Double
    private set
  var minIEC61672: Double
    private set
  var minSpectralEntropy: Double
    private set
  var rmsLevel: Int
    private set
  var iec61672Level: Int
    private set
  var spectralEntropyLevel: Int
    private set
  var sampleRate: Int
    private set
  var sampleSize: Int
    private set
  var channels: Int
    private set
  var selectedMicLine: String?
    private set
  init {
    minRMS = AppSettingsState.instance.minRMS
    minIEC61672 = AppSettingsState.instance.minIEC61672
    minSpectralEntropy = AppSettingsState.instance.minSpectralEntropy
    rmsLevel = AppSettingsState.instance.rmsLevel
    iec61672Level = AppSettingsState.instance.iec61672Level
    spectralEntropyLevel = AppSettingsState.instance.spectralEntropyLevel
    sampleRate = AppSettingsState.instance.sampleRate
    sampleSize = AppSettingsState.instance.sampleSize
    channels = AppSettingsState.instance.channels
    selectedMicLine = AppSettingsState.instance.selectedMicLine
    talkTime = AppSettingsState.instance.talkTime
    memorySeconds = AppSettingsState.instance.memorySeconds
    lookbackSeconds = AppSettingsState.instance.lookbackSeconds
  }

  fun setMinSpectralEntropy(value: Double) {
    if (value == minSpectralEntropy) return
    minSpectralEntropy = value
    AppSettingsState.instance.minSpectralEntropy = value
    configuration.notifyListeners()
  }

  fun setSpectralEntropyLevel(value: Int) {
    if (value == spectralEntropyLevel) return
    spectralEntropyLevel = value
    AppSettingsState.instance.spectralEntropyLevel = value
    configuration.notifyListeners()
  }

  fun setMinRMS(value: Double) {
    if (value == minRMS) return
    minRMS = value
    AppSettingsState.instance.minRMS = value
    configuration.notifyListeners()
  }

  fun setMinIEC61672(value: Double) {
    if (value == minIEC61672) return
    minIEC61672 = value
    AppSettingsState.instance.minIEC61672 = value
    configuration.notifyListeners()
  }

  fun setRecordingState(isRecording: Boolean) {
    if (isRecording == this.isRecording) return
    this.isRecording = isRecording
    configuration.notifyListeners()
  }

  fun setRmsLevel(value: Int) {
    if (value == rmsLevel) return
    rmsLevel = value
    AppSettingsState.instance.rmsLevel = value
    configuration.notifyListeners()
  }

  fun setIec61672Level(value: Int) {
    if (value == iec61672Level) return
    iec61672Level = value
    AppSettingsState.instance.iec61672Level = value
    configuration.notifyListeners()
  }

  fun setTalkTime(value: Double?) {
    if (value == null) return
    this.talkTime = value
    AppSettingsState.instance.talkTime = value
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
    SpeechRecognitionManager.selectedMicLine = value
    configuration.notifyListeners()
  }

  fun setLookbackSeconds(value: Double) {
    if (value == lookbackSeconds) return
    lookbackSeconds = value
    AppSettingsState.instance.lookbackSeconds = value
    configuration.notifyListeners()
  }

  fun setMemorySeconds(value: Double) {
    if (value == memorySeconds) return
    memorySeconds = value
    AppSettingsState.instance.memorySeconds = value
    configuration.notifyListeners()
  }

}

