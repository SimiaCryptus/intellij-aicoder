package com.github.simiacryptus.aicoder.util

import com.intellij.openapi.diagnostic.Logger
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import javax.sound.sampled.*

class AudioRecorder(
    private val audioBuffer: Deque<ByteArray>,
    private val secondsPerPacket: Double,
    val continueFn: () -> Boolean
) {
    fun run() {
        openMic().use { targetDataLine ->
            val buffer = ByteArray((audioFormat.frameRate * audioFormat.frameSize * secondsPerPacket).toInt())
            while (continueFn()) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                var bytesRead = 0
                val endTime = (System.currentTimeMillis() + secondsPerPacket * 1000).toLong()
                while (bytesRead != -1 && System.currentTimeMillis() < endTime) {
                    bytesRead = targetDataLine.read(buffer, 0, buffer.size)
                    if (bytesRead >= 0) byteArrayOutputStream.write(buffer, 0, bytesRead)
                }
                byteArrayOutputStream.close()
                audioBuffer.add(byteArrayOutputStream.toByteArray())
            }
        }
    }

    companion object {
        val log = Logger.getInstance(AudioRecorder::class.java)

        val audioFormat = AudioFormat(16000f, 16, 1, true, false)

        fun openMic(): TargetDataLine {
            val targetDataLine = AudioSystem.getTargetDataLine(audioFormat)
            targetDataLine.open(audioFormat)
            targetDataLine.start()
            return targetDataLine
        }

    }

}