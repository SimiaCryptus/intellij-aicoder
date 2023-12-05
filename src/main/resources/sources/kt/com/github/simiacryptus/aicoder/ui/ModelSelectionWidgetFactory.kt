package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.CollectionListModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.popup.list.ComboBoxPopup
import com.simiacryptus.jopenai.models.ChatModels
import kotlinx.coroutines.CoroutineScope
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.ListModel

class ModelSelectionWidgetFactory : StatusBarWidgetFactory {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ModelSelectionWidgetFactory::class.java)
    }

    class ModelSelectionWidget : StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {

        private var statusBar: StatusBar? = null
        private var activeModel: String = AppSettingsState.instance.defaultChatModel().modelName
        val models = listOf(
            ChatModels.GPT4Turbo,
            ChatModels.GPT4,
            ChatModels.GPT35Turbo,
        )

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

        override fun getPopup(): JBPopup {
            val context = object : ComboBoxPopup.Context<String> {
                override fun getProject(): Project? {
                    return null
                }

                override fun getModel(): ListModel<String> = CollectionListModel(models.map { it.modelName })

                override fun getRenderer(): ListCellRenderer<in String> {
                    return object : SimpleListCellRenderer<String>() {
                        override fun customize(
                            list: JList<out String>,
                            value: String?,
                            index: Int,
                            selected: Boolean,
                            hasFocus: Boolean
                        ) {
                            text = value
                        }

                    }
                }
            }
            return ComboBoxPopup(context, activeModel, { str ->
                activeModel = str
                AppSettingsState.instance.modelName = str
                statusBar?.updateWidget(ID())
            })
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
