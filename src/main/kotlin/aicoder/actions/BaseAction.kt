package aicoder.actions

/**
 * Base action class providing common functionality for AI Coder actions.
 * Handles API client initialization and common UI operations.
 */
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.simiacryptus.aicoder.util.IdeaChatClient
import com.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.ChatClient
import org.slf4j.LoggerFactory
import javax.swing.Icon

abstract class BaseAction(
  name: String? = null,
  description: String? = null,
  icon: Icon? = null,
) : AnAction(name, description, icon) {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }
  //override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  /**
   * Primary API client for chat interactions
   */

  val api: ChatClient
    @JvmName("getChatClient") get() = IdeaChatClient.instance
  val api2 = IdeaOpenAIClient.instance

  final override fun update(event: AnActionEvent) {
    val currentThread = Thread.currentThread()
    val scheduledFuture = scheduledPool.schedule({
      if (event.presentation.isEnabledAndVisible) {
        log.warn("Slow update: ${javaClass.simpleName} took too long; ${currentThread.name}\n\t${currentThread.stackTrace.joinToString("\n\t")}")
      }
    }, 1, java.util.concurrent.TimeUnit.SECONDS)
    event.presentation.isEnabledAndVisible = isEnabled(event)
    scheduledFuture.cancel(false)
    super.update(event)
  }

  /**
   * Handle the action event
   * @param e The action event to handle
   */
  abstract fun handle(e: AnActionEvent)

  /** Determines if this action is enabled in the current context */


  final override fun actionPerformed(e: AnActionEvent) {
    UITools.logAction(
      "Action: ${javaClass.simpleName}".trim()
    )
    IdeaChatClient.lastEvent = e
    try {
      handle(e)
    } catch (e: IllegalStateException) {
      UITools.error(log, "Invalid state in Action ${javaClass.simpleName}", e)
    } catch (e: IllegalArgumentException) {
      UITools.error(log, "Invalid input in Action ${javaClass.simpleName}", e)
    } catch (e: Throwable) {
      UITools.error(log, "Unexpected error in Action ${javaClass.simpleName}", e)
    }
  }

  open fun isEnabled(event: AnActionEvent): Boolean = true


  companion object {
    val log by lazy { LoggerFactory.getLogger(javaClass) }
    val scheduledPool = java.util.concurrent.Executors.newScheduledThreadPool(1).apply {
      ApplicationManager.getApplication().executeOnPooledThread {
        Runtime.getRuntime().addShutdownHook(Thread {
          this.shutdown()
          this.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
        })
      }
    }
  }
}
