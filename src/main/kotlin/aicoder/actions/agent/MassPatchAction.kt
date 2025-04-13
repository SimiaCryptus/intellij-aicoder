package aicoder.actions.agent

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.agent.MassPatchAction.Settings
import aicoder.actions.generate.items
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.config.Name
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.diff.AddApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.util.FileSelectionUtils.Companion.isLLMIncludableFile
import com.simiacryptus.skyenet.core.util.IterativePatchUtil.patchFormatPrompt
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.session.getChildClient
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*

class MassPatchAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  private val log = org.slf4j.LoggerFactory.getLogger(MassPatchAction::class.java)

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!super.isEnabled(event)) return false
    UITools.getSelectedFolder(event) ?: UITools.getSelectedFiles(event).let {
      when (it.size) {
        0 -> null
        1 -> null
        else -> it
      }
    } ?: return false
    return true
  }

  class SettingsUI {
    @Name("Files to Process")
    val filesToProcess = CheckBoxList<Path>()

    @Name("AI Instruction")
    val transformationMessage = JBTextArea(4, 40)

    @Name("Recent Instructions")
    val recentInstructions = JComboBox<String>()

    @Name("Auto Apply")
    val autoApply = JCheckBox("Auto Apply Changes")
  }

  class UserSettings(
    var transformationMessage: String = "Review, fix, and improve",
    var filesToProcess: List<Path> = listOf(),
    var autoApply: Boolean = false,
  )

  class Settings(
    val settings: UserSettings? = null,
    val project: Project? = null,
  )

  fun getConfig(project: Project?, e: AnActionEvent): Settings? {
    val root = UITools.getSelectedFolder(e)?.toNioPath()
    val files = Files.walk(root)
      .filter { isLLMIncludableFile(it.toFile()) }
      .toList().filterNotNull().toTypedArray()
    val settingsUI = SettingsUI().apply {
      filesToProcess.setItems(files.toMutableList()) { path ->
        root?.relativize(path)?.toString() ?: path.toString()
      }
      files.forEach { path ->
        filesToProcess.setItemSelected(path, true)
      }
      autoApply.isSelected = false
    }
    val mruPatchInstructions = AppSettingsState.instance.getRecentCommands("PatchInstructions")
    settingsUI.recentInstructions.model = DefaultComboBoxModel(
      mruPatchInstructions.getMostRecent(10).toTypedArray()
    )
    settingsUI.recentInstructions.selectedIndex = -1
    settingsUI.recentInstructions.addActionListener {
      updateUIFromSelection(settingsUI)
    }

    val dialog = ConfigDialog(project, settingsUI, "Mass Patch")
    dialog.show()
    if (!dialog.isOK) return null
    val settings: UserSettings = dialog.userSettings
    settings.filesToProcess = files.filter { path -> settingsUI.filesToProcess.isItemSelected(path) }.toList()
    mruPatchInstructions.addInstructionToHistory(settings.transformationMessage)
    return Settings(settings, project)
  }

  private fun updateUIFromSelection(settingsUI: SettingsUI) {
    val selected = settingsUI.recentInstructions.selectedItem as? String
    if (selected != null) {
      settingsUI.transformationMessage.text = selected
    }
  }


  override fun handle(event: AnActionEvent) {
    try {
      val project = event.project
      val config = getConfig(project, event)
      if (config == null) {
        log.info("Configuration cancelled by user")
        return
      }

      val session = Session.newGlobalID()
      SessionProxyServer.metadataStorage.setSessionName(
        null,
        session,
        "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
      )
      SessionProxyServer.chats[session] = MassPatchServer(config = config!!, api = api, autoApply = config.settings?.autoApply ?: false)
      ApplicationServer.appInfoMap[session] = AppInfoData(
        applicationName = "Code Chat",
        singleInput = true,
        stickyInput = false,
        loadImages = false,
        showMenubar = false
      )

      val server = AppServer.getServer(event.project)
      UITools.runAsync(project, "Opening browser") {
        Thread.sleep(500)
        try {
          val uri = server.server.uri.resolve("/#$session")
          log.info("Opening browser to $uri")
          browse(uri)
        } catch (e: Throwable) {
          log.warn("Error opening browser", e)
          UITools.showErrorDialog("Failed to open browser: ${e.message}", "Error")
        }
      }
    } catch (e: Exception) {
      log.error("Error in mass patch action", e)
      UITools.showErrorDialog(e.message ?: "", "Error")
    }

  }

  class ConfigDialog(project: Project?, private val settingsUI: SettingsUI, title: String) : DialogWrapper(project) {
    val userSettings = UserSettings()

    init {
      this.title = title
// Set the default values for the UI elements from userSettings
      settingsUI.transformationMessage.text = userSettings.transformationMessage
      settingsUI.autoApply.isSelected = userSettings.autoApply
      init()
    }

    override fun createCenterPanel(): JComponent {
      val panel = JPanel(BorderLayout()).apply {
        val filesScrollPane = JBScrollPane(settingsUI.filesToProcess).apply {
          preferredSize = Dimension(400, 300) // Adjust the preferred size as needed
        }
        add(JLabel("Files to Process"), BorderLayout.NORTH)
        add(filesScrollPane, BorderLayout.CENTER) // Make the files list the dominant element

        val optionsPanel = JPanel().apply {
          layout = BoxLayout(this, BoxLayout.Y_AXIS)
          add(JLabel("Recent Instructions"))
          add(settingsUI.recentInstructions)
          add(Box.createVerticalStrut(10))
          add(JLabel("AI Instruction"))
          add(settingsUI.transformationMessage)
          add(Box.createVerticalStrut(10))
          add(settingsUI.autoApply)
        }
        add(optionsPanel, BorderLayout.SOUTH)
      }
      return panel
    }

    override fun doOKAction() {
      super.doOKAction()
      userSettings.transformationMessage = settingsUI.transformationMessage.text
      userSettings.filesToProcess =
        settingsUI.filesToProcess.items.filter { path -> settingsUI.filesToProcess.isItemSelected(path) }
      userSettings.autoApply = settingsUI.autoApply.isSelected
    }
  }
}

