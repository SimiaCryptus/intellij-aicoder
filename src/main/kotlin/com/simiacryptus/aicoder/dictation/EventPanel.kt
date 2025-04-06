package com.simiacryptus.aicoder.dictation

import com.simiacryptus.jopenai.audio.TranscriptionProcessor
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*

class EventPanel : JPanel() {
    companion object {
        private const val MAX_RECORDS = 100
    }

    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        // Use default background
        // preferredSize = Dimension(500, 300) // Let layout manager decide preferred size

        // Initialize transcription list
        val listModel = DefaultListModel<TranscriptionProcessor.TranscriptionResult>()
        val transcriptionList = JList(listModel)
        transcriptionList.setCellRenderer(object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                val result = value as TranscriptionProcessor.TranscriptionResult
                text = result.text
                return this
            }
        })
        val listScrollPane = JScrollPane(transcriptionList)
        // Add control buttons panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            // Use default background
            add(JButton("Clear History").apply {
                addActionListener {
                    listModel.clear()
                }
            })
        }

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
            // Use default background
            // Initialize details panel
            val details = JPanel(GridBagLayout()).apply {
                border = BorderFactory.createTitledBorder("Details")
                // Use default background
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
            val textValue = JTextArea().apply {
                lineWrap = true
                wrapStyleWord = true
                isEditable = false // Keep non-editable
                // Use default background/foreground
                // background = UIUtil.getPanelBackground() // Or specific component background
                border = BorderFactory.createLineBorder(Color(200, 200, 200))
            }
            val promptValue = JTextArea().apply {
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                // Use default background/foreground
                foreground = Color.BLACK
                border = BorderFactory.createLineBorder(Color(200, 200, 200))
            }
            val processingTimeValue = JLabel()
            val durationValue = JLabel()
            gbc.gridy = 0
            details.add(JScrollPane(textValue).apply {
                preferredSize = Dimension(300, 100)
            }, gbc)
            gbc.gridy++
            details.add(JScrollPane(promptValue).apply {
                preferredSize = Dimension(300, 100)
            }, gbc)
            gbc.gridy++
            details.add(processingTimeValue, gbc)
            gbc.gridy++
            details.add(durationValue, gbc)

            // Create split pane
            val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, details).apply {
                dividerLocation = 200
                resizeWeight = 0.3
            }
            add(splitPane)
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
                    val textValue = details.getClientProperty("textValue") as JTextArea
                    val promptValue = details.getClientProperty("promptValue") as JTextArea
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
        add(buttonPanel, BorderLayout.SOUTH)

        DictationState.transctiption.addListener {
            val result = DictationState.recentTranscriptionResult ?: return@addListener
            SwingUtilities.invokeLater {
                // Remove oldest item if limit is reached
                if (listModel.size >= MAX_RECORDS) {
                    listModel.remove(0)
                }
                listModel.addElement(result)
                // Auto-scroll to the latest item
                transcriptionList.selectedIndex = listModel.size() - 1
                transcriptionList.ensureIndexIsVisible(listModel.size() - 1)
            }
        }
    }

}