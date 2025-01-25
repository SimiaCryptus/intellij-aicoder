package com.simiacryptus.aicoder.ui

open class DictationSettings {
  companion object : DictationSettings() {
    val log = org.slf4j.LoggerFactory.getLogger(DictationSettings::class.java)
  }

  private val listeners = mutableListOf<() -> Unit>()
  var isRecording: Boolean = false
    private set
  var rmsPercentage: Int = 0
    private set
  var iec61672Percentage: Int = 0
    private set
  var rmsPercentileThreshold: Int = 50
    private set
  var iec61672PercentileThreshold: Int = 25
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
    this.isRecording = isRecording
    notifyListeners()
  }


  fun setRmsPercentage(value: Int) {
    rmsPercentage = value
    notifyListeners()
  }

  fun setIec61672Percentage(value: Int) {
    iec61672Percentage = value
    notifyListeners()
  }

  fun setRmsPercentileThreshold(value: Int) {
    rmsPercentileThreshold = value
    notifyListeners()
  }

  fun setIec61672PercentileThreshold(value: Int) {
    iec61672PercentileThreshold = value
    notifyListeners()
  }

  fun setSampleRate(value: Int) {
    sampleRate = value
    notifyListeners()
  }

  fun setSampleSize(value: Int) {
    sampleSize = value
    notifyListeners()
  }

  fun setChannels(value: Int) {
    channels = value
    notifyListeners()
  }

  fun setSelectedMicLine(value: String?) {
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