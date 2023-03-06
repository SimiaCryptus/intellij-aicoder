package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools.getInstruction
import com.github.simiacryptus.aicoder.util.UITools.redoableRequest
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ToStatementListAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val settings = AppSettingsState.getInstance()
        val caret = event.getRequiredData(CommonDataKeys.EDITOR).caretModel.primaryCaret
        val languageName = ComputerLanguage.getComputerLanguage(event)!!.name
        val endOffset: Int
        val startOffset: Int
        val text = if (caret.hasSelection()) {
            startOffset = caret.selectionStart
            endOffset = caret.selectionEnd
            caret.selectedText
        } else {
            val psiFile = PsiUtil.getLargestContainedEntity(event) ?: return
            val element = PsiUtil.getSmallestIntersecting(psiFile, caret.offset, caret.offset, "ListItem") ?: return
            startOffset = element.textOffset
            endOffset = element.textOffset + element.textLength
            element.children.map { it.text }.joinToString("\n")
        }
        val completionRequest = settings.createTranslationRequest()
            .setInstruction(getInstruction("Transform into a list of independent statements of fact. Resolve all pronouns and fully qualify each item."))
            .setInputType(languageName)
            .setInputAttribute("type", "before")
            .setInputText(text)
            .setOutputType(languageName)
            .setOutputAttrute("style", settings.style)
            .setOutputAttrute("type", "after")
            .setOutputAttrute("pronouns", "none")
            .buildCompletionRequest()
            .appendPrompt("1.")
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        redoableRequest(completionRequest, "", event,
            { newText -> "1. $newText" },
            { newText: CharSequence? -> replaceString(document, startOffset, endOffset, newText!!) })
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (ComputerLanguage.Markdown != computerLanguage) return false
            val caret = e.getData(CommonDataKeys.CARET) ?: return false
            return (PsiUtil.getSmallestIntersecting(
                PsiUtil.getLargestContainedEntity(e) ?: return false,
                caret.selectionStart,
                caret.selectionEnd,
                "ListItem"
            ) != null) || caret.hasSelection()
        }
    }
}


