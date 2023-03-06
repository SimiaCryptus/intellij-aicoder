package com.github.simiacryptus.aicoder.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import javax.sound.sampled.*
import kotlin.collections.ArrayList

class AudioPump(
    val event: AnActionEvent,
    private val inputBuffer: Deque<ByteArray>,
    private val outputBuffer: Deque<ByteArray>,
    val continueFn: () -> Boolean
) {

    // Required number of quiet windows
    private val quietWindowMax = 3

    // Threshold for quiet windows
    private val quietThreshold = 0.25

    // Maximum number of seconds to flush the buffer
    private val flushSeconds = 60.0

    // Minimum number of seconds to flush the buffer
    private val minSeconds = 1.0

    // List of RMS values currently in the buffer
    private val rmsHeap = ArrayList<Double>()

    // List of consecutive quiet window percentiles
    private val quietWindow = ArrayList<Double>()

    // Byte array output stream
    private val buffer = ByteArrayOutputStream()

    // Main function of the AudioPump class
    fun run() {
        // Loop until the continueFn returns false
        while (this.continueFn()) {
            // Poll the input buffer for a byte array
            val recordAudio = inputBuffer.poll()
            // If the byte array is null, sleep for 1 millisecond and continue
            if (null == recordAudio) {
                Thread.sleep(1)
                continue
            }
            // Write the byte array to the buffer
            buffer.write(recordAudio)
            // Convert the byte array to an RMS value
            val rms = convertRawToRMS(recordAudio)
            // Binary search the RMS value in the rmsHeap list
            var index = rmsHeap.binarySearch(rms)
            // If the index is negative, set it to the negative index - 1
            if (index < 0) index = -index - 1
            // Calculate the percentile of the RMS value
            val percentile = index.toDouble() / rmsHeap.size
            // Calculate the minimum buffer size
            val minBufferSize = AudioRecorder.audioFormat.frameRate * AudioRecorder.audioFormat.frameSize * minSeconds
            // If the buffer size is less than the minimum buffer size, add the percentile to the quiet window and add the RMS value to the rmsHeap list
            if (minBufferSize > buffer.size()) {
                // Add the percentile to the quiet window
                quietWindow.add(percentile)
                // Add the RMS value to the rmsHeap list
                rmsHeap.add(rms)
                // Sort the rmsHeap list
                rmsHeap.sort()
                // Continue to the next iteration of the loop
                continue
            }
            // While the quiet window size is greater than or equal to the quiet window max, remove the first element
            while (quietWindow.size >= quietWindowMax) quietWindow.removeAt(0)
            // While the quiet window is not empty and the maximum value is greater than the quiet threshold, remove the first element
            while (quietWindow.isNotEmpty() && quietWindow.maxOrNull()!! > quietThreshold) quietWindow.removeAt(0)
            // If the percentile is less than the quiet threshold, add the percentile to the quiet window
            if (percentile < quietThreshold) {
                quietWindow.add(percentile)
                // Otherwise, clear the quiet window
            } else {
                quietWindow.clear()
            }
            // Log the RMS value, percentile, and quiet windows
            AudioRecorder.log.warn(
                "RMS: %.3f, percentile: %.3f, quiet windows=[%s]".format(
                    rms,
                    percentile,
                    quietWindow.map { "%.3f".format(it) }.joinToString(", ")
                )
            )
            // Calculate the maximum buffer size
            val maxBufferSize = AudioRecorder.audioFormat.frameRate * AudioRecorder.audioFormat.frameSize * flushSeconds
            // If the buffer size is greater than the maximum buffer size or the quiet window size is greater than or equal to the quiet window max,
            // add the converted raw to wav byte array to the output buffer, reset the buffer, clear the rmsHeap list, and clear the quiet window
            if (buffer.size() > maxBufferSize || quietWindow.size >= quietWindowMax) {
                // Add the converted raw to wav byte array to the output buffer
                outputBuffer.add(convertRawToWav(buffer.toByteArray()))
                // Reset the buffer
                buffer.reset()
                // Clear the rmsHeap list
                rmsHeap.clear()
                // Clear the quiet window
                quietWindow.clear()
                // Otherwise, add the RMS value to the rmsHeap list and sort it
            } else {
                // Add the RMS value to the rmsHeap list
                rmsHeap.add(rms)
                // Sort the rmsHeap list
                rmsHeap.sort()
            }
        }
    }

    companion object {
        // Create a Logger instance for the AudioPump class
        val log = Logger.getInstance(AudioPump::class.java)

        // Function to convert raw audio data to a WAV file
        fun convertRawToWav(audio: ByteArray): ByteArray? {
            // Create an AudioInputStream from the raw audio data
            AudioInputStream(
                ByteArrayInputStream(audio),
                AudioRecorder.audioFormat,
                audio.size.toLong()
            ).use { audioInputStream ->
                // Create a ByteArrayOutputStream to store the WAV file
                val wavBuffer = ByteArrayOutputStream()
                // Write the AudioInputStream to the ByteArrayOutputStream
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavBuffer)
                // Return the WAV file as a ByteArray
                return wavBuffer.toByteArray()
            }
        }

        // Function to convert raw audio data to a Root Mean Square value
        fun convertRawToRMS(audio: ByteArray): Double {
            // Create a ByteArrayInputStream from the raw audio data
            val byteArrayInputStream = ByteArrayInputStream(audio)
            // Create an AudioInputStream from the ByteArrayInputStream
            val audioInputStream =
                AudioInputStream(byteArrayInputStream, AudioRecorder.audioFormat, audio.size.toLong())
            // Create an AudioFloatInputStream from the AudioInputStream
            val audioFloatInputStream =
                AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, audioInputStream)
            // Read all the bytes from the AudioFloatInputStream
            val samples = audioFloatInputStream.readAllBytes()
            // Calculate the sum of the squares of the samples
            var sum = (samples.indices step 2).sumOf { i ->
                // Convert the bytes to a double
                val r = samples[i].toInt()
                val l = samples[i + 1].toInt()
                val sample = ((r and 0xff) or ((l and 0xff) shl 8)).toDouble() / 32768.0
                // Square the sample
                sample * sample
            }
            // Return the Root Mean Square value
            return Math.sqrt(sum / (samples.size / 2.0))
        }

    }
}