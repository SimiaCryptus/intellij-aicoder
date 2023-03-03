package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.OpenAI_API
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.*
import javax.swing.*

class DictationAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(e1: AnActionEvent) {
        val editor = e1.getRequiredData(CommonDataKeys.EDITOR)
        val offset = AtomicInteger(editor.caretModel.offset)
        val audioBuffer = ConcurrentLinkedDeque<ByteArray>()
        val dialog = statusDialog(e1)

        Thread {
            try {
                log.warn("Recording started")
                while(dialog.isVisible) {
                    audioBuffer.add(recordAudio(5)!!)
                }
                log.warn("Recording complete")
            } catch (e: Exception) {
                log.error(e)
            }
        }.start()
        Thread {
            try {
                log.warn("Dictation started")
                while(dialog.isVisible) {
                    val recordAudio = audioBuffer.poll()
                    if(null == recordAudio) {
                        Thread.sleep(10)
                        continue
                    }
                    val text = OpenAI_API.text_to_speech(recordAudio!!)
                    log.warn("Dictation: $text")
                    WriteCommandAction.runWriteCommandAction(e1.project) {
                        editor.document?.insertString(offset.getAndAdd(text.length), text)
                    }
                }
                log.warn("Dictation complete")
            } catch (e: Exception) {
                log.error(e)
            }
        }.start()
    }

    private fun statusDialog(e1: AnActionEvent): JFrame {
        val dialog = JFrame("Dictation")
        val jLabel = JLabel("Close this window to stop recording and dictation")
        jLabel.setFont(jLabel.getFont().deriveFont(48f))
        dialog.add(jLabel)
        dialog.pack()
        dialog.location = e1.getData(PlatformDataKeys.CONTEXT_COMPONENT)?.locationOnScreen
        dialog.isAlwaysOnTop = true
        dialog.isVisible = true
        return dialog
    }

    private fun isEnabled(e: AnActionEvent): Boolean {
        if (!AppSettingsState.getInstance().devActions) return false
        try {
            AudioSystem.getTargetDataLine(audioFormat)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    companion object {
        val log = Logger.getInstance(DictationAction::class.java)

        fun recordAudio(seconds: Int): ByteArray? {
            val targetDataLine = openMic()
            val audio = try {
                recordAudio(seconds, targetDataLine)
            } finally {
                targetDataLine.close()
            }
            return convertRawToWav(audio)
        }

        val audioFormat = AudioFormat(16000f, 16, 1, true, false)

        fun openMic(): TargetDataLine {
            val targetDataLine = AudioSystem.getTargetDataLine(audioFormat)
            targetDataLine.open(audioFormat)
            targetDataLine.start()
            return targetDataLine
        }

        fun convertRawToWav(audio: ByteArray): ByteArray? {
            val audioInputStream = AudioInputStream(ByteArrayInputStream(audio), audioFormat, audio.size.toLong())
            try {
                val wavBuffer = ByteArrayOutputStream()
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavBuffer)
                return wavBuffer.toByteArray()
            } finally {
                audioInputStream.close()
            }
        }

         fun recordAudio(
            seconds: Int,
            targetDataLine: TargetDataLine
        ): ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(16384)
            var bytesRead = 0
            val endTime = System.currentTimeMillis() + seconds * 1000
            while (bytesRead != -1 && System.currentTimeMillis() < endTime) {
                bytesRead = targetDataLine.read(buffer, 0, buffer.size)
                if (bytesRead >= 0) byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
            val audio = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.close()
            return audio
        }

    }

}