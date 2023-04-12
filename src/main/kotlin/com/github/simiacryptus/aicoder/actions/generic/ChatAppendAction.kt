package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.hasSelection
import com.github.simiacryptus.aicoder.util.UITools.insertString
import com.github.simiacryptus.aicoder.util.UITools.redoableRequest
import com.simiacryptus.openai.ChatMessage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*

/**
 * The GenericAppend IntelliJ action allows users to quickly append a prompt to the end of a selected text.
 * To use, select some text and then select the GenericAppend action from the editor context menu.
 * The action will insert the completion at the end of the selected text.
 */
class ChatAppendAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

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
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        val selectionEnd = caret!!.selectionEnd
        redoableRequest(
            request, "", event
        ) { newText: CharSequence? ->
            insertString(
                document, selectionEnd,
                newText!!
            )
        }
    }

    companion object {
        @Suppress("unused")
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (UITools.isSanctioned()) return false
            return hasSelection(e)
        }
    }
}





