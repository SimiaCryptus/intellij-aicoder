package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import java.awt.Component
import javax.swing.JOptionPane

/**
 * Action that allows custom editing of code selections using AI.
 * Supports multiple languages and provides custom edit instructions.
 */

open class CustomEditAction : SelectionAction<String>() {
    private val log = Logger.getInstance(CustomEditAction::class.java)
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    interface VirtualAPI {
        fun editCode(
            code: String,
            operation: String,
            computerLanguage: String,
            humanLanguage: String
        ): EditedText

        data class EditedText(
            var code: String? = null,
            var language: String? = null
        )
    }

    val proxy: VirtualAPI
        get() {
            val chatProxy = ChatProxy(
                clazz = VirtualAPI::class.java,
                api = api,
                temperature = AppSettingsState.instance.temperature,
                model = AppSettingsState.instance.smartModel.chatModel(),
            )
            chatProxy.addExample(
                VirtualAPI.EditedText(
                    """
                // Print Hello, World! to the console
                println("Hello, World!")
                """.trimIndent(),
                    "java"
                )
            ) {
                it.editCode(
                    """println("Hello, World!")""",
                    "Add code comments",
                    "java",
                    "English"
                )
            }
            return chatProxy.create()
        }

    override fun getConfig(project: Project?): String? {
        return UITools.showInputDialog(
            project as Component?,
            "Enter edit instruction:",
            "Edit Code",
            JOptionPane.QUESTION_MESSAGE
        ) as String?
    }

    override fun processSelection(state: SelectionState, instruction: String?): String {
        if (instruction == null || instruction.isBlank()) return state.selectedText ?: ""
        return try {
            UITools.run(state.project, "Processing Edit", true) { progress ->
                progress.isIndeterminate = true
                progress.text = "Applying edit: $instruction"
                val settings = AppSettingsState.instance
                val outputHumanLanguage = settings.humanLanguage
                settings.getRecentCommands("customEdits").addInstructionToHistory(instruction)
                val result = proxy.editCode(
                    state.selectedText ?: "",
                    instruction,
                    state.language?.name ?: "text",
                    outputHumanLanguage
                )
                result.code ?: state.selectedText ?: ""
            }
        } catch (e: Exception) {
            log.error("Failed to process edit", e)
            UITools.showErrorDialog(
                state.project,
                "Failed to process edit: ${e.message}",
                "Edit Error"
            )
            state.selectedText ?: ""
        }
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!super.isEnabled(event)) return false
        return event.getData(DATA_KEY) != null
    }

    companion object {
        private val DATA_KEY = DataKey.create<String>("CustomEditAction.key")
    }
}