package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.jopenai.OpenAIClient
import org.slf4j.LoggerFactory
import javax.swing.Icon

abstract class BaseAction(
    name: String? = null,
    description: String? = null,
    icon: Icon? = null,
) : AnAction(name, description, icon) {

    private val log by lazy { LoggerFactory.getLogger(javaClass) }
    //override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    val api: OpenAIClient
        get() = IdeaOpenAIClient.instance

    final override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isEnabled(event)
        super.update(event)
    }

    abstract fun handle(e: AnActionEvent)


    final override fun actionPerformed(e: AnActionEvent) {
        UITools.logAction(
            """
            |Action: ${javaClass.simpleName}
        """.trimMargin().trim()
        )
        IdeaOpenAIClient.lastEvent = e
        try {
            handle(e)
        } catch (e: Throwable) {
            UITools.error(log, "Error in Action ${javaClass.simpleName}", e)
        }
    }

    open fun isEnabled(event: AnActionEvent): Boolean = true

    companion object {
        val log by lazy { LoggerFactory.getLogger(javaClass) }
        val scheduledPool = java.util.concurrent.Executors.newScheduledThreadPool(1)
    }
}
