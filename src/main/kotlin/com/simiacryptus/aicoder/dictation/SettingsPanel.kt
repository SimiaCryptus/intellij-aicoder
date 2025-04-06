package com.simiacryptus.aicoder.dictation

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.simiacryptus.jopenai.audio.DictationManager
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.JSlider // Keep JSlider import if needed elsewhere, otherwise remove if unused.

class SettingsPanel(
  val project: Project,
  val settings: DictationState = DictationState,
) : JPanel(), AutoCloseable {
  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(SettingsPanel::class.java)
  }

  private val biasSlider = JSlider(
    JSlider.HORIZONTAL, -100, 100,
    (DictationManager.discriminator.bias * 100).toInt()
  ).apply {
    paintTicks = true
    paintLabels = true
    majorTickSpacing = 50
    minorTickSpacing = 10
    border = JBUI.Borders.emptyRight(5)
    addChangeListener {
      DictationManager.discriminator.bias = value.toDouble() / 100.0
      updateBiasLabel()
    }
  }
  private val biasLabel = JBLabel("Bias: ${biasSlider.value / 100.0}")
  private val msPerPacketSlider = JSlider(JSlider.HORIZONTAL, 10, 500, DictationManager.msPerPacket.toInt()).apply {
    paintTicks = true
    paintLabels = true
    majorTickSpacing = 100
    minorTickSpacing = 50
    border = JBUI.Borders.emptyRight(5)
    addChangeListener {
      DictationManager.msPerPacket = value.toLong()
      updateMsPerPacketLabel()
    }
  }
  private val msPerPacketLabel = JBLabel("Packet Size: ${DictationManager.msPerPacket}ms")
  private val minTalkTimeSlider = JSlider(
    JSlider.HORIZONTAL, 0, 10000,
    (DictationManager.discriminator.minTalkTime * 1000).toInt()
  ).apply {
    paintTicks = true
    paintLabels = true
    majorTickSpacing = 2000
    minorTickSpacing = 500
    border = JBUI.Borders.emptyRight(5)
    addChangeListener {
      DictationManager.discriminator.minTalkTime = value.toDouble() / 1000.0
      updateMinTalkTimeLabel()
    }
  }
  private val minTalkTimeLabel = JBLabel("Min Talk Time: ${DictationManager.discriminator.minTalkTime}s")
  private val quietWindowsSlider = JSlider(
    JSlider.HORIZONTAL, 1, 20,
    DictationManager.discriminator.requiredQuietWindowsForTransition
  ).apply {
    paintTicks = true
    paintLabels = true
    majorTickSpacing = 5
    minorTickSpacing = 1
    border = JBUI.Borders.emptyRight(5)
    addChangeListener {
      DictationManager.discriminator.requiredQuietWindowsForTransition = value
      updateQuietWindowsLabel()
    }
  }
  private val quietWindowsLabel =
    JBLabel("Required Quiet Windows: ${DictationManager.discriminator.requiredQuietWindowsForTransition}")
  private val talkWindowsSlider = JSlider(
    JSlider.HORIZONTAL, 1, 20,
    DictationManager.discriminator.requiredTalkWindowsForTransition
  ).apply {
    paintTicks = true
    paintLabels = true
    majorTickSpacing = 5
    minorTickSpacing = 1
    border = JBUI.Borders.emptyRight(5)
    addChangeListener {
      DictationManager.discriminator.requiredTalkWindowsForTransition = value
      updateTalkWindowsLabel()
    }
  }
  private val talkWindowsLabel =
    JBLabel("Required Talk Windows: ${DictationManager.discriminator.requiredTalkWindowsForTransition}")


  init {
    layout = GridBagLayout()

    add(biasLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 0
    })
    add(biasSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 0
      fill = GridBagConstraints.HORIZONTAL
    })
    add(msPerPacketLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 1
    })
    add(msPerPacketSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 1
      fill = GridBagConstraints.HORIZONTAL
    })
    add(minTalkTimeLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 2
    })
    add(minTalkTimeSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 2
      fill = GridBagConstraints.HORIZONTAL
    })
    add(quietWindowsLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 3
    })
    add(quietWindowsSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 3
      fill = GridBagConstraints.HORIZONTAL
    })
    add(talkWindowsLabel, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 0
      gridy = 4
    })
    add(talkWindowsSlider, GridBagConstraints().apply {
      anchor = GridBagConstraints.WEST
      insets = JBUI.insets(10)
      gridx = 1
      gridy = 4
      fill = GridBagConstraints.HORIZONTAL
    })
    // Filler component to push everything to the top-left
    add(JPanel(), GridBagConstraints().apply {
      gridx = 0
      gridy = 5 // Next available row
      gridwidth = GridBagConstraints.REMAINDER // Span remaining columns
      weightx = 1.0
      weighty = 1.0
      fill = GridBagConstraints.BOTH
    })


    revalidate()
  }


  private fun updateBiasLabel() {
    biasLabel.text = "Bias: ${biasSlider.value / 100.0}"
  }

  private fun updateMsPerPacketLabel() {
    msPerPacketLabel.text = "Packet Size: ${msPerPacketSlider.value}ms"
  }

  private fun updateMinTalkTimeLabel() {
    minTalkTimeLabel.text = "Min Talk Time: ${minTalkTimeSlider.value / 1000.0}s"
  }

  private fun updateQuietWindowsLabel() {
    quietWindowsLabel.text = "Required Quiet Windows: ${quietWindowsSlider.value}"
  }

  private fun updateTalkWindowsLabel() {
    talkWindowsLabel.text = "Required Talk Windows: ${talkWindowsSlider.value}"
  }


  override fun close() {
  }
}