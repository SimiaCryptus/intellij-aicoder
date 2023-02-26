package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.psi.PsiElement
import java.util.*

/**
 * The RewordCommentAction is an IntelliJ action that allows users to reword comments in their code.
 * It is triggered when the user selects a comment in their code and then clicks the RewordCommentAction button.
 * This will replace the old comment with the new one.
 */
class TranslateCommentAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val humanLanguage = AppSettingsState.getInstance().humanLanguage
        val computerLanguage = Objects.requireNonNull(ComputerLanguage.getComputerLanguage(event))
        val rewordCommentParams = Objects.requireNonNull(getRewordCommentParams(event))!!
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val settings = AppSettingsState.getInstance()
        val largestIntersectingComment = rewordCommentParams.largestIntersectingComment
        val indent = UITools.getIndent(rewordCommentParams.caret)
        val text = largestIntersectingComment.text
        val textRange = largestIntersectingComment.textRange
        val commentModel = Objects.requireNonNull(computerLanguage!!.getCommentModel(text))
        val commentText = commentModel!!.fromString(text.trim { it <= ' ' })!!.stream()
            .map { obj: CharSequence -> obj.toString() }
            .reduce { a: String, b: String ->
                """
                     $a
                     $b
                     """.trimIndent()
            }.get()
        val request = settings.createCompletionRequest()
            .appendPrompt(
                String.format(
                    "TRANSLATE INTO %s\nINPUT:\n\t%s\n%s:\n",
                    humanLanguage.uppercase(Locale.getDefault()),
                    commentText.replace("\n", "\n\t"),
                    humanLanguage.uppercase(Locale.getDefault())
                )
            )
        val document = editor.document
        UITools.redoableRequest(request, "", event,
            { newText: CharSequence? ->
                indent.toString() + commentModel.fromString(newText.toString().trim { it <= ' ' })!!
                    .withIndent(indent)
            }
        ) { newText: CharSequence? ->
            UITools.replaceString(
                document, textRange.startOffset, textRange.endOffset,
                newText!!
            )
        }
    }

    class RewordCommentParams constructor(val caret: Caret, val largestIntersectingComment: PsiElement)
    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if(computerLanguage == ComputerLanguage.Text) return false
            return null != getRewordCommentParams(e)
        }

        fun getRewordCommentParams(e: AnActionEvent): RewordCommentParams? {
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)
                ?: return null
            val caret = e.getData(CommonDataKeys.CARET)
                ?: return null
            val selectionStart = caret.selectionStart
            val selectionEnd = caret.selectionEnd
            val largestIntersectingComment = PsiUtil.getLargestIntersectingComment(
                psiFile,
                selectionStart,
                selectionEnd
            )
                ?: return null
            return RewordCommentParams(caret, largestIntersectingComment)
        }
    }
}