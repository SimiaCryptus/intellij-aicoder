
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent

class AIAssistantWidgetFactory : StatusBarWidgetFactory {
     override fun getId(): String = "com.example.AIAssistantWidget"
     override fun getDisplayName(): String = "AI Assistant"
     override fun isAvailable(project: Project): Boolean = true
     override fun createWidget(project: Project): StatusBarWidget = AIAssistantWidget()
     override fun disposeWidget(widget: StatusBarWidget) {
         widget.dispose()
     }
     override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

     inner class AIAssistantWidget : StatusBarWidget, StatusBarWidget.TextPresentation {
         override fun ID(): String = "AIAssistantWidget"
         override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? = this
         override fun getTooltipText(): String = "AI Assistant"
         override fun getClickConsumer(): Consumer<MouseEvent>? = null
         override fun getText(): String = "AI Assistant: Ready"
         override fun getAlignment(): Float = Component.CENTER_ALIGNMENT
         override fun dispose() {}
     }
 }
