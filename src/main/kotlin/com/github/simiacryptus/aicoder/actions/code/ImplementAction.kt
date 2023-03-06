package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools.redoableRequest
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getCode
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getDocComment
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getSmallestIntersectingMajorCodeElement
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.matchesType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import java.util.*
import java.util.regex.Pattern

class ImplementAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val caret = event.getData(CommonDataKeys.CARET)
        val psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE)
        val smallestIntersectingMethod = getSmallestIntersectingMajorCodeElement(psiFile, caret!!) ?: return
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return
        val settings = AppSettingsState.getInstance()
        val code = smallestIntersectingMethod.text
        val indentedInput = IndentedText.fromString(code)
        var declaration: CharSequence = smallestIntersectingMethod.text
        //declaration = StringTools.stripPrefix(declaration.toString().trim(), PsiUtil.getDocComment(smallestIntersectingMethod).trim());
        declaration = StringTools.stripSuffix(declaration.toString().trim { it <= ' ' },
            getCode(smallestIntersectingMethod).trim { it <= ' ' })
        declaration = declaration.toString().trim { it <= ' ' }
        val completionRequest = settings.createCompletionRequest()
            .appendPrompt("```${computerLanguage.name.lowercase(Locale.ROOT)}\n")
            .addStops("```")
            //.appendPrompt("<code>").addStops("</code>")
            .appendPrompt(declaration)
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        val textRange = smallestIntersectingMethod.textRange
        val finalDeclaration = declaration
        redoableRequest(completionRequest, "", event,
            { string: CharSequence? ->
                IndentedText.fromString(
                    finalDeclaration.toString() + string.toString().trim { it <= ' ' })
                    .withIndent(indentedInput.indent).toString()
            }, { docString: CharSequence? ->
                replaceString(
                    document, textRange.startOffset, textRange.endOffset,
                    docString!!
                )
            }
        )
    }

    companion object {

        private fun isEnabled(event: AnActionEvent): Boolean {
            if (!AppSettingsState.getInstance().devActions) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
            if(computerLanguage == ComputerLanguage.Text) return false
            val caret = event.getData(CommonDataKeys.CARET)
            val psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE)
            val smallestIntersectingMethod =
                getSmallestIntersectingMajorCodeElement(psiFile, caret!!) ?: return false
            return isStub(smallestIntersectingMethod)
        }

        private fun isStub(element: PsiElement): Boolean {
            var declaration: CharSequence = element.text
            declaration = StringTools.stripPrefix(declaration.toString().trim { it <= ' ' },
                getDocComment(element).trim { it <= ' ' })
            declaration =
                StringTools.stripSuffix(declaration.toString().trim { it <= ' ' }, getCode(element).trim { it <= ' ' })
            declaration = declaration.toString().trim { it <= ' ' }
            var sansComments = StringTools.stripPrefix(compacted(element).trim { it <= ' ' }, declaration).toString()
                .trim { it <= ' ' }
            sansComments =
                StringTools.stripSuffix(StringTools.stripPrefix(sansComments, "{").toString().trim { it <= ' ' }, "}")
                    .trim { it <= ' ' }
            if (sansComments.isBlank()) return true
            return Pattern.compile("(?s)\\{\\s*}").matcher(sansComments).matches()
        }

        private fun compacted(element: PsiElement?): String {
            if (null == element) return ""
            val sb = StringBuffer()
            element.accept(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (matchesType(element, "Comment", "DocComment")) {
                        // Ignore
                    } else if (matchesType(element, "Method", "CodeBlock", "BlockExpr", "FunctionDefinition")) {
                        element.acceptChildren(this)
                    } else {
                        sb.append(element.text)
                    }
                    super.visitElement(element)
                }
            })
            return sb.toString()
        }
    }
}