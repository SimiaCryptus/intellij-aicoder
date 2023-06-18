package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.insertString
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.openai.proxy.ChatProxy

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
class DocAction : BaseAction() {

    interface VirtualAPI {
        fun processCode(
            code: String,
            operation: String,
            computerLanguage: String,
            humanLanguage: String,
        ): ConvertedText

        data class ConvertedText(
            val text: String? = null,
            val language: String? = null,
        )
    }

    val proxy: VirtualAPI
        get() {
            val chatProxy = ChatProxy(
                clazz = VirtualAPI::class.java,
                api = api,
                deserializerRetries = 5,
            )
            chatProxy.addExample(
                VirtualAPI.ConvertedText(
                    text = """
                        /**
                         *  Prints "Hello, world!" to the console
                         */
                        """.trimIndent().trim(),
                    language = "English"
                )
            ) {
                it.processCode(
                    code = """
                        fun hello() {
                            println("Hello, world!")
                        }
                        """.trimIndent().trim(),
                    operation = "Write detailed KDoc prefix for code block",
                    computerLanguage = "Kotlin",
                    humanLanguage = "English"
                )
            }
            return chatProxy.create()
        }

    override fun actionPerformed(event: AnActionEvent) {
        val language = ComputerLanguage.getComputerLanguage(event)
        val caret = event.getData(CommonDataKeys.CARET)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val smallestIntersectingMethod =
            PsiUtil.getSmallestIntersectingMajorCodeElement(psiFile, caret!!)
                ?: return
        val settings = AppSettingsState.instance
        val code = smallestIntersectingMethod.text
        val indentedInput = IndentedText.fromString(code)
        val startOffset = smallestIntersectingMethod.textRange.startOffset
        val document = (event.getData(CommonDataKeys.EDITOR) ?: return).document
        val outputHumanLanguage = settings.humanLanguage

        UITools.redoableTask(event) {
            val docString = UITools.run(
                event.project, "Documenting Code", true
            ) {
                proxy.processCode(
                    code = indentedInput.textBlock.toString(),
                    operation = "Write detailed " + (language?.docStyle ?: "documentation") + " prefix for code block",
                    computerLanguage = language!!.name,
                    humanLanguage = outputHumanLanguage,
                ).text ?: ""
            }
            UITools.writeableFn(event) {
                insertString(
                    document,
                    startOffset,
                    docString
                )
            }
        }
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        if (computerLanguage == ComputerLanguage.Text) return false
        if (computerLanguage.docStyle.isEmpty()) return false
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return false
        val caret = event.getData(CommonDataKeys.CARET)
        PsiUtil.getSmallestIntersectingMajorCodeElement(psiFile, caret!!) ?: return false
        return true
    }
}