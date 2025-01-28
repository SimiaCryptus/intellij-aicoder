package com.simiacryptus.aicoder.dictation

import com.simiacryptus.jopenai.audio.TranscriptionProcessor
import java.awt.*
import javax.swing.*

class EventPanel : JPanel() {
    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        background = Color(250, 250, 250)
        preferredSize = Dimension(500, 300)
        // Initialize transcription list
        val listModel = DefaultListModel<TranscriptionProcessor.TranscriptionResult>()
        val transcriptionList = JList(listModel)
        val listScrollPane = JScrollPane(transcriptionList)

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
            // Initialize details panel
            val details = JPanel(GridBagLayout()).apply {
                border = BorderFactory.createTitledBorder("Details")
                background = Color(250, 250, 250)
            }
            val gbc = GridBagConstraints()
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 0.0
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.anchor = GridBagConstraints.WEST
            // Labels column
            details.add(JLabel("Text:", JLabel.RIGHT), gbc)
            gbc.gridy++
            details.add(JLabel("Prompt:", JLabel.RIGHT), gbc)
            gbc.gridy++
            details.add(JLabel("Processing Time (ms):", JLabel.RIGHT), gbc)
            gbc.gridy++
            details.add(JLabel("Audio Duration (s):", JLabel.RIGHT), gbc)
            // Value labels column
            gbc.gridx = 1
            gbc.weightx = 1.0
            gbc.insets.left = 10
            val textValue = JLabel()
            val promptValue = JLabel()
            val processingTimeValue = JLabel()
            val durationValue = JLabel()
            gbc.gridy = 0
            details.add(textValue, gbc)
            gbc.gridy++
            details.add(promptValue, gbc)
            gbc.gridy++
            details.add(processingTimeValue, gbc)
            gbc.gridy++
            details.add(durationValue, gbc)

            // Create split pane
            val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, details).apply {
                dividerLocation = 200
                resizeWeight = 0.3
            }
            add(splitPane, BorderLayout.CENTER)
            // Store references to value labels
            details.putClientProperty("textValue", textValue)
            details.putClientProperty("promptValue", promptValue)
            details.putClientProperty("processingTimeValue", processingTimeValue)
            details.putClientProperty("durationValue", durationValue)
        }
        // Add listener to update details on selection
        transcriptionList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val selected = transcriptionList.selectedValue
                selected?.let { result ->
                    // Get references to value labels
                    val details = transcriptionPanel.components.first { it is JSplitPane }
                        .let { (it as JSplitPane).rightComponent as JPanel }
                    val textValue = details.getClientProperty("textValue") as JLabel
                    val promptValue = details.getClientProperty("promptValue") as JLabel
                    val processingTimeValue = details.getClientProperty("processingTimeValue") as JLabel
                    val durationValue = details.getClientProperty("durationValue") as JLabel
                    // Update values
                    textValue.text = result.text
                    promptValue.text = result.prompt ?: "N/A"
                    processingTimeValue.text = "${result.processingTime}"
                    durationValue.text = String.format("%.2f", result.packet.duration)
                }
            }
        }
        add(transcriptionPanel, BorderLayout.CENTER)

        DictationManager.transctiption.addListener {
            val result = DictationManager.recentTranscriptionResult ?: return@addListener
            SwingUtilities.invokeLater {
                updateTranscriptionDetails(result, listModel)
            }
        }
    }

    private fun updateTranscriptionDetails(
        result: TranscriptionProcessor.TranscriptionResult,
        listModel: DefaultListModel<TranscriptionProcessor.TranscriptionResult>
    ) {
        val age = System.currentTimeMillis() - result.packet.createdOn
        val ageInSeconds = age / 1000.0
        listModel.addElement(result)
    }
}