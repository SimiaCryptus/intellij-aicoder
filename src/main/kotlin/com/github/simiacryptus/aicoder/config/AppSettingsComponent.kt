package com.github.simiacryptus.aicoder.config

import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.simiacryptus.jopenai.models.APIProvider
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import org.slf4j.LoggerFactory
import java.awt.event.ActionEvent
import java.io.FileOutputStream
import javax.swing.AbstractAction
import javax.swing.JButton

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
  val modelName = ComboBox<String>()

  @Suppress("unused")
  @Name("API Provider")
  val apiProvider = ComboBox<String>()

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
            FileEditorManager.getInstance(project!!).openTextEditor(openFileDescriptor, true)?.document?.setReadOnly(
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

  @Name("Editor Actions")
  var usage = UsageTable(ApplicationServices.usageManager)

  init {
//    tokenCounter.isEditable = false
//    this.modelName.addItem(ChatModels.GPT35Turbo.modelName)
//    this.modelName.addItem(ChatModels.GPT4.modelName)
//    this.modelName.addItem(ChatModels.GPT4Turbo.modelName)
    ChatModels.values().forEach { this.modelName.addItem(it.key) }
    this.modelName.isEditable = true
    this.apiProvider.addItem(APIProvider.OpenAI.name)
    this.apiProvider.addItem(APIProvider.Perplexity.name)
    this.apiProvider.addItem(APIProvider.Groq.name)
    this.apiProvider.addItem(APIProvider.ModelsLab.name)
  }

  companion object {
    private val log = LoggerFactory.getLogger(AppSettingsComponent::class.java)
  }

  override fun dispose() {
  }
}