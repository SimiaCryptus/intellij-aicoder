package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.AppServer
import com.github.simiacryptus.aicoder.actions.generic.SessionProxyServer
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.CollectionListModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBList
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.core.platform.Session
import icons.MyIcons
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*

class SettingsWidgetFactory : StatusBarWidgetFactory {

  class SettingsWidget : StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {

    private var statusBar: StatusBar? = null
    private val smartModelList = createModelList()
    private val fastModelList = createModelList()
    private var project: Project? = null
    private val sessionsList = JList<Session>()
    private val sessionsListModel = DefaultListModel<Session>()
    private val temperatureSlider by lazy {
      val slider = JSlider(0, 100, (AppSettingsState.instance.temperature * 100).toInt())
      slider.addChangeListener { AppSettingsState.instance.temperature = slider.value / 100.0 }
      val panel = JPanel(BorderLayout(5, 5)) // Add padding
      panel.add(slider, BorderLayout.CENTER)
      val label = JLabel(String.format("%.2f", AppSettingsState.instance.temperature))
      slider.addChangeListener { label.text = String.format("%.2f", slider.value / 100.0) }
      panel.add(label, BorderLayout.EAST)
      panel
    }

    private fun createServerControlPanel(): JPanel {
      val panel = JPanel(BorderLayout())
      // Server control buttons
      val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
      val startButton = JButton("Start Server")
      val stopButton = JButton("Stop Server")
      // Set initial button states
      startButton.isEnabled = !AppServer.isRunning()
      stopButton.isEnabled = AppServer.isRunning()

      startButton.addActionListener {
        AppServer.getServer(project)
        startButton.isEnabled = false
        stopButton.isEnabled = true
        updateSessionsList()
      }
      stopButton.addActionListener {
        AppServer.getServer(project).server.stop()
        startButton.isEnabled = true
        stopButton.isEnabled = false
        updateSessionsList()
      }
      buttonPanel.add(startButton)
      buttonPanel.add(stopButton)
      panel.add(buttonPanel, BorderLayout.NORTH)
      // Sessions list
      sessionsList.model = sessionsListModel
      sessionsList.cellRenderer = SessionListRenderer()
      val sessionPanel = JPanel(BorderLayout())
      sessionPanel.add(JLabel("Active Sessions:"), BorderLayout.NORTH)
      sessionPanel.add(JScrollPane(sessionsList), BorderLayout.CENTER)
      // Action buttons for sessions
      val actionPanel = JPanel(GridLayout(1, 2))
      val copyButton = JButton("Copy Link")
      val openButton = JButton("Open Link")
      // Set initial button states for session actions
      copyButton.isEnabled = false
      openButton.isEnabled = false
      // Add selection listener to enable/disable action buttons
      sessionsList.addListSelectionListener {
        val hasSelection = sessionsList.selectedValue != null
        copyButton.isEnabled = hasSelection
        openButton.isEnabled = hasSelection
      }

      copyButton.addActionListener {
        val session = sessionsList.selectedValue
        if (session != null) {
          val link = getSessionLink(session)
          val selection = StringSelection(link)
          Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
        }
      }
      openButton.addActionListener {
        val session = sessionsList.selectedValue
        if (session != null) {
          browse(URI(getSessionLink(session)))
        }
      }
      actionPanel.add(copyButton)
      actionPanel.add(openButton)
      sessionPanel.add(actionPanel, BorderLayout.SOUTH)
      panel.add(sessionPanel, BorderLayout.CENTER)
      return panel
    }

    fun updateSessionsList() {
      sessionsListModel.clear()
      SessionProxyServer.chats.keys.forEach { sessionsListModel.addElement(it) }
      SessionProxyServer.agents.keys.forEach { sessionsListModel.addElement(it) }
    }

    private fun getSessionLink(session: Session): String {
      val settings = AppSettingsState.instance
      return "http://${settings.listeningEndpoint}:${settings.listeningPort}/?session=${session.sessionId}"
    }

    private class SessionListRenderer : ListCellRenderer<Session> {
      private val label = JLabel()
      override fun getListCellRendererComponent(
        list: JList<out Session>?,
        value: Session?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        label.text = "Session ${value?.sessionId?.take(8)}"
        if (isSelected) {
          label.background = list?.selectionBackground
          label.foreground = list?.selectionForeground
        } else {
          label.background = list?.background
          label.foreground = list?.foreground
        }
        return label
      }
    }

    private fun createModelList(): JBList<String> {
      val list = JBList(CollectionListModel(models().map { it?.modelName ?: "" }))
      list.cellRenderer = getRenderer()
      list.visibleRowCount = 20
      return list
    }


    init {
      AppSettingsState.instance.addOnSettingsLoadedListener {
        statusBar?.updateWidget(ID())
      }
      // Initialize selection for both lists
      smartModelList.setSelectedValue(AppSettingsState.instance.smartModel, true)
      fastModelList.setSelectedValue(AppSettingsState.instance.fastModel, true)
    }

