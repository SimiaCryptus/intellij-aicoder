package com.simiacryptus.aicoder.dictation

import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.dictation.DictationManager.Companion
import com.simiacryptus.aicoder.util.EventDispatcher

open class DictationSettings {
  companion object : DictationSettings() {
    val log = org.slf4j.LoggerFactory.getLogger(DictationSettings::class.java)
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
  init {
    rmsLevel = AppSettingsState.instance.rmsLevel
    iec61672Level = AppSettingsState.instance.iec61672Level
    sampleRate = AppSettingsState.instance.sampleRate
    sampleSize = AppSettingsState.instance.sampleSize
    channels = AppSettingsState.instance.channels
    selectedMicLine = AppSettingsState.instance.selectedMicLine
    talkTime = AppSettingsState.instance.talkTime
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
    if (value == this.talkTime) return
    //log.debug("Setting talk time to $value")
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
    DictationManager.selectedMicLine = value
    configuration.notifyListeners()
  }

}

