package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools.getIndent
import com.github.simiacryptus.aicoder.util.UITools.getInstruction
import com.github.simiacryptus.aicoder.util.UITools.insertString
import com.github.simiacryptus.aicoder.util.UITools.redoableRequest
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getAll
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getSmallestIntersecting
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.psi.PsiElement
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * The MarkdownListAction class is an action that allows users to quickly expand a list of items in IntelliJ.
 * It is triggered when the user selects a list in the markdown editor and then invokes the action.
 * The action will then use current list items to generate further items via OpenAI's GPT-3 API.
 * These new items will be inserted into the document at the end of the list.
 */
class MarkdownListAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val markdownListParams = getMarkdownListParams(event)
        val settings = AppSettingsState.getInstance()
        val items = StringTools.trim(
            getAll(
                Objects.requireNonNull(markdownListParams)!!.list, "MarkdownListItemImpl"
            )
                .stream().map { item: PsiElement? ->
                    getAll(
                        item!!, "MarkdownParagraphImpl"
                    )[0].text
                }.collect(Collectors.toList()), 10, false
        )
        val indent = getIndent(markdownListParams!!.caret)
        val n: CharSequence = Integer.toString(items.size * 2)
        val endOffset = markdownListParams.list.textRange.endOffset
        val listPrefix = "* "
        val completionRequest = settings.createTranslationRequest()
            .setInstruction(getInstruction("List $n items"))
            .setInputType("instruction")
            .setInputText("List $n items")
            .setOutputType("list")
            .setOutputAttrute("style", settings.style)
            .buildCompletionRequest()
            .appendPrompt(
                """
                ${
                    items.stream().map { x2: CharSequence? -> listPrefix + x2 }
                        .collect(Collectors.joining("\n"))
                }
                $listPrefix
                """.trimIndent()
            )
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        redoableRequest(completionRequest, "", event,
            Function { newText: CharSequence? ->
                transformCompletion(
                    markdownListParams, indent, listPrefix, newText!!
                )
            },
            Function { newText: CharSequence? ->
                insertString(
                    document, endOffset,
                    newText!!
                )
            })
    }

    class MarkdownListParams constructor(val caret: Caret, val list: PsiElement)
    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            return if (ComputerLanguage.Markdown != computerLanguage) false else null != getMarkdownListParams(e)
        }

        fun getMarkdownListParams(e: AnActionEvent): MarkdownListParams? {
            val caret = e.getData(CommonDataKeys.CARET)
                ?: return null
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)
                ?: return null
            val list = getSmallestIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, "MarkdownListImpl")
                ?: return null
            return MarkdownListParams(caret, list)
        }

        private fun transformCompletion(
            markdownListParams: MarkdownListParams,
            indent: CharSequence,
            listPrefix: String,
            complete: CharSequence
        ): String {
            val newItems = Arrays.stream(complete.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()).map { obj: String -> obj.trim { it <= ' ' } }
                .filter { x1: String -> x1.length > 0 }.map { x1: String? ->
                    StringTools.stripPrefix(
                        x1!!, listPrefix
                    )
                }.collect(Collectors.toList())
            val strippedList =
                Arrays.stream(markdownListParams.list.text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
                    .map { obj: String -> obj.trim { it <= ' ' } }.filter { x: String -> x.length > 0 }
                    .collect(Collectors.joining("\n"))
            val bulletString = Stream.of("- [ ] ", "- ", "* ")
                .filter { prefix: String? -> strippedList.startsWith(prefix!!) }.findFirst().orElse("1. ")
            val itemText: CharSequence =
                indent.toString() + newItems.stream().map { x: CharSequence -> bulletString + x }
                    .collect(
                        Collectors.joining(
                            """
                         
                         $indent
                         """.trimIndent()
                        )
                    )
            return """
                
                $itemText
                """.trimIndent()
        }
    }
}


