package com.simiacryptus.aicoder.ui

open class DictationSettings {
  companion object : DictationSettings() {
    val log = org.slf4j.LoggerFactory.getLogger(DictationSettings::class.java)
  }

  private val listeners = mutableListOf<() -> Unit>()
  var isRecording: Boolean = false
    private set
  var rmsLevel: Int = 0
    private set
  var iec61672Level: Int = 0
    private set
  var rmsThreshold: Int = 50
    private set
  var iec61672Threshold: Int = 25
    private set
  var sampleRate: Int = 44100
    private set
  var sampleSize: Int = 16
    private set
  var channels: Int = 1
    private set
  var selectedMicLine: String? = null
    private set
  fun setRecordingState(isRecording: Boolean) {
    if (isRecording == this.isRecording) return
    this.isRecording = isRecording
    notifyListeners()
  }

  fun setRmsLevel(value: Int) {
    if (value == rmsLevel) return
    rmsLevel = value
    notifyListeners()
  }

  fun setIec61672Level(value: Int) {
    if (value == iec61672Level) return
    iec61672Level = value
    notifyListeners()
  }

  fun setRmsThreshold(value: Int) {
    if (value == rmsThreshold) return
    rmsThreshold = value
    notifyListeners()
  }

  fun setIec61672Threshold(value: Int) {
    if (value == iec61672Threshold) return
    iec61672Threshold = value
    notifyListeners()
  }

  fun setSampleRate(value: Int) {
    if (value == sampleRate) return
    sampleRate = value
    notifyListeners()
  }

  fun setSampleSize(value: Int) {
    if (value == sampleSize) return
    sampleSize = value
    notifyListeners()
  }

  fun setChannels(value: Int) {
    if (value == channels) return
    channels = value
    notifyListeners()
  }

  fun setSelectedMicLine(value: String?) {
    if (value == selectedMicLine) return
    selectedMicLine = value
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