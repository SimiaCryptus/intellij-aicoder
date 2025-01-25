package com.simiacryptus.aicoder.ui

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import icons.MyIcons
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.awt.event.MouseEvent

class SpeechToTextWidgetFactory : StatusBarWidgetFactory {
  override fun getId(): String = SpeechToTextWidget.ID
  override fun getDisplayName(): String = "AI Speech-to-Text"
  override fun isAvailable(project: Project) = true
  override fun createWidget(project: Project) = SpeechToTextWidget(project)
  override fun createWidget(project: Project, scope: CoroutineScope) = SpeechToTextWidget(project)
  override fun canBeEnabledOn(statusBar: StatusBar) = true
}

class SpeechToTextWidget(private val project: Project) : StatusBarWidget,
  StatusBarWidget.IconPresentation {
  companion object {
    private val log = LoggerFactory.getLogger(SpeechToTextWidget::class.java)
    var statusBar: StatusBar? = null
    val ID = "AICodingAssistant.SpeechToTextWidget"
  }

  override fun ID(): String = ID

  override fun install(statusBar: StatusBar) {
    Companion.statusBar = statusBar
  }

  override fun getPresentation() = this
  override fun getIcon() = if (DictationSettings.isRecording) MyIcons.micActive else MyIcons.micInactive

  override fun getTooltipText(): String = if (DictationSettings.isRecording) "Click to stop recording" else "Click to start recording"

  override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
    Thread { toggleRecording() }.start()
  }


  private fun toggleRecording() {
    if (DictationSettings.isRecording) {
      DictationSettings.setRecordingState(false)
      SpeechRecognitionManager.stopRecording()
    } else {
      DictationSettings.setRecordingState(true)
      SpeechRecognitionManager.startRecording(
        onTranscriptionUpdate = {
          log.info("Transcription: $it")
          project.currentEditor()?.document?.insertString(project.currentEditor()?.caretModel?.offset ?: 0, it)
        },
        onException = {
          log.error("Error during recording", it)
        }
      )
    }
    statusBar?.updateWidget(ID())
  }

}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor