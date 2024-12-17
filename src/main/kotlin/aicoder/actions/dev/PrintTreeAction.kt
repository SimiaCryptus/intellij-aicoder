package aicoder.actions.dev

import aicoder.actions.BaseAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.aicoder.util.psi.PsiUtil
import org.slf4j.LoggerFactory

/**
 * The PrintTreeAction class is an IntelliJ action that enables developers to print the tree structure of a PsiFile.
 * To use this action, first make sure that the "devActions" setting is enabled.
 * Then, open the file you want to print the tree structure of.
 * Finally, select the "PrintTreeAction" action from the editor context menu.
 * This will print the tree structure of the file to the log.
 *
 * @property log Logger instance for this class
 * @see BaseAction
 * @see PsiUtil
 */
class PrintTreeAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun handle(e: AnActionEvent) {
        val project = e.project ?: return
        UITools.runAsync(project, "Analyzing Code Structure", true) { progress ->
            try {
                progress.isIndeterminate = true
                progress.text = "Generating PSI tree structure..."
                ApplicationManager.getApplication().executeOnPooledThread {
                    val psiEntity = PsiUtil.getLargestContainedEntity(e)
                    if (psiEntity != null) {
                        log.info(PsiUtil.printTree(psiEntity))
                    } else {
                        log.warn("No PSI entity found in current context")
                    }
                }
            } catch (ex: Throwable) {
                UITools.error(log, "Failed to print PSI tree", ex)
            }
        }
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!super.isEnabled(event)) return false
        if (!AppSettingsState.instance.devActions) return false
        return PsiUtil.getLargestContainedEntity(event) != null
    }

    companion object {
        private val log = LoggerFactory.getLogger(PrintTreeAction::class.java)
    }
}