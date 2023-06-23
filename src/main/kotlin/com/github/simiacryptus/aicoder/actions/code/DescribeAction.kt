package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.UITools
import com.simiacryptus.util.StringUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.openai.proxy.ChatProxy

/**
 * The DescribeAction class is an action that can be used to describe a piece of code in plain language.
 * It is triggered when the user selects a piece of code and then selects the action.
 * The action will then generate a description of the code in the user's chosen language.
 * The description will be formatted according to the user's chosen style and will be inserted prior to the code as a comment.
 */
class DescribeAction : BaseAction() {

    interface VirtualAPI {
        fun describeCode(
            code: String,
            computerLanguage: String,
            humanLanguage: String,
        ): ConvertedText

        data class ConvertedText(
            val text: String? = null,
            val language: String? = null
        )
    }

    val proxy: VirtualAPI
        get() = ChatProxy(
            clazz = VirtualAPI::class.java,
            api = api,
            model = AppSettingsState.instance.defaultChatModel(),
            deserializerRetries = 5,
        ).create()


    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        var selectionStart = primaryCaret.selectionStart
        var selectionEnd = primaryCaret.selectionEnd
        var selectedText = primaryCaret.selectedText
        val language = ComputerLanguage.getComputerLanguage(event)!!
        if (selectedText.isNullOrEmpty()) {
            val document = editor.document
            val lineNumber = document.getLineNumber(selectionStart)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val lineEndOffset = document.getLineEndOffset(lineNumber)
            val currentLine = document.text.substring(lineStartOffset, lineEndOffset)
            selectionStart = lineStartOffset
            selectionEnd = lineEndOffset
            selectedText = currentLine
        }
        val indent = UITools.getIndent(event)
        val settings = AppSettingsState.instance

        UITools.redoableTask(event) {
            val newText = UITools.run(
                event.project, "Converting Code To Text", true
            ) {
                proxy.describeCode(
                    code = IndentedText.fromString(selectedText).textBlock.toString().trim(),
                    computerLanguage = language.name,
                    humanLanguage = settings.humanLanguage,
                ).text ?: ""
            }
            val wrapping = StringUtil.lineWrapping(newText.trim(), 120)
            val numberOfLines = wrapping.trim().split("\n")
                .dropLastWhile { it.isEmpty() }.toTypedArray<String>().size
            val commentStyle =
                if (numberOfLines == 1) {
                    language.lineComment
                } else {
                    language.blockComment
                }
            UITools.writeableFn(event) {
                UITools.replaceString(
                    editor.document,
                    selectionStart,
                    selectionEnd,
                    """
                    $indent${commentStyle.fromString(wrapping)!!.withIndent(indent)}
                    $indent$selectedText
                    """.trimIndent()
                )
            }
        }
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        return computerLanguage != ComputerLanguage.Text
    }

}