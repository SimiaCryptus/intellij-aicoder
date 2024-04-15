package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy
import javax.swing.JOptionPane

open class CustomEditAction : SelectionAction<String>() {

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

    override fun getConfig(project: Project?): String {
        return UITools.showInputDialog(
            null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE
            //, AppSettingsState.instance.getRecentCommands("customEdits").mostRecentHistory
        ) as String? ?: ""
    }

    override fun processSelection(state: SelectionState, instruction: String?): String {
        if (instruction == null || instruction.isBlank()) return state.selectedText ?: ""
        val settings = AppSettingsState.instance
        val outputHumanLanguage = AppSettingsState.instance.humanLanguage
        settings.getRecentCommands("customEdits").addInstructionToHistory(instruction)
        return proxy.editCode(
            state.selectedText ?: "",
            instruction,
            state.language?.name ?: "",
            outputHumanLanguage
        ).code ?: state.selectedText ?: ""
    }
}