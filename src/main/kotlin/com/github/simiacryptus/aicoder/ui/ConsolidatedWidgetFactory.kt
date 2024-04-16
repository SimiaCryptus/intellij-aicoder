package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
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
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import javax.swing.*

class ConsolidatedWidgetFactory : StatusBarWidgetFactory {

    class ConsolidatedWidget : StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {

        private var statusBar: StatusBar? = null
        private var activeModel: String = AppSettingsState.instance.smartModel.chatModel().modelName
        var models: List<ChatModels> = models()
        private val temperatureSlider by lazy {
            val slider = JSlider(0, 100, (AppSettingsState.instance.temperature * 100).toInt())
            slider.addChangeListener { AppSettingsState.instance.temperature = slider.value / 100.0 }
            slider
        }

        init {
            AppSettingsState.instance.addOnSettingsLoadedListener {
                models = models()
                statusBar?.updateWidget(ID())
            }
        }

        fun models() = ChatModels.values().filter {
            AppSettingsState.instance.apiKey?.filter { it.value.isNotBlank() }?.keys?.contains(it.value.provider.name)
                ?: false
        }.map { it.value }.toList()

        override fun ID(): String {
            return "ConsolidatedComponent"
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

        override fun getSelectedValue(): String {
            return activeModel
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
                    val model = models.find { it.modelName == value }
                    text = "${model?.provider?.name} - $value"
                }
            }
        }

        override fun getPopup(): JBPopup {
            val inputField = JTextField()
            val listModel = CollectionListModel(models.map { it.modelName })
            val list = JBList(listModel)
            list.cellRenderer = getRenderer()

            val panel = JPanel(BorderLayout())
            panel.add(inputField, BorderLayout.NORTH)
            panel.add(JScrollPane(list), BorderLayout.CENTER)
            panel.add(temperatureSlider, BorderLayout.SOUTH)

            val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, inputField)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .createPopup()

            list.addListSelectionListener {
                val selectedValue = list.selectedValue
                activeModel = selectedValue
                AppSettingsState.instance.smartModel = selectedValue
                statusBar?.updateWidget(ID())
                popup.closeOk(null)
            }

            inputField.addActionListener {
                val inputValue = inputField.text
                if (inputValue.isNotEmpty()) {
                    activeModel = inputValue
                    AppSettingsState.instance.smartModel = inputValue
                    statusBar?.updateWidget(ID())
                    popup.closeOk(null)
                }
            }

            return popup
        }
    }

    override fun getId(): String {
        return "ConsolidatedComponent"
    }

    override fun getDisplayName(): String {
        return "Consolidated Widget"
    }

    override fun createWidget(project: Project, scope: CoroutineScope): StatusBarWidget {
        return ConsolidatedWidget()
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return ConsolidatedWidget()
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}