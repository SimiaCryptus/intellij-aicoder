package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiClassContext
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.simiacryptus.openai.proxy.ChatProxy
import java.util.*

class InsertImplementationAction : BaseAction() {

    interface VirtualAPI {
        fun implementCode(
            specification: String,
            prefix: String,
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
            deserializerRetries = 5,
        ).create()

    override fun actionPerformed(event: AnActionEvent) {
        val humanLanguage = AppSettingsState.instance.humanLanguage
        val computerLanguage = ComputerLanguage.getComputerLanguage(event)
        val psiClassContextActionParams = getPsiClassContextActionParams(event).get()
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
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
            .filter { x: String -> x.isNotEmpty() }
            .reduce { a: String, b: String -> "$a $b" }.get()
        val endOffset = psiClassContextActionParams.largestIntersectingComment.textRange.endOffset
        val psiClassContext = PsiClassContext.getContext(
            psiClassContextActionParams.psiFile,
            psiClassContextActionParams.selectionStart,
            psiClassContextActionParams.selectionEnd,
            computerLanguage
        )


        UITools.redoableTask(event) {
            val newText = UITools.run(
                event.project, "Converting for Paste", true
            ) {
                proxy.implementCode(
                    specification = specification,
                    prefix = psiClassContext.toString(),
                    computerLanguage = computerLanguage.name,
                    humanLanguage = humanLanguage,
                ).code ?: ""
            }
            UITools.writeableFn(event) {
                UITools.insertString(
                    editor.document, endOffset,
                    newText
                )
            }
        }
    }

    class PsiClassContextActionParams(
        val psiFile: PsiFile,
        val caret: Caret,
        val selectionStart: Int,
        val selectionEnd: Int,
        val largestIntersectingComment: PsiElement
    )

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        if (computerLanguage == ComputerLanguage.Text) return false
        return getPsiClassContextActionParams(event).isPresent
    }

    companion object {
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