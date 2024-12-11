package com.github.simiacryptus.aicoder.actions.generic

 import com.intellij.openapi.project.Project
 import com.intellij.openapi.application.ApplicationManager
 import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.ui.Messages
import com.simiacryptus.jopenai.ChatClient

 abstract class BaseAction : AnAction() {
    protected val api: ChatClient by lazy { 
        ChatClient()
    }

     protected fun showError(project: Project?, message: String) {
         Messages.showErrorDialog(project, message, "Error")
     }

     protected fun showWarning(project: Project?, message: String) {
         Messages.showWarningDialog(project, message, "Warning") 
     }

     protected fun runWriteAction(project: Project, action: () -> Unit) {
         WriteCommandAction.runWriteCommandAction(project) {
             action()
         }
     }
}