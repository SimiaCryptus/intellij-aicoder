package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.openai.proxy.ChatProxy
import javax.swing.JOptionPane

/**
 * The DescribeAction class is an action that can be used to describe a piece of code in plain language.
 * It is triggered when the user selects a piece of code and then selects the action.
 * The action will then generate a description of the code in the user's chosen language.
 * The description will be formatted according to the user's chosen style and will be inserted prior to the code as a comment.
 */
class QuestionAction : BaseAction() {

    interface VirtualAPI {
        fun questionCode(
            code: String,
            question: String,
        ): Answer
        data class Answer(
            val text: String? = null,
        )
    }

    val proxy: VirtualAPI
        get() = ChatProxy(
            clazz = VirtualAPI::class.java,
            api = api,
            deserializerRetries = 5,
        ).create()

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        return computerLanguage != ComputerLanguage.Text
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        var selectionStart = primaryCaret.selectionStart
        var selectedText = primaryCaret.selectedText
        val language = ComputerLanguage.getComputerLanguage(event)!!
        if (selectedText.isNullOrEmpty()) {
            val document = editor.document
            val lineNumber = document.getLineNumber(selectionStart)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val lineEndOffset = document.getLineEndOffset(lineNumber)
            val currentLine = document.text.substring(lineStartOffset, lineEndOffset)
            selectionStart = lineStartOffset
            selectedText = currentLine
        }

        val indent = UITools.getIndent(event)
        val question =
            JOptionPane.showInputDialog(null, "Question:", "Question", JOptionPane.QUESTION_MESSAGE) ?: return
        if (question.isBlank()) return

        UITools.redoableTask(event) {
            val newText = UITools.run(
                event.project, "Answering Question", true
            ) {
                proxy.questionCode(
                    code = selectedText,
                    question = question,
                ).text ?: ""
            }
            val answer = """
                    |Question: $question
                    |Answer: ${newText.trim()}
                    |""".trimMargin().trim()
            UITools.writeableFn(event) {
                UITools.insertString(
                    editor.document,
                    selectionStart,
                    "$indent${language.blockComment.fromString(answer)!!.withIndent(indent)}\n$indent"
                )
            }
        }

    }
}