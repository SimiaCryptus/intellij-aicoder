package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.simiacryptus.util.StringTools
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
class RewordCommentAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val humanLanguage = AppSettingsState.instance.humanLanguage
        val computerLanguage = ComputerLanguage.getComputerLanguage(event)
        val rewordCommentParams = getRewordCommentParams(event)
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val settings = AppSettingsState.instance
        val text = Objects.requireNonNull(rewordCommentParams)!!.largestIntersectingComment.text
        val commentModel = computerLanguage!!.getCommentModel(text)
        val commentText = Objects.requireNonNull(commentModel)!!.fromString(text.trim { it <= ' ' })!!
            .stream()
            .map { obj: CharSequence -> obj.toString() }
            .map { obj: String -> obj.trim { it <= ' ' } }
            .filter { x: String -> x.isNotEmpty() }
            .reduce { a: String, b: String ->
                """
                     $a
                     $b
                     """.trimIndent()
            }.get()
        val startOffset = rewordCommentParams!!.largestIntersectingComment.textRange.startOffset
        val endOffset = rewordCommentParams.largestIntersectingComment.textRange.endOffset
        val indent = UITools.getIndent(rewordCommentParams.caret)
        val request = settings.createTranslationRequest()
            .setInstruction(UITools.getInstruction("Reword"))
            .setInputText(commentText)
            .setInputType(humanLanguage)
            .setOutputAttrute("type", "input")
            .setOutputType(humanLanguage)
            .setOutputAttrute("type", "output")
            .setOutputAttrute("style", settings.style)
            .buildCompletionRequest()
        val document = editor.document
        UITools.redoableRequest(request, "", event,
            { newText ->
                indent.toString() + commentModel.fromString(
                    StringTools.lineWrapping(
                        newText, 120
                    )
                )!!.withIndent(indent)
            }, { newText ->
                UITools.replaceString(
                    document, startOffset, endOffset,
                    newText
                )
            }
        )
    }

    class RewordCommentParams constructor(val caret: Caret, val largestIntersectingComment: PsiElement)
    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (UITools.isSanctioned()) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (computerLanguage == ComputerLanguage.Text) return false
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