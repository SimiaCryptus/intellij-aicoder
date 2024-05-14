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
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.ImageModels
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import java.awt.event.ActionEvent
import java.io.FileOutputStream
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class AppSettingsComponent : com.intellij.openapi.Disposable {

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
    @Name("Main Image Model")
    val mainImageModel = ComboBox<String>()

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
    @Name("Shell Command")
    val shellCommand = JBTextField()

    @Suppress("unused")
    @Name("Show Welcome Screen")
    val showWelcomeScreen = JBCheckBox("Show Welcome Screen", true)

    @Suppress("unused")
    @Name("Enable Legacy Actions")
    val enableLegacyActions = JBCheckBox()

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
        ChatModels.values()
            .filter { AppSettingsState.instance.apiKey?.filter { it.value.isNotBlank() }?.keys?.contains(it.value.provider.name) ?: false }
            .forEach {
                this.smartModel.addItem(it.value.modelName)
                this.fastModel.addItem(it.value.modelName)
            }
        ImageModels.values().forEach {
            this.mainImageModel.addItem(it.name)
        }
        // Sort the items in the ComboBoxes
        val smartModelItems = (0 until smartModel.itemCount).map { smartModel.getItemAt(it) }.sortedBy { modelItem ->
            val model = ChatModels.values().entries.find { it.value.modelName == modelItem }?.value ?: return@sortedBy ""
            "${model.provider.name} - ${model.modelName}" }.toList()
        val fastModelItems = (0 until fastModel.itemCount).map { fastModel.getItemAt(it) }.sortedBy { modelItem ->
            val model = ChatModels.values().entries.find { it.value.modelName == modelItem }?.value ?: return@sortedBy ""
            "${model.provider.name} - ${model.modelName}" }.toList()
        smartModel.removeAllItems()
        fastModel.removeAllItems()
        smartModelItems.forEach { smartModel.addItem(it) }
        fastModelItems.forEach { fastModel.addItem(it) }
        this.smartModel.isEditable = true
        this.fastModel.isEditable = true
        this.smartModel.renderer = getModelRenderer()
        this.fastModel.renderer = getModelRenderer()
        this.mainImageModel.isEditable = true
        this.mainImageModel.renderer = getImageModelRenderer()
    }


    override fun dispose() {
    }

    private fun getModelRenderer(): ListCellRenderer<in String> = object : SimpleListCellRenderer<String>() {
        override fun customize(
            list: JList<out String>,
            value: String?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            text = value // Here you can add more customization if needed
            if (value != null) {
                val model = ChatModels.values().entries.find { it.value.modelName == value }?.value
                text = "${model?.provider?.name} - $value"
            }
        }
    }

    private fun getImageModelRenderer(): ListCellRenderer<in String> = object : SimpleListCellRenderer<String>() {
        override fun customize(
            list: JList<out String>,
            value: String?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            text = value // Here you can add more customization if needed
        }
    }
}