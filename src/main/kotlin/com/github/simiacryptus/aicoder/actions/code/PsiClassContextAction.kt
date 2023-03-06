package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiClassContext
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.*

class PsiClassContextAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val humanLanguage = AppSettingsState.getInstance().humanLanguage
        val computerLanguage = ComputerLanguage.getComputerLanguage(event)
        val psiClassContextActionParams = getPsiClassContextActionParams(event).get()
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val settings = AppSettingsState.getInstance()
        var instruct = psiClassContextActionParams.largestIntersectingComment.text.trim { it <= ' ' }
        if (primaryCaret.selectionEnd > primaryCaret.selectionStart) {
            val selectedText = Objects.requireNonNull(primaryCaret.selectedText)
            if (selectedText!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray().size > 4) {
                instruct = selectedText.trim { it <= ' ' }
            }
        }
        assert(computerLanguage != null)
        val specification = Objects.requireNonNull(computerLanguage!!.getCommentModel(instruct))!!
            .fromString(instruct)!!.stream()
            .map { obj: CharSequence -> obj.toString() }
            .map { obj: String -> obj.trim { it <= ' ' } }
            .filter { x: String -> !x.isEmpty() }
            .reduce { a: String, b: String -> "$a $b" }.get()
        val endOffset = psiClassContextActionParams.largestIntersectingComment.textRange.endOffset
        val request = settings.createTranslationRequest()
            .setInstruction("Implement " + humanLanguage + " as " + computerLanguage.name + " code")
            .setInputType(humanLanguage)
            .setInputAttribute("type", "instruction")
            .setInputText(specification)
            .setOutputType(computerLanguage.name)
            .setOutputAttrute("type", "code")
            .setOutputAttrute("style", settings.style)
            .buildCompletionRequest()
            .appendPrompt(
                """
                ${
                    PsiClassContext.getContext(
                        psiClassContextActionParams.psiFile,
                        psiClassContextActionParams.selectionStart,
                        psiClassContextActionParams.selectionEnd
                    )
                }
                
                """.trimIndent()
            )
        UITools.redoableRequest(request, UITools.getIndent(psiClassContextActionParams.caret), event,
            { newText ->
                """
                    
                    $newText
                    """.trimIndent()
            }, { newText ->
                UITools.insertString(
                    editor.document, endOffset,
                    newText!!
                )
            }
        )
    }

    class PsiClassContextActionParams constructor(
        val psiFile: PsiFile,
        val caret: Caret,
        val selectionStart: Int,
        val selectionEnd: Int,
        val largestIntersectingComment: PsiElement
    )

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if(UITools.isSanctioned()) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (computerLanguage == ComputerLanguage.Text) return false
            return getPsiClassContextActionParams(e).isPresent
        }

        fun getPsiClassContextActionParams(e: AnActionEvent): Optional<PsiClassContextActionParams> {
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)
            if (null != psiFile) {
                val caret = e.getData(CommonDataKeys.CARET)
                if (null != caret) {
                    val selectionStart = caret.selectionStart
                    val selectionEnd = caret.selectionEnd
                    val largestIntersectingComment =
                        PsiUtil.getLargestIntersectingComment(psiFile, selectionStart, selectionEnd)
                    if (largestIntersectingComment != null) {
                        return Optional.of(
                            PsiClassContextActionParams(
                                psiFile,
                                caret,
                                selectionStart,
                                selectionEnd,
                                largestIntersectingComment
                            )
                        )
                    }
                }
            }
            return Optional.empty()
        }
    }
}