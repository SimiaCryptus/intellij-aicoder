package com.simiacryptus.aicoder.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
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

  override fun install(statusBar: StatusBar) {
    Companion.statusBar = statusBar
    val connection = statusBar.project?.messageBus?.connect()
    connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun selectionChanged(event: FileEditorManagerEvent) {

        val editor = FileEditorManager.getInstance(statusBar.project!!).selectedTextEditor
        editor?.document?.addDocumentListener(object : DocumentListener {
          override fun documentChanged(event: DocumentEvent) {
            SpeechRecognitionManager.transcriptionProcessor?.prompt = event.document.text.take(1024)
          }
        })

        editor?.selectionModel?.addSelectionListener(object : SelectionListener {
          override fun selectionChanged(event: SelectionEvent) {
            SpeechRecognitionManager.transcriptionProcessor?.prompt = editor.selectionModel.selectedText?.take(1024) ?: ""
          }
        })
      }
    })
  }

  override fun ID(): String = ID
  override fun getPresentation() = this
  override fun getIcon() = if (DictationSettings.isRecording) MyIcons.micActive else MyIcons.micInactive
  override fun getTooltipText(): String = if (DictationSettings.isRecording) "Click to stop recording" else "Click to start recording"
  override fun getClickConsumer(): Consumer<MouseEvent> = Consumer { Thread { toggleRecording() }.start() }

  private fun toggleRecording() {
    if (DictationSettings.isRecording) {
      DictationSettings.setRecordingState(false)
      SpeechRecognitionManager.stopRecording()
    } else {
      DictationSettings.setRecordingState(true)
      SpeechRecognitionManager.startRecording()
    }
    statusBar?.updateWidget(ID())
  }
}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor