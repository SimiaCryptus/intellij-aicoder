package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.openai.ui.OpenAI_API
import com.simiacryptus.util.AudioRecorder
import com.simiacryptus.util.LoudnessWindowBuffer
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.simiacryptus.util.LookbackLoudnessWindowBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioSystem
import javax.swing.JFrame
import javax.swing.JLabel

class DictationAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled()
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val continueFn = statusDialog(event)::isVisible

        val rawBuffer = ConcurrentLinkedDeque<ByteArray>()
        Thread({
            try {
                log.warn("Recording thread started")
                AudioRecorder(rawBuffer, 0.05, continueFn).run()
                log.warn("Recording thread complete")
            } catch (e: Throwable) {
                log.error(e)
            }
        }, "dication-audio-recorder").start()

        val wavBuffer = ConcurrentLinkedDeque<ByteArray>()
        Thread({
            log.warn("Audio processing thread started")
            try {
                LookbackLoudnessWindowBuffer(rawBuffer, wavBuffer, continueFn).run()
            } catch (e: Throwable) {
                log.error(e)
            }
            log.warn("Audio processing thread complete")
        }, "dictation-audio-processor").start()

        val caretModel = event.getRequiredData(CommonDataKeys.EDITOR).caretModel
        val primaryCaret = caretModel.primaryCaret
        val dictationPump = if (primaryCaret.hasSelection()) {
            DictationPump(event, wavBuffer, continueFn, primaryCaret.selectionEnd, primaryCaret.selectedText ?: "")
        } else {
            DictationPump(event, wavBuffer, continueFn, caretModel.offset)
        }
        Thread({
            log.warn("Speech-To-Text thread started")
            try {
                dictationPump.run()
            } catch (e: Throwable) {
                log.error(e)
            }
            log.warn("Speech-To-Text thread complete")
        }, "dictation-api-processor").start()
    }

    private class DictationPump(
        val event: AnActionEvent,
        private val audioBuffer: Deque<ByteArray>,
        val continueFn: () -> Boolean,
        offsetStart: Int,
        var prompt: String = ""
    ) {

        private val offset: AtomicInteger = AtomicInteger(offsetStart)

        fun run() {
            while (this.continueFn() || audioBuffer.isNotEmpty()) {
                val recordAudio = audioBuffer.poll()
                if (null == recordAudio) {
                    Thread.sleep(1)
                } else {
                    log.warn("Speech-To-Text Starting...")
                    var text = OpenAI_API.text_to_speech(recordAudio, prompt)
                    if (prompt.isNotEmpty()) text = " $text"
                    val newPrompt = (prompt + text).split(" ").takeLast(32).joinToString(" ")
                    log.warn(
                        """Speech-To-Text Complete
                        |   Prompt: $prompt
                        |   Result: $text""".trimMargin()
                    )
                    prompt = newPrompt
                    WriteCommandAction.runWriteCommandAction(event.project) {
                        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
                        editor.document.insertString(offset.getAndAdd(text.length), text)
                    }
                }
            }
        }
    }


    private fun statusDialog(e1: AnActionEvent): JFrame {
        val dialog = JFrame("Dictation")
        val jLabel = JLabel("Close this window to stop recording and dictation")
        jLabel.font = jLabel.font.deriveFont(48f)
        dialog.add(jLabel)
        dialog.pack()
        dialog.location = e1.getData(PlatformDataKeys.CONTEXT_COMPONENT)?.locationOnScreen!!
        dialog.isAlwaysOnTop = true
        dialog.isVisible = true
        return dialog
    }

    private fun isEnabled(): Boolean {
        if (UITools.isSanctioned()) return false
        try {
            AudioSystem.getTargetDataLine(AudioRecorder.audioFormat)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    companion object {
        val log = Logger.getInstance(DictationAction::class.java)
    }

}