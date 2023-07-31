package com.github.simiacryptus.aicoder.ui

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.Panel
import com.intellij.util.Consumer
import com.intellij.util.IconUtil
import com.intellij.webSymbols.webTypes.WebTypesSvgStringIconLoader
import kotlinx.coroutines.CoroutineScope
import java.awt.Point
import java.awt.event.MouseEvent
import java.nio.charset.Charset
import java.util.concurrent.Executors
import javax.swing.Icon
import javax.swing.JSlider
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class TemperatureControlWidgetFactory : StatusBarWidgetFactory {
    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(TemperatureControlWidgetFactory::class.java)
        val pool = Executors.newCachedThreadPool()
    }

    class TemperatureControlWidget : StatusBarWidget, StatusBarWidget.IconPresentation {

        val slider = JSlider(0, 100, (AppSettingsState.instance.temperature * 100).toInt())

        init {
            slider.addChangeListener(object : ChangeListener {
                override fun stateChanged(e: ChangeEvent?) {
                    AppSettingsState.instance.temperature = slider.value / 100.0
                }
            })
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

        override fun getClickConsumer(): com.intellij.util.Consumer<MouseEvent>? {
            return Consumer { event: MouseEvent ->
                val widgetComp = event.component
                if (widgetComp != null) {
                    val modePanel = Panel()
                    modePanel.add(slider)
                    val builder =
                        JBPopupFactory.getInstance().createComponentPopupBuilder(modePanel, null)
                    val popup = builder.createPopup()
                    popup.show(RelativePoint(widgetComp, Point(0, -modePanel.getPreferredSize().height)))
                }
            }
        }

        override fun getIcon(): Icon? =
            IconLoader.findIcon(javaClass.classLoader.getResource("./META-INF/pluginIcon.svg"))

        override fun getPresentation(): StatusBarWidget.WidgetPresentation? {
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
