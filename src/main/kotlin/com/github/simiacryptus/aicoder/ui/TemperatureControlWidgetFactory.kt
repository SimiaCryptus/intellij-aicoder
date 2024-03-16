package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.Panel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.Consumer
import kotlinx.coroutines.CoroutineScope
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.util.concurrent.Executors
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.JTabbedPane
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener


class TemperatureControlWidgetFactory : StatusBarWidgetFactory {
    companion object {
      val pool = Executors.newCachedThreadPool()
    }

    class TemperatureControlWidget : StatusBarWidget, StatusBarWidget.IconPresentation {

        private val temperatureSlider by lazy {
            val slider = JSlider(0, 100, (AppSettingsState.instance.temperature * 100).toInt())
            slider.addChangeListener(object : ChangeListener {
                override fun stateChanged(e: ChangeEvent?) {
                    AppSettingsState.instance.temperature = slider.value / 100.0
                }
            })
            slider
        }

        override fun ID(): String {
            return "TemperatureControlComponent"
        }

        override fun install(statusBar: StatusBar) {
            // No need to listen to file changes for this widget
        }

        override fun dispose() {
            // No resources to dispose
        }

        override fun getTooltipText(): String {
            return "AI Coding Assistant\nTemp = ${AppSettingsState.instance.temperature}"
        }

        override fun getClickConsumer(): Consumer<MouseEvent> {
            return Consumer { event: MouseEvent ->
                val widgetComp = event.component
                if (widgetComp != null) {
                    val modePanel = Panel()
                    modePanel.layout = VerticalLayout(0)
                    modePanel.add(JBLabel("AI Coding Assistant"))

                    val tabbedPane = JTabbedPane()

                    val tempPanel = JPanel()
                    tempPanel.setLayout(VerticalLayout(0))
                    tempPanel.add(temperatureSlider)
                    tabbedPane.addTab("Temperature", tempPanel)

                    val feedbackPanel = JPanel()
                    feedbackPanel.setLayout(VerticalLayout(5))

                    feedbackPanel.add(
                        link(
                            JBLabel("<html><a href=''>Problem? Request help...</a></html>"),
                            URI("https://github.com/SimiaCryptus/intellij-aicoder/issues")
                        )
                    )
                    feedbackPanel.add(
                        link(
                            JBLabel("<html><a href=''>Love It? Leave us a review!</a></html>"),
                            URI("https://plugins.jetbrains.com/plugin/20724-ai-coding-assistant/reviews")
                        )
                    )
                    tabbedPane.addTab("Feedback", feedbackPanel)

                    modePanel.add(tabbedPane)

                    JBPopupFactory.getInstance().createComponentPopupBuilder(modePanel, null).createPopup()
                        .show(RelativePoint(widgetComp, Point(0, -modePanel.getPreferredSize().height)))
                }
            }
        }

        private fun link(jbLabel: JBLabel, uri: URI): JBLabel {
            jbLabel.setCursor(Cursor(Cursor.HAND_CURSOR))
            jbLabel.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    try {
                        Desktop.getDesktop().browse(uri)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            })
            return jbLabel
        }

        override fun getIcon(): Icon? =
            IconLoader.findIcon(
                url = javaClass.classLoader.getResource("./META-INF/toolbarIcon.svg"),
                storeToCache = true
            )

        override fun getPresentation(): StatusBarWidget.WidgetPresentation {
            return this
        }
    }

    override fun getId(): String {
        return "TemperatureControlComponent"
    }

    override fun getDisplayName(): String {
        return "Temperature Control"
    }

    override fun createWidget(project: Project, scope: CoroutineScope): StatusBarWidget {
        return TemperatureControlWidget()
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return TemperatureControlWidget()
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}
