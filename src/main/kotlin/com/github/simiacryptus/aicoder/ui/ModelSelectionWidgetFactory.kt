package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
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

class ModelSelectionWidgetFactory : StatusBarWidgetFactory {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ModelSelectionWidgetFactory::class.java)
    }

    class ModelSelectionWidget : StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {

        private var statusBar: StatusBar? = null
        private var activeModel: String = AppSettingsState.instance.defaultChatModel().modelName
        val models = ChatModels.values().filter { it.value.provider.name.contains(AppSettingsState.instance.apiProvider) }.map { it.value }.toList()
        override fun ID(): String {
            return "ModelSelectionComponent"
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
            return "Current active model"
        }

        override fun getSelectedValue(): String {
            return activeModel
        }

        private fun getRenderer(): ListCellRenderer<in String> = object : SimpleListCellRenderer<String>() {
            override fun customize(list: JList<out String>, value: String?, index: Int, selected: Boolean, hasFocus: Boolean) {
                text = value // Here you can add more customization if needed
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

            val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, inputField)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .createPopup()

            list.addListSelectionListener {
                val selectedValue = list.selectedValue
                activeModel = selectedValue
                AppSettingsState.instance.modelName = selectedValue
                statusBar?.updateWidget(ID())
                popup.closeOk(null)
            }

            inputField.addActionListener {
                val inputValue = inputField.text
                if (inputValue.isNotEmpty()) {
                    activeModel = inputValue
                    AppSettingsState.instance.modelName = inputValue
                    statusBar?.updateWidget(ID())
                    popup.closeOk(null)
                }
            }

            return popup
        }
    }

    override fun getId(): String {
        return "ModelSelectionComponent"
    }

    override fun getDisplayName(): String {
        return "Model Selector"
    }

    override fun createWidget(project: Project, scope: CoroutineScope): StatusBarWidget {
        return ModelSelectionWidget()
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return ModelSelectionWidget()
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}
