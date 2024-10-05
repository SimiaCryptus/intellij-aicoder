package com.github.simiacryptus.aicoder.test.demotest

import org.monte.media.av.Format
import org.monte.media.av.FormatKeys
import org.monte.media.av.codec.audio.AudioFormatKeys
import org.monte.media.av.codec.video.VideoFormatKeys
import org.monte.media.math.Rational
import org.monte.media.screenrecorder.ScreenRecorder
import org.slf4j.LoggerFactory
import java.awt.AWTException
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.io.File
import java.io.IOException
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

open class ScreenRec {
    companion object {
        private val log = LoggerFactory.getLogger(ScreenRec::class.java)
    }

    private var screenRecorder: ScreenRecorder? = null
    private val lock = Any()
    private val screenRecordingStarted = AtomicBoolean(false)

    fun File.waitFor(message: String, timeoutMs: Long = 10000, intervalMs: Long = 100) {
        val startTime = System.currentTimeMillis()
        while (!this.exists() || !this.canRead()) {
            if ((System.currentTimeMillis() - startTime) > timeoutMs) {
                log.warn("Timeout while $message")
                return
            }
            Thread.sleep(intervalMs)
        }

    }

    protected fun startScreenRecording() {
        synchronized(lock) {
            if (screenRecordingStarted.get()) {
                log.warn("Screen recording already started")
                return
            }
            try {
                log.info("Initializing screen recording...")
                val gd = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
                val captureSize = Rectangle(0, 0, 1920, 1080) // Adjust as needed
                val outputFolder = File("test-recordings")
                outputFolder.mkdirs()
                val fileFormat = Format(
                    FormatKeys.MediaTypeKey, FormatKeys.MediaType.FILE,
                    FormatKeys.MimeTypeKey, FormatKeys.MIME_AVI,
                )
                val screenFormat = Format(
                    FormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO,
                    FormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                    VideoFormatKeys.CompressorNameKey, VideoFormatKeys.ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                    VideoFormatKeys.DepthKey, 24,
                    FormatKeys.FrameRateKey, Rational(15, 1),
                    VideoFormatKeys.QualityKey, 1.0f,
                    FormatKeys.KeyFrameIntervalKey, 15 * 60
                )
                val mouseFormat = Format(
                    FormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO,
                    FormatKeys.EncodingKey, "black",
                    FormatKeys.FrameRateKey, Rational(15, 1)
                )
                val audioFormat = Format(
                    FormatKeys.MediaTypeKey, FormatKeys.MediaType.AUDIO,
                    FormatKeys.EncodingKey, AudioFormatKeys.ENCODING_PCM_SIGNED,
                    AudioFormatKeys.SampleRateKey, Rational(44100, 1),
                    AudioFormatKeys.SampleSizeInBitsKey, 16,
                    AudioFormatKeys.ChannelsKey, 2,
                    AudioFormatKeys.FrameSizeKey, 1,
                    AudioFormatKeys.SignedKey, true,
                    AudioFormatKeys.ByteOrderKey, ByteOrder.LITTLE_ENDIAN,
                )
                try {
                    log.info("Creating ScreenRecorder instance...")
                    val recorder = ScreenRecorder(
                        gd.defaultConfiguration,
                        captureSize,
                        fileFormat,
                        screenFormat,
                        mouseFormat,
                        audioFormat,
                        outputFolder
                    )
                    log.info("Starting ScreenRecorder...")
                    recorder.start()
                    screenRecorder = recorder
                    screenRecordingStarted.set(true)
                    log.info("Screen recording started successfully")
                } catch (e: Exception) {
                    log.error("Failed to initialize ScreenRecorder", e)
                    log.error("GraphicsDevice: ${gd.iDstring}, isFullScreenSupported: ${gd.isFullScreenSupported}")
                    log.error("OutputFolder exists: ${outputFolder.exists()}, canWrite: ${outputFolder.canWrite()}")
                    throw e
                }
            } catch (e: AWTException) {
                log.error("Failed to start screen recording", e)
                cleanupResources()
            } catch (e: IOException) {
                log.error("Failed to start screen recording", e)
                cleanupResources()
            } catch (e: Throwable) {
                log.error("Unexpected error during screen recording setup", e)
                cleanupResources()
                throw e
            }
        }
    }

    protected fun stopScreenRecording() {
        synchronized(lock) {
            if (!screenRecordingStarted.get()) {
                log.warn("Screen recording was not started, nothing to stop")
                return
            }
            try {
                log.info("Stopping screen recording...")
                screenRecorder?.stop()
                screenRecorder?.createdMovieFiles?.firstOrNull()?.absoluteFile?.apply {
                    val dest = this.parentFile.resolve(this@ScreenRec.javaClass.simpleName + ".avi")
                    waitFor("Waiting for file to exist: $this", timeoutMs = 10000) // Reduced timeout
                    log.info("Rename $this to $dest")
                    if (!this.renameTo(dest)) {
                        log.error("Failed to rename $this to $dest")
                    }
                } ?: log.warn("No screen recording file was created")
            } catch (e: Exception) {
                log.error("Error stopping screen recording", e)
            } finally {
                cleanupResources()
            }
        }
    }

    private fun cleanupResources() {
        try {
            log.info("Cleaning up screen recording resources...")
            screenRecorder?.stop()
        } catch (e: Exception) {
            log.error("Error during resource cleanup", e)
        } finally {
            screenRecorder = null
            screenRecordingStarted.set(false)
            log.info("Screen recording resources cleaned up")
        }
    }
}