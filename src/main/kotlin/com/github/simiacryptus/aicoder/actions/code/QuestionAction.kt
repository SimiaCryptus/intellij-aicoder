package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import javax.swing.JOptionPane

/**
 * The DescribeAction class is an action that can be used to describe a piece of code in plain language.
 * It is triggered when the user selects a piece of code and then selects the action.
 * The action will then generate a description of the code in the user's chosen language.
 * The description will be formatted according to the user's chosen style and will be inserted prior to the code as a comment.
 */
class QuestionAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    private fun isEnabled(e: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
        if (computerLanguage == ComputerLanguage.Text) return false
        return true
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        var selectionStart = primaryCaret.selectionStart
        var selectionEnd = primaryCaret.selectionEnd
        var selectedText = primaryCaret.selectedText
        val language = ComputerLanguage.getComputerLanguage(event)!!
        if (selectedText.isNullOrEmpty()) {
            val document = editor.document
            val lineNumber = document.getLineNumber(selectionStart)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val lineEndOffset = document.getLineEndOffset(lineNumber)
            val currentLine = document.text.substring(lineStartOffset, lineEndOffset)
            selectionStart = lineStartOffset
            selectionEnd = lineEndOffset
            selectedText = currentLine
        }
        actionPerformed(event, editor, selectionStart, selectionEnd, selectedText, language)
    }

    private fun actionPerformed(
        event: AnActionEvent,
        editor: Editor,
        selectionStart: Int,
        selectionEnd: Int,
        selectedText: String,
        language: ComputerLanguage
    ) {
        val indent = UITools.getIndent(event)
        val settings = AppSettingsState.instance
        val question =
            JOptionPane.showInputDialog(null, "Question:", "Question", JOptionPane.QUESTION_MESSAGE) ?: return
        if (question.isBlank()) return
        val request = settings.createCompletionRequest()
            .appendPrompt(
                """
                Analyze the following code to answer the question "$question"
                ```$language
                    ${selectedText.replace("\n", "\n    ")}
                ```
                
                Question: $question
                Answer:
            """.trimIndent()
            )
        UITools.redoableRequest(request, indent, event,
            { newText ->
                var text = """
                    Question: $question
                    Answer: ${newText.toString().trim()}
                """.trimMargin()
                //text = StringTools.lineWrapping(text!!.toString().trim { it <= ' ' }, 120)
                "$indent${language.blockComment.fromString(text)!!.withIndent(indent)}\n$indent$selectedText"
            }, { newText ->
                UITools.replaceString(
                    editor.document,
                    selectionStart,
                    selectionEnd,
                    newText!!
                )
            })
    }
}