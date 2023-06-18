package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.hasSelection
import com.github.simiacryptus.aicoder.util.UITools.insertString
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.simiacryptus.openai.OpenAIClient.ChatMessage
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*

/**
 * The GenericAppend IntelliJ action allows users to quickly append a prompt to the end of a selected text.
 * To use, select some text and then select the GenericAppend action from the editor context menu.
 * The action will insert the completion at the end of the selected text.
 */
class AppendAction : BaseAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val caret = event.getData(CommonDataKeys.CARET)
        val before: CharSequence? = Objects.requireNonNull(caret)!!.selectedText
        val settings = AppSettingsState.instance
        val request = settings.createChatRequest() //.appendPrompt(before ?: "")
        request.messages = arrayOf(
            ChatMessage(
                ChatMessage.Role.system,
                "Append text to the end of the user's prompt"
            ),
            ChatMessage(
                ChatMessage.Role.user,
                before.toString()
            )
        )
        val document = (event.getData(CommonDataKeys.EDITOR) ?: return).document
        val selectionEnd = caret!!.selectionEnd
        UITools.redoableTask(event) {
            val newText = UITools.run(
                event.project, "Getting completion", true
            ) {
                (api.chat(request).choices[0].message?.content ?: "").trimPrefix(before ?: "")
            }
            UITools.writeableFn(event) {
                insertString(document, selectionEnd, newText)
            }
        }
    }
    @Suppress("unused")
    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        return hasSelection(event)
    }

}

private fun String.trimPrefix(charSequence: CharSequence) = if (this.startsWith(charSequence)) {
    this.substring(charSequence.length)
} else {
    this
}





