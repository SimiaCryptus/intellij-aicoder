package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.openai.ui.OpenAI_API
import com.github.simiacryptus.aicoder.util.StyleUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.jetbrains.rd.util.LogLevel
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent

class AppSettingsComponent {
    /*
     Question: Why is the name annotation missing at runtime from the public field?
     Answer: The annotation removes the name annotation from the public field at runtime. This allows the field to be accessed directly from Java code without the need for getter and setter methods.
    */
    @Name("Style")
    val style = JBTextField()

    @Suppress("unused")
    val randomizeStyle = JButton(object : AbstractAction("Randomize Style") {
        override fun actionPerformed(e: ActionEvent) {
            style.text = StyleUtil.randomStyle()
        }
    })

    @Suppress("unused")
    val testStyle = JButton(object : AbstractAction("Test Style") {
        override fun actionPerformed(e: ActionEvent) {
            StyleUtil.demoStyle(style.text)
        }
    })

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
    @Name("History Limit")
    val historyLimit = JBTextField()

    @Suppress("unused")
    @Name("Developer Tools")
    val devActions = JBCheckBox()

    @Suppress("unused")
    @Name("Suppress Progress (UNSAFE)")
    val suppressProgress = JBCheckBox()

    @Suppress("unused")
    @Name("API Log Level")
    val apiLogLevel = ComboBox(Arrays.stream(LogLevel.values()).map { obj: LogLevel -> obj.name }
        .toArray { size: Int -> arrayOfNulls<String>(size) })

    @Suppress("unused")
    @Name("Temperature")
    val temperature = JBTextField()

    @Suppress("unused")
    @Name("Max Tokens")
    val maxTokens = JBTextField()

    @Suppress("unused")
    @Name("Max Prompt (Characters)")
    val maxPrompt = JBTextField()

    @Suppress("unused")
    @Name("Completion Model")
    val model_completion = OpenAI_API.modelSelector

    @Name("Edit Model")
    val model_edit = OpenAI_API.modelSelector

    @Name("Chat Model")
    val model_chat = OpenAI_API.modelSelector

    @Name("API Threads")
    val apiThreads = JBTextField()

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