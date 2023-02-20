package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools.getIndent
import com.github.simiacryptus.aicoder.util.UITools.getInstruction
import com.github.simiacryptus.aicoder.util.UITools.redoableRequest
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.psi.PsiMarkdownContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*

/**
 * The MarkdownContextAction is an action in IntelliJ that allows users to quickly create a Markdown document with the selected text.
 *
 * Work In Progress
 */
class MarkdownContextAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectionStart = primaryCaret.selectionStart
        val selectionEnd = primaryCaret.selectionEnd
        val selectedText = primaryCaret.selectedText
        val humanLanguage = AppSettingsState.getInstance().humanLanguage
        val markdownContextParams = getMarkdownContextParams(event, humanLanguage)
        val settings = AppSettingsState.getInstance()
        val psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE)
        var context = PsiMarkdownContext.getContext(
            psiFile,
            Objects.requireNonNull(markdownContextParams)!!.selectionStart,
            markdownContextParams!!.selectionEnd
        ).toString(markdownContextParams.selectionEnd)
        context = "$context\n<!-- $selectedText-->\n"
        context = """
            $context
            
            """.trimIndent()
        val request = settings.createTranslationRequest()
            .setOutputType("markdown")
            .setInstruction(getInstruction(String.format("Using Markdown and %s", markdownContextParams.humanLanguage)))
            .setInputType("instruction")
            .setInputText(selectedText)
            .setOutputAttrute("type", "document")
            .setOutputAttrute("style", settings.style)
            .buildCompletionRequest()
            .appendPrompt(context)
        val caret = event.getData(CommonDataKeys.CARET)
        val indent = getIndent(caret)
        redoableRequest(
            request, indent, event
        ) { newText: CharSequence? ->
            replaceString(
                editor.document, selectionStart, selectionEnd,
                newText!!
            )
        }
    }

    class MarkdownContextParams constructor(
        val humanLanguage: CharSequence,
        val selectionStart: Int,
        val selectionEnd: Int
    )

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (!AppSettingsState.getInstance().devActions) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            return if (ComputerLanguage.Markdown != computerLanguage) false else null != getMarkdownContextParams(
                e,
                AppSettingsState.getInstance().humanLanguage
            )
        }

        fun getMarkdownContextParams(e: AnActionEvent, humanLanguage: CharSequence): MarkdownContextParams? {
            val caret = e.getData(CommonDataKeys.CARET)
            if (null != caret) {
                val selectionStart = caret.selectionStart
                val selectionEnd = caret.selectionEnd
                if (selectionStart < selectionEnd) {
                    return MarkdownContextParams(humanLanguage, selectionStart, selectionEnd)
                }
            }
            return null
        }
    }
}