class MassPatchServer(
  val config: Settings,
  val api: ChatClient,
  val autoApply: Boolean
) : ApplicationServer(
  applicationName = "Multi-file Patch Chat",
  path = "/patchChat",
  showMenubar = false,
) {
  private val log = org.slf4j.LoggerFactory.getLogger(MassPatchServer::class.java)
  private lateinit var _root: Path


  override val singleInput = false
  override val stickyInput = true
  private val mainActor: SimpleActor
    get() {

      return SimpleActor(
        prompt = buildString {
          append("You are a helpful AI that helps people with coding.\n")
            append(patchFormatPrompt)
          append("\nIf needed, new files can be created by using code blocks labeled with the filename in the same manner.")
        },
        model = AppSettingsState.instance.smartModel.chatModel(),
        temperature = AppSettingsState.instance.temperature,
      )
    }


  override fun newSession(user: User?, session: Session): SocketManager {
    val socketManager = super.newSession(user, session)
    val ui = (socketManager as ApplicationSocketManager).applicationInterface
    try {
      _root = config.project?.basePath?.let { Path.of(it) }
        ?: throw IllegalStateException("Project base path not found")
    } catch (e: Exception) {
      log.error("Failed to initialize root path", e)
      throw e
    }
    val task = ui.newTask(true)
    val api = api.getChildClient(task)
    val tabs = TabbedDisplay(task)
    val userMessage = config.settings?.transformationMessage ?: "Create user documentation"
    val codeFiles = config.settings?.filesToProcess
    codeFiles?.forEach { path ->
      socketManager.scheduledThreadPoolExecutor.schedule({
        socketManager.pool.submit {
          try {
            val codeSummary = listOf(path)
              .filter { isLLMIncludableFile(it.toFile()) }
              .associateWith { it.toFile().readText(Charsets.UTF_8) }
              .entries.joinToString("\n\n") { (path, code) ->
                val extension = path.toString().split('.').lastOrNull()
                "# $path\n```$extension\n$code\n```"
              }
            val fileTask = ui.newTask(false).apply {
              tabs[path.toString()] = placeholder
            }
            val toInput = { it: String -> listOf(codeSummary ?: "", it) }
            Discussable(
              task = fileTask,
              userMessage = { userMessage },
              heading = renderMarkdown(userMessage),
              initialResponse = {
                mainActor.answer(toInput(it), api = api)
              },
              outputFn = { design: String ->
                val markdown = AddApplyFileDiffLinks.instrumentFileDiffs(
                  ui.socketManager!!,
                  root = _root,
                  response = design,
                  handle = { newCodeMap: Map<Path, String> ->
                    newCodeMap.forEach { (path, newCode) ->
                      fileTask.complete("<a href='${"fileIndex/$session/$path"}'>$path</a> Updated")
                    }
                  },
                  ui = ui,
                  api = api as API,
                  shouldAutoApply = { autoApply },
                  model = AppSettingsState.instance.fastModel.chatModel(),
                )
                """<div>${renderMarkdown(markdown)}</div>"""
              },
              ui = ui,
              reviseResponse = { userMessages: List<Pair<String, Role>> ->
                mainActor.respond(
                  messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                    .toTypedArray<ApiModel.ChatMessage>()),
                  input = toInput(userMessage),
                  api = api
                )
              },
              atomicRef = AtomicReference(),
              semaphore = Semaphore(0),
            ).call()
          } catch (e: Exception) {
            log.warn("Error processing $path", e)
            task.error(ui, e)
          }
        }
      }, 10, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
    return socketManager
  }

  companion object {
    val log = org.slf4j.LoggerFactory.getLogger(MassPatchServer::class.java)
  }
}