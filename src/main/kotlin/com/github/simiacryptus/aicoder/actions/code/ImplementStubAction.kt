package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getCode
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getDocComment
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getSmallestIntersectingMajorCodeElement
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.matchesType
import com.simiacryptus.util.StringUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.simiacryptus.openai.proxy.ChatProxy
import java.util.*
import java.util.regex.Pattern

class ImplementStubAction : BaseAction() {

    interface VirtualAPI {
        fun editCode(
            code: String,
            operation: String,
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
            model = AppSettingsState.instance.defaultChatModel(),
            api = api,
            deserializerRetries = 5,
        ).create()

    override fun actionPerformed(event: AnActionEvent) {
        val caret = event.getData(CommonDataKeys.CARET)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val smallestIntersectingMethod = getSmallestIntersectingMajorCodeElement(psiFile, caret!!) ?: return
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return
        val settings = AppSettingsState.instance
        val code = smallestIntersectingMethod.text
        var declaration: CharSequence = smallestIntersectingMethod.text
        //declaration = StringTools.stripPrefix(declaration.toString().trim(), PsiUtil.getDocComment(smallestIntersectingMethod).trim());
        declaration = StringUtil.stripSuffix(declaration.toString().trim(), getCode(smallestIntersectingMethod).trim())
        declaration = declaration.toString().trim()
        val document = (event.getData(CommonDataKeys.EDITOR) ?: return).document
        val textRange = smallestIntersectingMethod.textRange
        val finalDeclaration = declaration
        val outputHumanLanguage = settings.humanLanguage

        UITools.redoableTask(event) {
            val newText = UITools.run(
                event.project, "Editing Code", true
            ) {
                proxy.editCode(
                    code = finalDeclaration,
                    operation = "Implement Stub",
                    computerLanguage = computerLanguage.name.lowercase(Locale.ROOT),
                    humanLanguage = outputHumanLanguage,
                ).code ?: ""
            }
            UITools.writeableFn(event) {
                replaceString(
                    document,
                    textRange.startOffset,
                    textRange.endOffset,
                    IndentedText.fromString(finalDeclaration.toString() + newText.trim())
                        .withIndent(IndentedText.fromString(code).indent).toString()
                )
            }
        }

    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        if (!AppSettingsState.instance.devActions) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        if (computerLanguage == ComputerLanguage.Text) return false
        val caret = event.getData(CommonDataKeys.CARET)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        if(psiFile == null) return false
        val smallestIntersectingMethod =
            getSmallestIntersectingMajorCodeElement(psiFile, caret!!) ?: return false
        return isStub(smallestIntersectingMethod)
    }

    companion object {

        private fun isStub(element: PsiElement): Boolean {
            var declaration: CharSequence = element.text
            declaration = StringUtil.stripPrefix(declaration.toString().trim(),
                getDocComment(element).trim())
            declaration =
                StringUtil.stripSuffix(declaration.toString().trim(), getCode(element).trim())
            declaration = declaration.toString().trim()
            var sansComments = StringUtil.stripPrefix(compacted(element).trim(), declaration).toString()
                .trim()
            sansComments =
                StringUtil.stripSuffix(StringUtil.stripPrefix(sansComments, "{").toString().trim(), "}")
                    .trim()
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