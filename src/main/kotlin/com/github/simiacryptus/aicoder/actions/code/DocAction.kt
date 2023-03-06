package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.getInstruction
import com.github.simiacryptus.aicoder.util.UITools.redoableRequest
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*

/**
 * The DocAction is an IntelliJ action that enables users to add detailed documentation to their code.
 * It works by taking the current code element and translating it into a documentation comment.
 * The style of the comments is determined by the user's settings.
 *
 * To use DocAction, place the cursor on the code member they wish to document.
 * Then, select the action in the context menu.
 * DocAction will then take the selected code, translate it into a comment, and prepend the original code with the new doc comment.
 *
 * DocAction is a useful tool for quickly adding detailed documentation to code.
 * It can save time and effort, and make code easier to read and understand.
 */
class DocAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val language = ComputerLanguage.getComputerLanguage(event)
        val caret = event.getData(CommonDataKeys.CARET)
        val psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE)
        val smallestIntersectingMethod =
            PsiUtil.getSmallestIntersectingMajorCodeElement(psiFile, caret!!)
                ?: return
        val settings = AppSettingsState.getInstance()
        val code = smallestIntersectingMethod.text
        val indentedInput = IndentedText.fromString(code)
        val indent = indentedInput.indent
        val startOffset = smallestIntersectingMethod.textRange.startOffset
        val endOffset = smallestIntersectingMethod.textRange.endOffset
        val completionRequest = settings.createTranslationRequest()
            .setInputType(language!!.name)
            .setOutputType(language.name)
            .setInstruction(getInstruction("Rewrite to include detailed " + language.docStyle))
            .setInputAttribute("type", "uncommented")
            .setOutputAttrute("type", "commented")
            .setOutputAttrute("style", settings.style)
            .setInputText(indentedInput.textBlock)
            .buildCompletionRequest()
            .addStops(language.multilineCommentSuffix!!)
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        redoableRequest(completionRequest, "", event,
            { docString ->
                language.docComment!!.fromString(docString.toString().trim { it <= ' ' })!!.withIndent(indent)
                    .toString() + "\n" + indent + StringTools.trimPrefix(indentedInput.toString())
            },
            { docString ->
                replaceString(
                    document, startOffset, endOffset,
                    docString!!
                )
            }
        )
    }

    companion object {
        private fun isEnabled(event: AnActionEvent): Boolean {
            if(UITools.isSanctioned()) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
            if(computerLanguage == ComputerLanguage.Text) return false
            if (computerLanguage.docStyle.isEmpty()) return false
            val psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE)
            val caret = event.getData(CommonDataKeys.CARET)
            PsiUtil.getSmallestIntersectingMajorCodeElement(psiFile, caret!!) ?: return false
            return true
        }
    }
}