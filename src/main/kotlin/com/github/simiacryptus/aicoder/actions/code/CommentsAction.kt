package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.openai.proxy.ChatProxy

/**
 * The CommentsAction class is an IntelliJ action that allows users to add detailed comments to their code.
 * To use the CommentsAction, first select a block of code in the editor.
 * Then, select the action in the context menu.
 * The action will then generate a new version of the code with comments added.
 */
class CommentsAction : BaseAction() {

    interface VirtualAPI {
        fun editCode(
            code: String,
            operations: String,
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
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectionStart = primaryCaret.selectionStart
        val selectionEnd = primaryCaret.selectionEnd
        val selectedText = primaryCaret.selectedText
        val outputHumanLanguage = AppSettingsState.instance.humanLanguage
        val language = ComputerLanguage.getComputerLanguage(event)

        UITools.redoableTask(event) {
            val newText = UITools.run(
                event.project, "Commenting Code", true
            ) {
                proxy.editCode(
                    code = selectedText!!,
                    operations = "Add comments to each line explaining the code",
                    computerLanguage = language!!.name,
                    humanLanguage = outputHumanLanguage,
                ).code ?: ""
            }
            UITools.writeableFn(event) {
                UITools.replaceString(editor.document, selectionStart, selectionEnd, newText!!)
            }
        }

    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        if (!UITools.hasSelection(e)) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
        if (computerLanguage == ComputerLanguage.Text) return false
        return true
    }
}