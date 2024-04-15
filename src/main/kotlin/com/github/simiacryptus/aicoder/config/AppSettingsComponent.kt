package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import java.awt.event.ActionEvent
import java.io.FileOutputStream
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class AppSettingsComponent : com.intellij.openapi.Disposable {

//  @Name("Token Counter")
//  val tokenCounter = JBTextField()

//  @Suppress("unused")
//  val clearCounter = JButton(object : AbstractAction("Clear Token Counter") {
//    override fun actionPerformed(e: ActionEvent) {
//      tokenCounter.text = "0"
//    }
//  })

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
    val smartModel = ComboBox<String>()

    @Suppress("unused")
    @Name("Model")
    val fastModel = ComboBox<String>()

    @Suppress("unused")
    @Name("Enable API Log")
    val apiLog = JBCheckBox()

    @Suppress("unused")
    val openApiLog = JButton(object : AbstractAction("Open API Log") {
        override fun actionPerformed(e: ActionEvent) {
            AppSettingsState.auxiliaryLog?.let {
                if (it.exists()) {
                    val project = ApplicationManager.getApplication().runReadAction<Project> {
                        ProjectManager.getInstance().openProjects.firstOrNull()
                    }
                    ApplicationManager.getApplication().invokeLater {
                        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(it)
                        val openFileDescriptor = OpenFileDescriptor(project, virtualFile!!, virtualFile.length.toInt())
                        FileEditorManager.getInstance(project!!)
                            .openTextEditor(openFileDescriptor, true)?.document?.setReadOnly(
                                true
                            )
                    }
                }
            }
        }
    })


    @Suppress("unused")
    val clearApiLog = JButton(object : AbstractAction("Clear API Log") {
        override fun actionPerformed(e: ActionEvent) {
            val openAIClient = IdeaOpenAIClient.instance
            openAIClient.logStreams.retainAll { it.close(); false }
            AppSettingsState.auxiliaryLog?.let {
                if (it.exists()) {
                    it.delete()
                }
                openAIClient.logStreams.add(FileOutputStream(it, true).buffered())
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
    @Name("Plugin Home")
    val pluginHome = JBTextField()

    @Suppress("unused")
    val choosePluginHome = com.intellij.openapi.ui.TextFieldWithBrowseButton(pluginHome).apply {
        addBrowseFolderListener(
            "Select Plugin Home Directory",
            null,
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }

    @Suppress("unused")
    @Name("Show Welcome Screen")
    val showWelcomeScreen = JBCheckBox("Show Welcome Screen", true)

    @Suppress("unused")
    @Name("Temperature")
    val temperature = JBTextField()

    @Name("APIs")
    val apis = JBTable(DefaultTableModel(arrayOf("Provider", "Key", "Base URL"), 0)).apply {
        columnModel.getColumn(0).preferredWidth = 100
        columnModel.getColumn(1).preferredWidth = 200
        columnModel.getColumn(2).preferredWidth = 200
        val keyColumnIndex = 1
        columnModel.getColumn(keyColumnIndex).cellRenderer = object : DefaultTableCellRenderer() {
            override fun setValue(value: Any?) {
                text =
                    if (value is String && value.isNotEmpty()) value.map { '*' }.joinToString("") else value?.toString()
                        ?: ""
            }
        }
    }

    @Name("Editor Actions")
    var usage = UsageTable(ApplicationServices.usageManager)

    init {
//    tokenCounter.isEditable = false
//    this.modelName.addItem(ChatModels.GPT35Turbo.modelName)
//    this.modelName.addItem(ChatModels.GPT4.modelName)
//    this.modelName.addItem(ChatModels.GPT4Turbo.modelName)
        ChatModels.values().forEach {
//      this.modelName.addItem(it.key)
            this.smartModel.addItem(it.value.modelName)
            this.fastModel.addItem(it.value.modelName)
        }
//    this.modelName.isEditable = true
        this.smartModel.isEditable = true
        this.fastModel.isEditable = true
    }

    companion object;

    override fun dispose() {
    }
}