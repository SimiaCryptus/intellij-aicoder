package aicoder.actions.agent

import aicoder.actions.BaseAction
import aicoder.actions.SessionProxyServer
import aicoder.actions.generate.items
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.Name
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.util.FileSelectionUtils.Companion.isLLMIncludableFile
import com.simiacryptus.skyenet.webui.application.AppInfoData
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import javax.swing.*

class DocumentedMassPatchAction : BaseAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  class SettingsUI {
    @Name("Documentation Files")
    val documentationFiles = CheckBoxList<Path>()

    @Name("Code Files")
    val codeFiles = CheckBoxList<Path>()

    @Name("AI Instruction")
    val transformationMessage = JBTextArea(4, 40)

    @Name("Recent Instructions")
    val recentInstructions = JComboBox<String>()

    @Name("Auto Apply")
    val autoApply = JCheckBox("Auto Apply Changes")
  }

  class UserSettings(
    var transformationMessage: String = "Review and update code according to documentation standards",
    var documentationFiles: List<Path> = listOf(),
    var codeFilePaths: List<Path> = listOf(),
    var autoApply: Boolean = false,
  )

  class Settings(
    val settings: UserSettings? = null,
    val project: Project? = null,
  )

  override fun handle(e: AnActionEvent) {
    val project = e.project
    val config = getConfig(project, e)
    if (config == null) return

    val session = Session.newGlobalID()
    SessionProxyServer.metadataStorage.setSessionName(
      null,
      session,
      "${javaClass.simpleName} @ ${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}"
    )
    SessionProxyServer.chats[session] = DocumentedMassPatchServer(
      config = config,
      api = api,
      autoApply = config.settings?.autoApply ?: false
    )
    ApplicationServer.appInfoMap[session] = AppInfoData(
      applicationName = "Documented Code Patch",
      singleInput = true,
      stickyInput = false,
      loadImages = false,
      showMenubar = false
    )

    val server = AppServer.getServer(e.project)
    Thread {
      Thread.sleep(500)
      try {
        val uri = server.server.uri.resolve("/#$session")
        log.info("Opening browser to $uri")
        browse(uri)
      } catch (e: Throwable) {
        log.warn("Error opening browser", e)
      }
    }.start()
  }

  private fun getConfig(project: Project?, e: AnActionEvent): Settings? {
    var root = UITools.getSelectedFolder(e)?.toNioPath()
    val allFiles: List<Path> = root?.let { Files.walk(it).toList() }
      ?: UITools.getSelectedFiles(e).map { it.toNioPath() }
    if (root == null) {
      root = e.project?.basePath?.let { File(it).toPath() }
    }
    val docFiles: Array<Path> = allFiles.filter { it.toString().endsWith(".md") }.toTypedArray()
    val sourceFiles: Array<Path> = allFiles.filter {
      isLLMIncludableFile(it.toFile()) && !it.toString().endsWith(".md")
    }.toTypedArray()

    val settingsUI = SettingsUI().apply {
      documentationFiles.setItems(docFiles.toMutableList()) { path ->
        root?.relativize(path)?.toString() ?: path.toString()
      }
      codeFiles.setItems(sourceFiles.toMutableList()) { path ->
        root?.relativize(path)?.toString() ?: path.toString()
      }

      docFiles.forEach { path ->
        documentationFiles.setItemSelected(path, true)
      }
      sourceFiles.forEach { path ->
        codeFiles.setItemSelected(path, true)
      }
      autoApply.isSelected = false
    }

    val dialog = ConfigDialog(project, settingsUI, "Documented Mass Patch")
    dialog.show()
    if (!dialog.isOK) return null

    return Settings(dialog.userSettings, project)
  }

  class ConfigDialog(project: Project?, private val settingsUI: SettingsUI, title: String) : DialogWrapper(project) {
    val userSettings = UserSettings()

    init {
      this.title = title
      settingsUI.transformationMessage.text = userSettings.transformationMessage
      settingsUI.autoApply.isSelected = userSettings.autoApply
      init()
    }

    override fun createCenterPanel(): JComponent {
      return JPanel(BorderLayout()).apply {
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
          topComponent = JPanel(BorderLayout()).apply {
            add(JLabel("Documentation Files"), BorderLayout.NORTH)
            add(JBScrollPane(settingsUI.documentationFiles), BorderLayout.CENTER)
          }
          bottomComponent = JPanel(BorderLayout()).apply {
            add(JLabel("Code Files"), BorderLayout.NORTH)
            add(JBScrollPane(settingsUI.codeFiles), BorderLayout.CENTER)
          }
          preferredSize = Dimension(400, 500)
        }

        add(splitPane, BorderLayout.CENTER)
        add(JPanel().apply {
          layout = BoxLayout(this, BoxLayout.Y_AXIS)
          add(JLabel("AI Instruction"))
          add(settingsUI.transformationMessage)
          add(Box.createVerticalStrut(10))
          add(settingsUI.autoApply)
        }, BorderLayout.SOUTH)
      }
    }

    override fun doOKAction() {
      super.doOKAction()
      userSettings.apply {
        transformationMessage = settingsUI.transformationMessage.text
        documentationFiles = settingsUI.documentationFiles.items
          .filter { settingsUI.documentationFiles.isItemSelected(it) }
        codeFilePaths = settingsUI.codeFiles.items
          .filter { settingsUI.codeFiles.isItemSelected(it) }
        autoApply = settingsUI.autoApply.isSelected
      }
    }
  }

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

}