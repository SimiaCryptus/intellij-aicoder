package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.getInstruction
import com.github.simiacryptus.aicoder.util.UITools.redoableRequest
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import java.net.URLEncoder

class FactCheckLinkedListAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val settings = AppSettingsState.instance
        val caret = event.getRequiredData(CommonDataKeys.EDITOR).caretModel.primaryCaret
        val languageName = ComputerLanguage.getComputerLanguage(event)!!.name
        val psiFile = PsiUtil.getPsiFile(event)!!
        val elements = PsiUtil.getAllIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, "ListItem")
        val elementText = elements.flatMap { it.children.map { it.text } }.toTypedArray()
        val startOffset = elements.minByOrNull { it.startOffset }?.startOffset ?: caret.selectionStart
        val endOffset = elements.maxByOrNull { it.endOffset }?.endOffset ?: caret.selectionEnd
        val replaceString = event.getRequiredData(CommonDataKeys.EDITOR).document.text.substring(startOffset, endOffset)
        val completionRequest = settings.createTranslationRequest()
            .setInstruction(getInstruction("Translate each item into a search query that can be used to fact check each item with a search engine"))
            .setInputType(languageName)
            .setInputAttribute("type", "before")
            .setInputText(elementText.mapIndexed { index, s -> "${index + 1}. $s" }.joinToString("\n"))
            .setOutputType(languageName)
            .setOutputAttrute("type", "after")
            .buildCompletionRequest()
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        redoableRequest(completionRequest, "", event,
            { newText ->
                val queries = newText.replace("\"".toRegex(), "").split("\n\\d+\\.\\s+".toRegex()).toTypedArray()
                if (queries.size != elementText.size) {
                    throw RuntimeException("Invalid response: " + newText)
                }
                elementText.zip(queries).fold(replaceString) { acc, (statement, validationQuery) ->
                    acc.replace(
                        statement,
                        "[$statement](https://www.google.com/search?q=${URLEncoder.encode(validationQuery, "UTF-8")})"
                    )
                }
            },
            { newText ->
                replaceString(document, startOffset, endOffset, newText)
            })
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (UITools.isSanctioned()) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (ComputerLanguage.Markdown != computerLanguage) return false
            val caret = e.getData(CommonDataKeys.CARET) ?: return false
            val element = PsiUtil.getPsiFile(e) ?: return false
            return PsiUtil.getAllIntersecting(
                element,
                caret.selectionStart,
                caret.selectionEnd,
                "ListItem"
            ).isNotEmpty()
        }
    }
}


