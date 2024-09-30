package com.github.simiacryptus.aicoder.demotest

import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.AudioModels
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress
import kotlin.concurrent.thread
import java.util.concurrent.ConcurrentLinkedQueue

object TestUtil {
    private const val UDP_PORT = 41390
    private var isServerRunning = false
    private val messageBuffer = ConcurrentLinkedQueue<String>()

    fun startUdpServer() {
        if (isServerRunning) return
        isServerRunning = true
        thread(isDaemon = true) {
            try {
                val socket = DatagramSocket(UDP_PORT)
                val buffer = ByteArray(1024)
                log.info("UDP server started on port $UDP_PORT")
                while (isServerRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val received = String(packet.data, 0, packet.length)
                    log.info("Received UDP message: $received")
                    messageBuffer.offer(received)
                }
                socket.close()
            } catch (e: Exception) {
                log.error("Error in UDP server", e)
            } finally {
                isServerRunning = false
            }
        }
    }
    fun stopUdpServer() {
        isServerRunning = false
    }
    fun getReceivedMessages(): List<String> {
        return messageBuffer.toList()
    }
    fun clearMessageBuffer() {
        messageBuffer.clear()
    }




    fun speak(text: String) {
        log.info("Speaking: $text")
        val speechWavBytes = OpenAIClient().createSpeech(
            ApiModel.SpeechRequest(
                input = text,
                model = AudioModels.TTS.modelName,
                voice = "alloy",
                speed = 1.0,
                response_format = "wav"
            )
        ) ?: throw RuntimeException("No response")
        // Play the speech
        val byteInputStream = ByteArrayInputStream(speechWavBytes)
        AudioSystem.getAudioInputStream(byteInputStream).use { originalAudioInputStream ->
            val format = originalAudioInputStream.format
            val frameSize = format.frameSize
            val audioData = originalAudioInputStream.readAllBytes()
            // Ensure all values are positive
            for (i in audioData.indices) {
                audioData[i] = (audioData[i].toInt() and 0xFF).toByte()
            }
            val correctedAudioInputStream = AudioInputStream(ByteArrayInputStream(audioData), format, audioData.size.toLong() / frameSize)

            val clip: Clip = AudioSystem.getClip()
            clip.open(correctedAudioInputStream)
            clip.apply {
                start()
                // Wait for the audio to finish playing
                val millis = (frameLength * 1000L) / format.frameRate.toLong()
                log.info("Playing audio for $millis ms")
                Thread.sleep(millis)
                // Ensure the clip is closed after playing
                close()
            }
        }
        log.info("Audio playback completed")
    }
    val log = LoggerFactory.getLogger(TestUtil::class.java)

}