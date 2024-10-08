package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.BrowseUtil.browse
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.CollectionListModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBList
import com.simiacryptus.jopenai.models.ChatModels
import icons.MyIcons
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*

class SettingsWidgetFactory : StatusBarWidgetFactory {

    class SettingsWidget : StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {

        private var statusBar: StatusBar? = null
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

        init {
            AppSettingsState.instance.addOnSettingsLoadedListener {
                statusBar?.updateWidget(ID())
            }
        }

        fun models() = ChatModels.values().filter { isVisible(it.value) }
            .entries.sortedBy { "${it.value.provider.name} - ${it.value.modelName}" }.map { it.value }.toList()

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

        override fun getTooltipText(): String {
            return "Current active model and temperature control"
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
                        URI("https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant/edit/reviews"))
                })
            }, BorderLayout.EAST)
            return header
        }

        override fun getSelectedValue(): String {
            return AppSettingsState.instance.smartModel
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
                    val model = models().find { it.modelName == value }
                    text = "<html><b>${model?.provider?.name}</b> - <i>$value</i></html>" // Enhance label formatting
                }
            }
        }

        override fun getPopup(): JBPopup {
            val listModel = CollectionListModel(models().map { it.modelName })
            val list = JBList(listModel)
            list.cellRenderer = getRenderer()
            list.visibleRowCount = 20

            val panel = JPanel(BorderLayout())
            panel.add(createHeader(), BorderLayout.NORTH)
            panel.add(JScrollPane(list), BorderLayout.CENTER)
            panel.add(temperatureSlider, BorderLayout.SOUTH)

            val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, list)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .createPopup()

            list.addListSelectionListener {
                val selectedValue = list.selectedValue
                AppSettingsState.instance.smartModel = selectedValue
                statusBar?.updateWidget(ID())
                popup.closeOk(null)
            }

            return popup
        }

        companion object {
            fun isVisible(it: ChatModels): Boolean {
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

    override fun createWidget(project: Project, scope: CoroutineScope): StatusBarWidget {
        return SettingsWidget()
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