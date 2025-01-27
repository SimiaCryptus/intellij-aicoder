package com.simiacryptus.aicoder.ui

import com.simiacryptus.jopenai.audio.TranscriptionProcessor
 import java.awt.BorderLayout
 import java.awt.GridLayout
 import java.awt.Font
 import java.awt.Color
 import java.awt.Dimension
 import javax.swing.BorderFactory
 import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class TranscriptionDebugPanel : JPanel() {
    private val transcriptionLabel = JLabel("Transcription: ").apply {
        font = Font("Segoe UI", Font.PLAIN, 14)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }
    private val promptLabel = JLabel("Prompt: ").apply {
        font = Font("Segoe UI", Font.PLAIN, 14)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }
    private val processingTimeLabel = JLabel("Processing Time: ").apply {
        font = Font("Segoe UI", Font.PLAIN, 14)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }
    private val latencyLabel = JLabel("Latency: ").apply {
        font = Font("Segoe UI", Font.PLAIN, 14)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }

    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        background = Color(250, 250, 250)
        preferredSize = Dimension(500, 300)

        // Add transcription info panel
        val transcriptionPanel = JPanel(GridLayout(0, 1, 5, 5)).apply {
            border = BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(180, 180, 180), 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ),
                "Transcription Info"
            ).apply {
                titleFont = Font("Segoe UI", Font.BOLD, 16)
                titleColor = Color(60, 60, 60)
            }
            background = Color(250, 250, 250)
            add(transcriptionLabel)
            add(promptLabel)
            add(processingTimeLabel)
            add(latencyLabel)
        }
        add(transcriptionPanel, BorderLayout.CENTER)

        SpeechRecognitionManager.transctiption.addListener {
            val result = SpeechRecognitionManager.recentTranscriptionResult ?: return@addListener
            SwingUtilities.invokeLater {
                updateTranscriptionDetails(result)
            }
        }
    }
    
    private fun updateTranscriptionDetails(result: TranscriptionProcessor.TranscriptionResult) {
        val age = System.currentTimeMillis() - result.packet.createdOn
        val ageInSeconds = age / 1000.0
        // Update transcription info
        transcriptionLabel.text = "<html><b style='color:#2C3E50'>Transcription:</b> <span style='color:#34495E'>${result.text}</span></html>"
        promptLabel.text = "<html><b style='color:#2C3E50'>Prompt:</b> <span style='color:#34495E'>${result.prompt}</span></html>"
        processingTimeLabel.text = "<html><b style='color:#2C3E50'>Processing Time:</b> <span style='color:#34495E'>${result.processingTime}ms</span></html>"
        latencyLabel.text = "<html><b style='color:#2C3E50'>Latency:</b> <span>%.2f seconds</span></html>".format(ageInSeconds)
        // Update colors based on latency
        latencyLabel.foreground = when {
            ageInSeconds < 1.0 -> Color(46, 204, 113)  // Green for good latency
            ageInSeconds < 2.0 -> Color(241, 196, 15) // Yellow for moderate latency
            else -> Color(231, 76, 60) // Red for high latency
        }
    }
}