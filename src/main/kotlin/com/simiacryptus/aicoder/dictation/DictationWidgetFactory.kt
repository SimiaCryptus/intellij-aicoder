package com.simiacryptus.aicoder.dictation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import com.simiacryptus.jopenai.audio.AudioState
import com.simiacryptus.jopenai.audio.DictationManager
import icons.MyIcons
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap

class DictationWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = SpeechToTextWidget.ID
    override fun getDisplayName(): String = "AI Speech-to-Text"
    override fun isAvailable(project: Project) = true
    override fun createWidget(project: Project) = SpeechToTextWidget()
    override fun createWidget(project: Project, scope: CoroutineScope) = createWidget(project)
    override fun canBeEnabledOn(statusBar: StatusBar) = true

    class SpeechToTextWidget : StatusBarWidget,
        StatusBarWidget.IconPresentation {
        companion object {
            private val log = LoggerFactory.getLogger(SpeechToTextWidget::class.java)
            var statusBar: StatusBar? = null
            val ID = "AICodingAssistant.SpeechToTextWidget"
            private val editorsWithListeners = ConcurrentHashMap.newKeySet<Int>()
            fun toggleRecording() {
                if (DictationState.isRecording) {
                    DictationState.setRecordingState(false)
                    DictationManager.stopRecording()
                } else {
                    DictationState.setRecordingState(true)
                    DictationState.resetState()
                    DictationManager.startRecording()
                }
                statusBar?.updateWidget(ID)
            }
        }

        override fun install(statusBar: StatusBar) {
            DictationManager.onTranscriptionUpdate = DictationState.onTranscriptionUpdate
            DictationManager.handlePacket = DictationState.onPacket
            Companion.statusBar = statusBar
            val project = statusBar.project ?: return
            DictationState.project = project
            val connection = project.messageBus.connect()
            connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    //log.debug("Selection changed")
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                    val editorHash = editor?.hashCode() ?: return
                    if (!editorsWithListeners.add(editorHash)) {
                        //log.debug("Listeners already added to editor")
                        return
                    }
                    editor.document.addDocumentListener(object : DocumentListener {
                        override fun documentChanged(event: DocumentEvent) {
//                            log.debug("Document changed")
                            val str = event.document.text.take(1024)
                            DictationManager.transcriptionProcessor?.prompt = str
//                            log.debug("Prompt updated: $str")
                        }
                    })
                    editor.selectionModel.addSelectionListener(object : SelectionListener {
                        override fun selectionChanged(event: SelectionEvent) {
//                            log.debug("Selection changed")
                            val str = editor.selectionModel.selectedText?.take(1024) ?: ""
                            DictationManager.transcriptionProcessor?.prompt = str
//                            log.debug("Prompt updated: $str")
                        }
                    })
                    editor.caretModel.addCaretListener(object : CaretListener {
                        override fun caretPositionChanged(event: CaretEvent) {
//                            log.debug("Caret position changed")
                            val caret = event.caret
                            val offset = caret?.offset
                            val document = caret?.editor?.document
                            val str = document?.text?.take(offset ?: 0)?.takeLast(1024)
                            DictationManager.transcriptionProcessor?.prompt = str ?: ""
//                            log.debug("Prompt updated on caret move: `${str?.replace("\n", "\\n")}`")
                        }
                    })
                    DictationManager.discriminator.onModeChanged.addListener {
                        Companion.statusBar?.updateWidget(ID)
                    }
                }
            })
        }

        override fun ID(): String = ID
        override fun getPresentation() = this
        override fun getIcon() = when (DictationManager.discriminator.currentState) {
            AudioState.QUIET -> when {
                DictationState.isRecording -> MyIcons.micActive
                else -> MyIcons.micInactive
            }
            AudioState.TALKING -> when {
                DictationState.isRecording -> MyIcons.micListening
                else -> MyIcons.micActive
            }
        }

        override fun getTooltipText(): String =
            if (DictationState.isRecording) "Click to stop recording" else "Click to start recording"

        override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
            ApplicationManager.getApplication().invokeLater { toggleRecording() }
        }

    }
}

