package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.openai.APIClientBase
import com.simiacryptus.openai.proxy.ChatProxy
import javax.swing.Icon
import javax.swing.JOptionPane

open class CustomEditAction(
    name: String? = null,
    description: String? = null,
    icon: Icon? = null,
) : BaseAction(name, description, icon) {

    interface VirtualAPI {
        fun editCode(
            code: String,
            operation: String,
            computerLanguage: String,
            humanLanguage: String,
        ): EditedText

        data class EditedText(
            val code: String? = null,
            val language: String? = null
        )
    }

    val proxy: VirtualAPI
        get() {
            val chatProxy = ChatProxy(
                clazz = VirtualAPI::class.java,
                api = api,
                model = AppSettingsState.instance.defaultChatModel(),
            )
            chatProxy.addExample(
                returnValue = VirtualAPI.EditedText(
                    code = """
                        |// Print Hello, World! to the console
                        |println("Hello, World!")
                        """.trimMargin(),
                    language = "java"
                )
            ) {
                it.editCode(
                    code = """println("Hello, World!")""",
                    operation = "Add code comments",
                    computerLanguage = "java",
                    humanLanguage = "English"
                )
            }
            return chatProxy.create()
        }

    override fun handle(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectionStart = primaryCaret.selectionStart
        val selectionEnd = primaryCaret.selectionEnd
        val selectedText = primaryCaret.selectedText
        val computerLanguage = ComputerLanguage.getComputerLanguage(event)!!.name
        val instruction = getInstruction() ?: return
        if (instruction.isBlank()) return
        val settings = AppSettingsState.instance
        val outputHumanLanguage = AppSettingsState.instance.humanLanguage
        settings.addInstructionToHistory(instruction)
        UITools.redoableTask(event) {
            val newText = UITools.run(
                event.project, "Editing Code", true
            ) {
                proxy.editCode(
                    code = selectedText!!,
                    operation = instruction,
                    computerLanguage = computerLanguage,
                    humanLanguage = outputHumanLanguage,
                ).code ?: ""
            }
            UITools.writeableFn(event) {
                UITools.replaceString(
                    editor.document, selectionStart, selectionEnd,
                    newText
                )
            }
        }
    }

    open fun getInstruction(): String? =
        JOptionPane.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE)

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (APIClientBase.isSanctioned()) return false
        if (!UITools.hasSelection(event)) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        return computerLanguage != ComputerLanguage.Text
    }
}
