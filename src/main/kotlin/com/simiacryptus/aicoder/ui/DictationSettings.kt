package com.simiacryptus.aicoder.ui

import com.simiacryptus.aicoder.config.AppSettingsState

open class DictationSettings {
  companion object : DictationSettings() {
    val log = org.slf4j.LoggerFactory.getLogger(DictationSettings::class.java)
  }

  var talkTime: Double = 0.0
    private set
  var lookbackSeconds: Double = 2.0
    private set
  var memorySeconds: Double = 60.0
    private set
  private val listeners = mutableListOf<() -> Unit>()
  var isRecording: Boolean = false
    private set
  var minRMS: Double
    private set
  var minIEC61672: Double
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
  var packetDuration: Long = 100
    private set
  init {
    minRMS = AppSettingsState.instance.minRMS
    minIEC61672 = AppSettingsState.instance.minIEC61672
    rmsLevel = AppSettingsState.instance.rmsLevel
    iec61672Level = AppSettingsState.instance.iec61672Level
    sampleRate = AppSettingsState.instance.sampleRate
    sampleSize = AppSettingsState.instance.sampleSize
    channels = AppSettingsState.instance.channels
    selectedMicLine = AppSettingsState.instance.selectedMicLine
  }

  fun setMinRMS(value: Double) {
    if (value == minRMS) return
    minRMS = value
    AppSettingsState.instance.minRMS = value
    notifyListeners()
  }

  fun setMinIEC61672(value: Double) {
    if (value == minIEC61672) return
    minIEC61672 = value
    AppSettingsState.instance.minIEC61672 = value
    notifyListeners()
  }

  fun setRecordingState(isRecording: Boolean) {
    if (isRecording == this.isRecording) return
    this.isRecording = isRecording
    notifyListeners()
  }

  fun setRmsLevel(value: Int) {
    if (value == rmsLevel) return
    rmsLevel = value
    AppSettingsState.instance.rmsLevel = value
    notifyListeners()
  }

  fun setIec61672Level(value: Int) {
    if (value == iec61672Level) return
    iec61672Level = value
    AppSettingsState.instance.iec61672Level = value
    notifyListeners()
  }
  fun setTalkTime(value: Double?) {
    if (value == null) return
    this.talkTime = value
    notifyListeners()
  }

  fun setSampleRate(value: Int) {
    if (value == sampleRate) return
    sampleRate = value
    AppSettingsState.instance.sampleRate = value
    notifyListeners()
  }

  fun setSampleSize(value: Int) {
    if (value == sampleSize) return
    sampleSize = value
    AppSettingsState.instance.sampleSize = value
    notifyListeners()
  }

  fun setChannels(value: Int) {
    if (value == channels) return
    channels = value
    AppSettingsState.instance.channels = value
    notifyListeners()
  }

  fun setSelectedMicLine(value: String?) {
    if (value == selectedMicLine) return
    selectedMicLine = value
    AppSettingsState.instance.selectedMicLine = value
    SpeechRecognitionManager.selectedMicLine = value
    notifyListeners()
  }

  fun addListener(listener: () -> Unit) {
    listeners.add(listener)
  }

  fun removeListener(listener: () -> Unit) {
    listeners.remove(listener)
  }

  private fun notifyListeners() {
    listeners.forEach { it() }
  }
}