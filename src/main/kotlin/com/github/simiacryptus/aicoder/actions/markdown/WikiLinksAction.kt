package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.ui.OpenAI_API
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.getInstruction
import com.github.simiacryptus.aicoder.util.UITools.redoableRequest
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.simiacryptus.util.StringTools.replaceAllNonOverlapping
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

class WikiLinksAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val settings = AppSettingsState.instance
        val caret = event.getRequiredData(CommonDataKeys.EDITOR).caretModel.primaryCaret
        val languageName = ComputerLanguage.getComputerLanguage(event)!!.name
        val endOffset: Int
        val startOffset: Int
        val psiFile = PsiUtil.getPsiFile(event)!!
        val elements = PsiUtil.getAllIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, "ListItem")
        val elementText: CharSequence
        if (elements.isEmpty()) {
            elementText = caret.selectedText!!
            startOffset = caret.selectionStart
            endOffset = caret.selectionEnd
        } else {
            elementText = elements.flatMap { it.children.map { it.text } }.toTypedArray().joinToString("\n")
            startOffset = elements.minByOrNull { it.startOffset }?.startOffset ?: caret.selectionStart
            endOffset = elements.maxByOrNull { it.endOffset }?.endOffset ?: caret.selectionEnd
        }
        val replaceString = event.getRequiredData(CommonDataKeys.EDITOR).document.text.substring(startOffset, endOffset)
        val completionRequest = settings.createTranslationRequest()
            .setInstruction(
                getInstruction(
                    """
                Describe the context of this text by listing terms and topics, linked to wikipedia. 
                For each term, list the words in the text that are linked to the term.
                """.trimIndent().trim()
                )
            )
            .setInputType(languageName)
            .setInputAttribute("type", "before")
            .setInputText(elementText)
            .setOutputType(languageName)
            .setOutputAttrute("type", "after")
            .addExample("1. [FooBar](https://en.wikipedia.org/wiki/FooBar) - foo, bar", mapOf("type" to "example"))
            .buildCompletionRequest()
            .appendPrompt("1. [")
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        redoableRequest(
            completionRequest, "", event,
            {
                val listItems = ("[$it").replace("\"".toRegex(), "").split("\n\\d+\\.\\s+".toRegex())
                val replacements = listItems
                    .flatMap { term ->
                        val mainTerm = term.substringBefore("](").substringAfter("[")
                        val linkTarget = term.substringBefore(")").substringAfter("(")
                        val extraWords = term.substringAfter(")").trim().trimStart('-').split(",".toRegex())
                            .map { it.trim() }.filter { it.isNotBlank() }
                        (extraWords + listOf(mainTerm)).distinct().map { it to "[$it]($linkTarget)" }
                    }
                replaceAllNonOverlapping(replaceString, *replacements.toTypedArray())
            },
            { replaceString(document, startOffset, endOffset, it) },
            OpenAI_API.filterStringResult(stripUnbalancedTerminators = false)
        )
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (UITools.isSanctioned()) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (ComputerLanguage.Markdown != computerLanguage) return false
            val caret = e.getData(CommonDataKeys.CARET) ?: return false
            return caret.hasSelection()
        }
    }
}


