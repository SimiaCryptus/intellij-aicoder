package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.NlsSafe
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy

/**
 * The CommentsAction class is an IntelliJ action that allows users to add detailed comments to their code.
 * To use the CommentsAction, first select a block of code in the editor.
 * Then, select the action in the context menu.
 * The action will then generate a new version of the code with comments added.
 */
class CommentsAction : BaseAction() {


    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectionStart = primaryCaret.selectionStart
        val selectionEnd = primaryCaret.selectionEnd
        val selectedText = primaryCaret.selectedText
        val outputHumanLanguage = AppSettingsState.instance.humanLanguage
        val language = ComputerLanguage.getComputerLanguage(event)

        UITools.redoableTask(event) {
            val newText = UITools.run(event.project, "Commenting Code", true) {
                edit(api, selectedText, language, outputHumanLanguage)
            }
            UITools.writeableFn(event) {
                UITools.replaceString(editor.document, selectionStart, selectionEnd, newText)
            }
        }

    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        if (!UITools.hasSelection(event)) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        return computerLanguage != ComputerLanguage.Text
    }

    companion object {
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

        fun edit(api: OpenAIClient, selectedText: @NlsSafe String?, language: ComputerLanguage?, outputHumanLanguage: String) =
                ChatProxy(
                        clazz = VirtualAPI::class.java,
                        model = AppSettingsState.instance.defaultChatModel(),
                        api = api,
                        deserializerRetries = 5,
                ).create().editCode(
                        code = selectedText!!,
                        operations = "Add comments to each line explaining the code",
                        computerLanguage = language!!.name,
                        humanLanguage = outputHumanLanguage,
                ).code ?: ""
    }
}