@file:Suppress("unused")

package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent

class AppSettingsComponent {

    @Name("Token Counter")
    val tokenCounter = JBTextField()

    @Suppress("unused")
    val clearCounter = JButton(object : AbstractAction("Clear Token Counter") {
        override fun actionPerformed(e: ActionEvent) {
            tokenCounter.text = "0"
        }
    })

    @Suppress("unused")
    @Name("Human Language")
    val humanLanguage = JBTextField()

    @Suppress("unused")
    @Name("Use GPT-4")
    val useGPT4 = JBCheckBox()

    @Suppress("unused")
    @Name("Developer Tools")
    val devActions = JBCheckBox()

    @Suppress("unused")
    @Name("API Log Level")
    val apiLogLevel = ComboBox(org.slf4j.event.Level.values().map { it.name }.toTypedArray())

    @Suppress("unused")
    @Name("Temperature")
    val temperature = JBTextField()

    @Name("API Key")
    val apiKey = JBPasswordField()

    @Suppress("unused")
    @Name("API Base")
    val apiBase = JBTextField()

    init {
        tokenCounter.isEditable = false
    }

    val preferredFocusedComponent: JComponent
        get() = apiKey

    companion object {
        private val log = Logger.getInstance(
            AppSettingsComponent::class.java
        )
    }
}