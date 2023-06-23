@file:Suppress("unused")

package com.github.simiacryptus.aicoder.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.simiacryptus.openai.OpenAIClient
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
    @Name("Enable API Log")
    val apiLog = JBCheckBox()

    @Suppress("unused")
    val openApiLog = JButton(object : AbstractAction("Open API Log") {
        override fun actionPerformed(e: ActionEvent) {
            OpenAIClient.auxillaryLog?.let {
                val project = ApplicationManager.getApplication().runReadAction<Project> {
                    com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
                }
                if (it.exists()) {
                    ApplicationManager.getApplication().invokeLater {
                        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(it)
                        FileEditorManager.getInstance(project!!).openFile(virtualFile!!, true)
                    }
                } else {
                    log.warn("Log file not found: ${it.absolutePath}")
                }
            }
        }
    })


    @Suppress("unused")
    @Name("Developer Tools")
    val devActions = JBCheckBox()

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