    fun models() = ChatModel.values().filter { it.value != null && isVisible(it.value!!) }
      .entries.sortedBy { "${it.value!!.provider.name} - ${it.value!!.modelName}" }.map { it.value }.toList()

    override fun ID(): String {
      return "AICodingAssistant.SettingsWidget"
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
      return this
    }

    override fun install(statusBar: StatusBar) {
      this.statusBar = statusBar
    }

    override fun dispose() {
      //connection?.disconnect()
    }

    private fun createHeader(): JPanel {
      val appname = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
      appname.add(JLabel("AI Coding Assistant"), FlowLayout.LEFT)
      appname.add(JLabel(MyIcons.icon), FlowLayout.LEFT)

      val header = JPanel(BorderLayout())
      header.add(appname, BorderLayout.WEST)
      header.add(JLabel("<html><a href=\"\">Rate Us!</a></html>").apply {
        cursor = Cursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
          override fun mouseClicked(e: MouseEvent) = browse(
            URI("https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant/edit/reviews")
          )
        })
      }, BorderLayout.EAST)
      return header
    }

    private fun getRenderer(): ListCellRenderer<in String> = object : SimpleListCellRenderer<String>() {
      override fun customize(
        list: JList<out String>,
        value: String?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
      ) {
        text = value // Here you can add more customization if needed
        if (value != null) {
          val model = models().find { it?.modelName == value }
          text = "<html><b>${model?.provider?.name}</b> - <i>$value</i></html>" // Enhance label formatting
        }
      }
    }

    override fun getPopup(): JBPopup {

      val panel = JPanel(BorderLayout())
      panel.add(createHeader(), BorderLayout.NORTH)
      // Create tabbed pane
      val tabbedPane = JTabbedPane()
      // Smart model tab
      val smartModelPanel = JPanel(BorderLayout())
      smartModelPanel.add(JScrollPane(smartModelList), BorderLayout.CENTER)
      tabbedPane.addTab("Smart Model", smartModelPanel)
      // Fast model tab
      val fastModelPanel = JPanel(BorderLayout())
      fastModelPanel.add(JScrollPane(fastModelList), BorderLayout.CENTER)
      tabbedPane.addTab("Fast Model", fastModelPanel)
      // Add server control tab
      tabbedPane.addTab("Server", createServerControlPanel())

      panel.add(tabbedPane, BorderLayout.CENTER)
      panel.add(temperatureSlider, BorderLayout.SOUTH)

      val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, tabbedPane)
        .setRequestFocus(true)
        .setCancelOnClickOutside(true)
        .createPopup()
      popup.addListener(object : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
          updateSessionsList()
        }
      })

      smartModelList.addListSelectionListener {
        val selectedValue = smartModelList.selectedValue
        if (selectedValue != null) {
          AppSettingsState.instance.smartModel = selectedValue
          statusBar?.updateWidget(ID())
        }
      }

      fastModelList.addListSelectionListener {
        val selectedValue = fastModelList.selectedValue
        if (selectedValue != null) {
          AppSettingsState.instance.fastModel = selectedValue
          statusBar?.updateWidget(ID())
        }
      }
      return popup
    }

    override fun getSelectedValue(): String {
//      return "${AppSettingsState.instance.smartModel} / ${AppSettingsState.instance.fastModel}"
      return "${AppSettingsState.instance.smartModel}"
    }

    override fun getTooltipText(): String {
      val serverStatus = if (AppServer.isRunning()) {
        "Server running on ${AppSettingsState.instance.listeningEndpoint}:${AppSettingsState.instance.listeningPort}"
      } else {
        "Server stopped"
      }
      return "Smart Model: ${AppSettingsState.instance.smartModel}\n" +
          "Fast Model: ${AppSettingsState.instance.fastModel}\n" +
          "Temperature: ${AppSettingsState.instance.temperature}\n" +
          serverStatus
    }

    companion object {
      fun isVisible(it: ChatModel): Boolean {
        val hasApiKey =
          AppSettingsState.instance.apiKey?.filter { it.value.isNotBlank() }?.keys?.contains(it.provider.name)
        return false != hasApiKey
      }
    }

  }

  override fun getId(): String {
    return "AICodingAssistant.SettingsWidgetFactory"
  }

  override fun getDisplayName(): String {
    return "AI Coding Assistant Settings"
  }

  companion object {
    val settingsWidget = SettingsWidget()
  }

  override fun createWidget(project: Project, scope: CoroutineScope): StatusBarWidget {
    return settingsWidget
  }

  override fun createWidget(project: Project): StatusBarWidget {
    return settingsWidget
  }

  override fun isAvailable(project: Project): Boolean {
    return true
  }

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
    return true
  }
}