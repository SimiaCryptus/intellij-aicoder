package com.simiacryptus.aicoder.config


import aicoder.actions.plan.PlanConfigDialog.Companion.isVisible
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.simiacryptus.aicoder.util.IdeaChatClient
import com.simiacryptus.jopenai.models.APIProvider
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.ImageModels
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.io.FileOutputStream
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class AppSettingsComponent : com.intellij.openapi.Disposable {
  @Suppress("unused")
  @Name("Enable Diff Logging")
  val diffLoggingEnabled = JBCheckBox()

  @Suppress("unused")
  @Name("GitHub Token")
  val githubToken = JBTextField().apply {
    toolTipText = "GitHub Personal Access Token"
    columns = 30
  }

  @Suppress("unused")
  @Name("Google API Key")
  val googleApiKey = JBTextField().apply {
    toolTipText = "Google API Key"
    columns = 30
  }

  @Suppress("unused")
  @Name("Google Search Engine ID")
  val googleSearchEngineId = JBTextField().apply {
    toolTipText = "Google Search Engine ID"
    columns = 30
  }

  @Suppress("unused")
  @Name("AWS Profile")
  val awsProfile = JBTextField().apply {
    toolTipText = "AWS Profile"
    columns = 30
  }

  @Suppress("unused")
  @Name("AWS Region")
  val awsRegion = JBTextField().apply {
    toolTipText = "AWS Region"
    columns = 30
  }

  @Suppress("unused")
  @Name("AWS Bucket")
  val awsBucket = JBTextField().apply {
    toolTipText = "AWS Bucket"
    columns = 30
  }

  @Suppress("unused")
  @Name("Store Metadata")
  val storeMetadata = JTextArea().apply {
    lineWrap = true
    wrapStyleWord = true
  }

  val executablesModel = DefaultListModel<String>().apply {
    AppSettingsState.instance.executables?.forEach { addElement(it) }
  }
  val executablesList = JBList(executablesModel)

  @Suppress("unused")
  @Name("Executables")
  val executablesPanel = JPanel(BorderLayout()).apply {
    val scrollPane = JScrollPane(executablesList)
    scrollPane.preferredSize = Dimension(300, 200)
    add(scrollPane, BorderLayout.CENTER)
    val buttonPanel = JPanel()
    val addButton = JButton("Add")
    val removeButton = JButton("Remove")
    val editButton = JButton("Edit")
    removeButton.isEnabled = false
    editButton.isEnabled = false

    addButton.addActionListener {
      val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
      descriptor.title = "Select Executable"
      FileChooser.chooseFile(descriptor, null, null) { file ->
        val executablePath = file.path
        if (executablePath.isNotBlank() && !executablesModel.contains(executablePath)) {
          executablesModel.addElement(executablePath)
          AppSettingsState.instance.executables?.add(executablePath)
        }
      }
    }
    removeButton.addActionListener {
      val selectedIndices = executablesList.selectedIndices
      for (i in selectedIndices.reversed()) {
        val removed = executablesModel.remove(i)
        AppSettingsState.instance.executables?.remove(removed)
      }
    }
    editButton.addActionListener {
      val selectedIndex = executablesList.selectedIndex
      if (selectedIndex != -1) {
        val currentValue = executablesModel.get(selectedIndex)
        val newValue = JOptionPane.showInputDialog(this, "Edit executable path:", currentValue)
        if (newValue != null && newValue.isNotBlank()) {
          executablesModel.set(selectedIndex, newValue)
          AppSettingsState.instance.executables?.remove(currentValue)
          AppSettingsState.instance.executables?.add(newValue)
        }
      }
    }
    executablesList.addListSelectionListener(object : ListSelectionListener {
      override fun valueChanged(e: ListSelectionEvent?) {
        val hasSelection = executablesList.selectedIndex != -1
        removeButton.isEnabled = hasSelection
        editButton.isEnabled = hasSelection
      }
    })
    buttonPanel.add(addButton)
    buttonPanel.add(removeButton)
    buttonPanel.add(editButton)
    add(buttonPanel, BorderLayout.SOUTH)
    // Enable multiple selection for the list
    executablesList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
  }

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
      val openAIClient = IdeaChatClient.instance
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
  @Name("Disable Auto-Open URLs")
  val disableAutoOpenUrls = JBCheckBox()


  @Suppress("unused")
  @Name("Plugin Home")
  val pluginHome = JBTextField()

  @Suppress("unused")
  val choosePluginHome = com.intellij.openapi.ui.TextFieldWithBrowseButton(pluginHome).apply {
    val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    val browserDescriptor = com.intellij.openapi.ui.ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
      "Select Plugin Home Directory",
      null,
      this,
      null,
      descriptor,
      com.intellij.openapi.ui.TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
    )
    addActionListener(browserDescriptor)
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

  @Name("User-Supplied Models")
  val userSuppliedModels = JBTable(DefaultTableModel(arrayOf("Display Name", "Model ID", "Provider"), 0)).apply {
    columnModel.getColumn(0).preferredWidth = 150
    columnModel.getColumn(1).preferredWidth = 200
    columnModel.getColumn(2).preferredWidth = 100
    columnModel.getColumn(2).cellEditor = DefaultCellEditor(JComboBox(APIProvider.values().map { it.name }.toTypedArray()))
  }
  val addUserModelButton = JButton("Add Model").apply {
    addActionListener {
      (userSuppliedModels.model as DefaultTableModel).addRow(arrayOf("", "", APIProvider.OpenAI))
    }
  }
  val removeUserModelButton = JButton("Remove Model").apply {
    addActionListener {
      if (userSuppliedModels.selectedRow != -1)
        (userSuppliedModels.model as DefaultTableModel).removeRow(userSuppliedModels.selectedRow)
    }
  }


  @Name("Editor Actions")
  var usage = UsageTable(ApplicationServices.usageManager)
  fun getUserSuppliedModels(): List<AppSettingsState.UserSuppliedModel> {
    return (0 until userSuppliedModels.rowCount).map { row ->
      AppSettingsState.UserSuppliedModel(
        userSuppliedModels.getValueAt(row, 0) as String,
        userSuppliedModels.getValueAt(row, 1) as String,
        userSuppliedModels.getValueAt(row, 2) as APIProvider
      )
    }
  }

  fun setUserSuppliedModels(models: List<AppSettingsState.UserSuppliedModel>) {
    val model = userSuppliedModels.model as DefaultTableModel
    model.rowCount = 0
    models.forEach { model.addRow(arrayOf(it.displayName, it.modelId, it.provider)) }
  }

  init {
    // Initialize new fields
    diffLoggingEnabled.isSelected = AppSettingsState.instance.diffLoggingEnabled
    githubToken.text = AppSettingsState.instance.githubToken ?: ""
    googleApiKey.text = AppSettingsState.instance.googleApiKey ?: ""
    googleSearchEngineId.text = AppSettingsState.instance.googleSearchEngineId ?: ""
    awsProfile.text = AppSettingsState.instance.awsProfile ?: ""
    awsRegion.text = AppSettingsState.instance.awsRegion ?: ""
    awsBucket.text = AppSettingsState.instance.awsBucket ?: ""
    disableAutoOpenUrls.isSelected = AppSettingsState.instance.disableAutoOpenUrls
    // Initialize executables list
    setExecutables(AppSettingsState.instance.executables ?: emptySet())
    ChatModel.values()
      .filter {
        AppSettingsState.instance.apiKeys?.filter { it.value.isNotBlank() }?.keys?.contains(it.value.provider.name)
          ?: false
      }
      .forEach {
        this.smartModel.addItem(it.value.modelName)
        this.fastModel.addItem(it.value.modelName)
      }
    ImageModels.values().forEach {
      this.mainImageModel.addItem(it.name)
    }
    // Sort the items in the ComboBoxes
    val smartModelItems = (0 until smartModel.itemCount).map { smartModel.getItemAt(it) }
      .filter { modelItem ->
        isVisible(ChatModel.values().entries.find { it.value.modelName == modelItem }?.value ?: return@filter false)
      }
      .sortedBy { modelItem ->
        val model =
          ChatModel.values().entries.find { it.value.modelName == modelItem }?.value ?: return@sortedBy ""
        "${model.provider.name} - ${model.modelName}"
      }.toList()
    val fastModelItems = (0 until fastModel.itemCount).map { fastModel.getItemAt(it) }
      .filter { modelItem ->
        isVisible(ChatModel.values().entries.find { it.value.modelName == modelItem }?.value ?: return@filter false)
      }
      .sortedBy { modelItem ->
        val model =
          ChatModel.values().entries.find { it.value.modelName == modelItem }?.value ?: return@sortedBy ""
        "${model.provider.name} - ${model.modelName}"
      }.toList()
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
        val model = ChatModel.values().entries.find { it.value.modelName == value }?.value
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

  fun getExecutables(): Set<String> {
    val model =
      ((executablesPanel.getComponent(0) as? JScrollPane)?.viewport?.view as? JList<String>)?.model as? DefaultListModel<String>
    return model?.elements()?.toList()?.toSet() ?: emptySet()
  }

  fun setExecutables(executables: Set<String>) {
    val model =
      ((executablesPanel.getComponent(0) as? JScrollPane)?.viewport?.view as? JList<String>)?.model as? DefaultListModel<String>
    model?.clear()
    executables.forEach { model?.addElement(it) }
  }
}