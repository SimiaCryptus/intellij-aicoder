package com.simiacryptus.aicoder.dictation

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
    val project = statusBar.project ?: return
    DictationManager.project = project
    val connection = project.messageBus.connect()
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun selectionChanged(event: FileEditorManagerEvent) {

        log.debug("Selection changed")

        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        editor?.document?.addDocumentListener(object : DocumentListener {
          override fun documentChanged(event: DocumentEvent) {
            log.debug("Document changed")
            val str = event.document.text.take(1024)
            DictationManager.transcriptionProcessor?.prompt = str
            log.debug("Prompt updated: $str")
          }
        })

        editor?.selectionModel?.addSelectionListener(object : SelectionListener {
          override fun selectionChanged(event: SelectionEvent) {
            log.debug("Selection changed")
            val str = editor.selectionModel.selectedText?.take(1024) ?: ""
            DictationManager.transcriptionProcessor?.prompt = str
            log.debug("Prompt updated: $str")
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
      DictationManager.stopRecording()
    } else {
      DictationSettings.setRecordingState(true)
      DictationManager.startRecording()
    }
    statusBar?.updateWidget(ID())
  }
}

// Extension function to get current editor
private fun Project.currentEditor() = FileEditorManager
  .getInstance(this)
  .selectedTextEditor