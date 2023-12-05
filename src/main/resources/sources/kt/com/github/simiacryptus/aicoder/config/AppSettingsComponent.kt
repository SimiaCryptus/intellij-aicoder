@file:Suppress("unused")

package com.github.simiacryptus.aicoder.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.simiacryptus.jopenai.ClientUtil
import com.simiacryptus.jopenai.models.ChatModels

import org.slf4j.LoggerFactory
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
    @Name("Listening Port")
    val listeningPort = JBTextField()

    @Suppress("unused")
    @Name("Listening Endpoint")
    val listeningEndpoint = JBTextField()

    @Suppress("unused")
    @Name("Suppress Errors")
    val suppressErrors = JBCheckBox()

    @Suppress("unused")
    @Name("Model")
    val modelName = ComboBox<String>()

    @Suppress("unused")
    @Name("Enable API Log")
    val apiLog = JBCheckBox()

    @Suppress("unused")
    val openApiLog = JButton(object : AbstractAction("Open API Log") {
        override fun actionPerformed(e: ActionEvent) {
            ClientUtil.auxiliaryLog?.let {
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
    @Name("Edit API Requests")
    val editRequests = JBCheckBox()

    @Suppress("unused")
    @Name("Temperature")
    val temperature = JBTextField()

    @Name("API Key")
    val apiKey = JBPasswordField()

    @Suppress("unused")
    @Name("API Base")
    val apiBase = JBTextField()

    @Name("File Actions")
    var fileActions = ActionTable(AppSettingsState.instance.fileActions.actionSettings.values.map { it.copy() }
        .toTypedArray().toMutableList())

    @Name("Editor Actions")
    var editorActions = ActionTable(AppSettingsState.instance.editorActions.actionSettings.values.map { it.copy() }
        .toTypedArray().toMutableList())

    init {
        tokenCounter.isEditable = false
        this.modelName.addItem(ChatModels.GPT35Turbo.modelName)
        this.modelName.addItem(ChatModels.GPT4.modelName)
        this.modelName.addItem(ChatModels.GPT4Turbo.modelName)
    }

    val preferredFocusedComponent: JComponent
        get() = apiKey

    class ActionChangedListener {
        fun actionChanged() {
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AppSettingsComponent::class.java)
        //val ACTIONS_TOPIC = Topic.create("Actions", ActionChangedListener::class.java)
    }
}
