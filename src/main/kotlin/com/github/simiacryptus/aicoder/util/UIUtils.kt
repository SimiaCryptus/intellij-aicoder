package com.github.simiacryptus.aicoder.util

import java.awt.Desktop
import java.awt.Point
import java.net.URI
import javax.swing.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.actionSystem.AnActionEvent
import java.util.*

object UIUtils {

    fun openBrowserToUri(uri: URI) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri)
        } else {
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
            val command = when {
                os.contains("win") -> "rundll32 url.dll,FileProtocolHandler $uri"
                os.contains("mac") -> "open $uri"
                os.contains("nix") || os.contains("nux") -> "xdg-open $uri"
                else -> throw UnsupportedOperationException("Cannot open URI on this OS")
            }
            Runtime.getRuntime().exec(command)
        }
    }

    fun showOptionsDialog(title: String, options: Array<String>): String? {
        val optionPane = JOptionPane()
        optionPane.message = title
        optionPane.messageType = JOptionPane.QUESTION_MESSAGE
        optionPane.optionType = JOptionPane.DEFAULT_OPTION
        optionPane.options = options
        val dialog = optionPane.createDialog(null, title)
        dialog.isVisible = true
        return optionPane.value as? String
    }

    fun createStatusDialog(message: String, location: Point): JFrame {
        val frame = JFrame()
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.setSize(300, 100)
        frame.location = location
        frame.add(JLabel(message, SwingConstants.CENTER))
        frame.isVisible = true
        return frame
    }

    fun redoableTask(e: AnActionEvent, task: () -> Unit) {
        CommandProcessor.getInstance().executeCommand(e.project, task, "Redoable Task", null)
    }

    fun run(project: Project?, title: String, canBeCancelled: Boolean, task: () -> Unit) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(task, title, canBeCancelled, project)
    }
}