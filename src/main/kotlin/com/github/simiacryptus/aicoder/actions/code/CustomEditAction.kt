package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.openai.proxy.ChatProxy
import javax.swing.Icon
import javax.swing.JOptionPane

/**
 * The CustomEditAction class is an IntelliJ action that allows users to edit computer language code.
 * When the action is triggered, a dialog box will appear prompting the user to enter an instruction.
 * The instruction will then be used to transform the selected code.
 *
 * To use the CustomEditAction, first select the code that you want to edit.
 * Then, select the action in the context menu.
 * A dialog box will appear, prompting you to enter an instruction.
 * Enter the instruction and press OK.
 */
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
        ): ConvertedText
        data class ConvertedText(
            val code: String? = null,
            val language: String? = null
        )
    }

    val proxy: VirtualAPI
        get() = ChatProxy(
            clazz = VirtualAPI::class.java,
            api = api,
            maxTokens = AppSettingsState.instance.maxTokens,
            deserializerRetries = 5,
        ).create()

    override fun actionPerformed(event: AnActionEvent) {
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
        if (UITools.isSanctioned()) return false
        if (!UITools.hasSelection(event)) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        return computerLanguage != ComputerLanguage.Text
    }
}