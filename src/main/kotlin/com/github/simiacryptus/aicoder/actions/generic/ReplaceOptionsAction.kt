package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.showRadioButtonDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.simiacryptus.util.StringTools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.TextRange
import com.simiacryptus.openai.proxy.ChatProxy
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

class ReplaceOptionsAction : BaseAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    interface VirtualAPI {
        fun suggestText(prefix: String, example: String, suffix: String): Suggestions
        data class Suggestions(
            val choices: List<String>? = null
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
        val caret = event.getData(CommonDataKeys.CARET)
        val document = caret!!.editor.document
        val selectedText = caret.selectedText
        val idealLength =
            2.0.pow(2 + ceil(ln(selectedText!!.length.toDouble()) / ln(2.0))).toInt()
        val newlines = "\n".toRegex()
        val selectionStart = caret.selectionStart
        val allBefore = document.getText(TextRange(0, selectionStart))
        val selectionEnd = caret.selectionEnd
        val allAfter = document.getText(TextRange(selectionEnd, document.textLength))
        val before = StringTools.getSuffixForContext(allBefore, idealLength).replace(newlines, " ")
        val after = StringTools.getPrefixForContext(allAfter, idealLength).replace(newlines, " ")

        UITools.redoableTask(event) {
            val options = UITools.run(
                event.project, "Brainstorming Options...", true
            ) {
                proxy.suggestText(
                    before,
                    selectedText,
                    after
                ).choices
            }
            val choice = showRadioButtonDialog("Select an option to fill in the blank:", *(options!!.toTypedArray()))
            if (null != choice) {
                UITools.writeableFn(event) {
                    UITools.replaceString(
                        document, selectionStart, selectionEnd,
                        choice
                    )
                }
            } else Runnable { }
        }
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        return UITools.hasSelection(event)
    }
}