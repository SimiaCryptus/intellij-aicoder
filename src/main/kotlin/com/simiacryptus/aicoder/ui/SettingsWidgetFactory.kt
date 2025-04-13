package com.simiacryptus.aicoder.ui

import aicoder.actions.SessionProxyServer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.treeStructure.Tree
import com.simiacryptus.aicoder.AppServer
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.config.UsageTable
import com.simiacryptus.aicoder.util.BrowseUtil.browse
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.ApplicationServicesConfig.dataStorageRoot
import icons.MyIcons
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.util.*
import javax.accessibility.AccessibleContext
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class SettingsWidgetFactory : StatusBarWidgetFactory {

  class SettingsWidget : StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {

    private var statusBar: StatusBar? = null
    private val smartModelTree by lazy { createModelTree("Smart Model", AppSettingsState.instance.smartModel) }
    private val fastModelTree by lazy { createModelTree("Fast Model", AppSettingsState.instance.fastModel) }
    private var project: Project? = null
    private val sessionsList = JBList<Session>()
    private val sessionsListModel = DefaultListModel<Session>()
    private fun createModelTree(title: String, selectedModel: String?): Tree {
      val root = DefaultMutableTreeNode(title)
      // Filter models by providers that have API keys set
      val providers = models()
        .filter { model ->
          val providerName = model.second.provider.name
          AppSettingsState.instance.apiKeys?.get(providerName)?.isNotEmpty() == true
        }
        .groupBy { it.second.provider }

      for ((provider, models) in providers) {
        val providerNode = DefaultMutableTreeNode(provider.name)
        for (model in models) {
          val modelNode = DefaultMutableTreeNode(model.second.modelName)
          providerNode.add(modelNode)
        }
        // Only add provider node if it has models
        if (providerNode.childCount > 0) {
          root.add(providerNode)
        }
      }
      val treeModel = DefaultTreeModel(root)
      val tree = Tree(treeModel)
      // Add accessibility description
      tree.accessibleContext.accessibleDescription = getMessage("tree.description", title)
      // Add keyboard navigation
      tree.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle")
      tree.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "select")
      // Add screen reader support for selection changes
      tree.addTreeSelectionListener {
        val selectedNode = tree.lastSelectedPathComponent?.toString()
        if (selectedNode != null) {
          tree.accessibleContext.firePropertyChange(
            AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY,
            null,
            getMessage("tree.selected", selectedNode)
          )
        }
      }
      tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
      tree.isRootVisible = false
      tree.showsRootHandles = true
      tree.addTreeSelectionListener {
        val selectedPath = tree.selectionPath
        if (selectedPath != null && selectedPath.pathCount == 3) { // Provider -> Model
          val modelName = selectedPath.lastPathComponent.toString()
          when (title) {
            "Smart Model" -> AppSettingsState.instance.smartModel = modelName
            "Fast Model" -> AppSettingsState.instance.fastModel = modelName
          }
          statusBar?.updateWidget(ID())
        }
      }
      // Expand and select the node if a model is selected
      if (selectedModel != null) {
        SwingUtilities.invokeLater {
          setSelectedModel(tree, selectedModel)
        }
      }
      return tree
    }

    private val temperatureSlider by lazy {
      val slider = JSlider(0, 100, (AppSettingsState.instance.temperature * 100).toInt())
      // Add accessibility description
      slider.accessibleContext.accessibleDescription = getMessage("slider.description")
      // Add keyboard increments
      slider.majorTickSpacing = 10
      slider.minorTickSpacing = 1
      slider.snapToTicks = true
      val panel = JPanel(BorderLayout(5, 5)) // Add padding

      // Add reasoning effort dropdown
      val reasoningPanel = JPanel(FlowLayout(FlowLayout.LEFT))
      val reasoningLabel = JLabel(getMessage("label.reasoningEffort"))
      val reasoningCombo = JComboBox(arrayOf("Low", "Medium", "High"))
      reasoningCombo.selectedItem = AppSettingsState.instance.reasoningEffort
      reasoningCombo.addActionListener {
        AppSettingsState.instance.reasoningEffort = reasoningCombo.selectedItem as String
      }
      reasoningPanel.add(reasoningLabel)
      reasoningPanel.add(reasoningCombo)

      val label = JLabel(String.format("%.2f", AppSettingsState.instance.temperature))
      label.accessibleContext.accessibleDescription = getMessage("label.temperature")
      slider.addChangeListener {
        slider.accessibleContext.firePropertyChange(
          AccessibleContext.ACCESSIBLE_VALUE_PROPERTY,
          null,
          getMessage("slider.value", slider.value / 100.0)
        )
        AppSettingsState.instance.temperature = slider.value / 100.0
        label.text = String.format("%.2f", slider.value / 100.0)
      }

      panel.add(slider, BorderLayout.CENTER)
      panel.add(reasoningPanel, BorderLayout.SOUTH)
      panel.add(label, BorderLayout.EAST)
      panel
    }

    private fun createServerControlPanel(): JPanel {
      val panel = JPanel(BorderLayout())
      panel.accessibleContext.accessibleDescription = getMessage("panel.server.description")
      sessionsList.accessibleContext.accessibleDescription = getMessage("list.sessions.description")
      sessionsList.accessibleContext.accessibleName = getMessage("list.sessions.name")
      // Add keyboard navigation for sessions list
      sessionsList.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate")
      sessionsList.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "activate")
      // Server control buttons
      val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
      val startButton = JButton(getMessage("server.start"))
      val stopButton = JButton(getMessage("server.stop"))
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
      sessionPanel.add(JLabel(getMessage("label.activeSessions")), BorderLayout.NORTH)
      sessionPanel.add(JScrollPane(sessionsList), BorderLayout.CENTER)
      // Action buttons for sessions
      val actionPanel = JPanel(GridLayout(1, 2))
      val copyButton = JButton(getMessage("action.copyLink"))
      val openButton = JButton(getMessage("action.openLink"))
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
      (SessionProxyServer.chats.keys + SessionProxyServer.agents.keys).distinct().forEach {
        sessionsListModel.addElement(it)
      }
    }

    private inner class SessionListRenderer : ListCellRenderer<Session> {
      private val label = JLabel()
      override fun getListCellRendererComponent(
        list: JList<out Session>?,
        value: Session?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        label.text = if (value != null) {
          try {
            val sessionName = ApplicationServices.metadataStorageFactory(dataStorageRoot).getSessionName(null, value)
            when {
              sessionName.isNullOrBlank() -> getDefaultSessionLabel(value)
              else -> "$sessionName (${value.sessionId.take(8)})"
            }
          } catch (e: Exception) {
            getDefaultSessionLabel(value)
          }
        } else {
          "Unknown Session"
        }

        if (isSelected) {
          label.background = list?.selectionBackground
          label.foreground = list?.selectionForeground
        } else {
          label.background = list?.background
          label.foreground = list?.foreground
        }
        // Add accessibility name to each list item
        label.accessibleContext.accessibleName = label.text
        label.accessibleContext.accessibleDescription = getMessage("session.item.description", label.text)
        return label
      }

      private fun getDefaultSessionLabel(session: Session): String {
        return "Session ${session.sessionId.take(8)}"
      }
    }


    init {
      AppSettingsState.instance.onSettingsLoadedListeners.add {
        statusBar?.updateWidget(ID())
      }
      // Initialize selection for both trees on EDT
      if (AppSettingsState.instance.smartModel.isNotEmpty()) {
        SwingUtilities.invokeLater {
          setSelectedModel(smartModelTree, AppSettingsState.instance.smartModel)
        }
      }
      if (AppSettingsState.instance.fastModel.isNotEmpty()) {
        SwingUtilities.invokeLater {
          setSelectedModel(fastModelTree, AppSettingsState.instance.fastModel)
        }
      }
    }

    fun models() = ChatModel.values().filter { it.value != null && isVisible(it.value!!) }.toList()
      .sortedBy { "${it.second.provider.name} - ${it.second.modelName}" }

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
      // Previously commented out: connection?.disconnect()
    }

    private fun createHeader(): JPanel {
      val appname = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
      appname.add(JLabel("AI Coding Assistant"), FlowLayout.LEFT)
      appname.add(JLabel(MyIcons.icon), FlowLayout.LEFT)

      val header = JPanel(BorderLayout())
      header.add(appname, BorderLayout.WEST)
      header.add(JLabel(String.format("<html><a href=\"\">%s</a></html>", getMessage("header.rateUs"))).apply {
        cursor = Cursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
          override fun mouseClicked(e: MouseEvent) = browse(
            URI("https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant/edit/reviews")
          )
        })
      }, BorderLayout.EAST)
      return header
    }

    private fun setSelectedModel(tree: JTree, modelName: String) {
      val root = tree.model as DefaultTreeModel
      val rootNode = root.root as DefaultMutableTreeNode
      for (i in 0 until rootNode.childCount) {
        val providerNode = rootNode.getChildAt(i) as DefaultMutableTreeNode
        for (j in 0 until providerNode.childCount) {
          val modelNode = providerNode.getChildAt(j) as DefaultMutableTreeNode
          if (modelNode.userObject == modelName) {
            val path = TreePath(modelNode.path)
            tree.selectionPath = path
            tree.scrollPathToVisible(path)
            break
          }
        }
      }
    }


    override fun getPopup(): JBPopup {
      // Update sessions list before creating popup
      updateSessionsList()

      val panel = JPanel(BorderLayout())
      panel.accessibleContext.accessibleDescription = getMessage("popup.description")
      panel.add(createHeader(), BorderLayout.NORTH)
      // Create tabbed pane
      val tabbedPane = JTabbedPane()
      // Add accessibility descriptions for tabs
      tabbedPane.accessibleContext.accessibleDescription = getMessage("tabs.description")
      // Smart model tab
      val smartModelPanel = JPanel(BorderLayout())
      smartModelPanel.add(JScrollPane(smartModelTree), BorderLayout.CENTER)
      // Fast model tab
      val fastModelPanel = JPanel(BorderLayout())
      fastModelPanel.add(JScrollPane(fastModelTree), BorderLayout.CENTER)
      // Add usage tab
      val usagePanel = JPanel(BorderLayout())
      usagePanel.add(UsageTable(ApplicationServices.usageManager), BorderLayout.CENTER)
      // Add server control tab
      tabbedPane.addTab(getMessage("tab.smartModel"), smartModelPanel)
      tabbedPane.addTab(getMessage("tab.fastModel"), fastModelPanel)
      tabbedPane.addTab(getMessage("tab.server"), createServerControlPanel())
      tabbedPane.addTab(getMessage("tab.usage"), usagePanel)

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
      return popup
    }

    override fun getSelectedValue(): String {
      return AppSettingsState.instance.smartModel
    }

    override fun getTooltipText(): String {
      val serverStatus = if (AppServer.isRunning()) {
        "Server running on ${AppSettingsState.instance.listeningEndpoint}:${AppSettingsState.instance.listeningPort}"
      } else {
        "Server stopped"
      }
      return """
        Smart Model: ${AppSettingsState.instance.smartModel}<br/>
        Fast Model: ${AppSettingsState.instance.fastModel}<br/>
        Temperature: ${AppSettingsState.instance.temperature}<br/>
        $serverStatus
        """.trimIndent().trim()
    }

    private fun isVisible(it: ChatModel): Boolean {
      return true
    }

    companion object {
      private val copyLinkURI = "action.openLinkURI"
      private val messages = ResourceBundle.getBundle("messages.SettingsWidget")
      private fun getMessage(key: String, vararg args: Any): String =
        String.format(messages.getString(key), *args)

      fun getSessionLink(session: Session) =
        "http://${AppSettingsState.instance.listeningEndpoint}:${AppSettingsState.instance.listeningPort}/#${session.sessionId}"
    }

  }

  override fun getId(): String {
    return "AICodingAssistant.SettingsWidgetFactory"
  }

  override fun getDisplayName(): String {
    return "AI Coding Assistant Settings"
  }


  override fun createWidget(project: Project): StatusBarWidget {
    return SettingsWidget()
  }

  override fun isAvailable(project: Project): Boolean {
    return true
  }

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
    return true
  }
}