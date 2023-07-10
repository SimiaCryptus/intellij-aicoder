package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.getIndent
import com.github.simiacryptus.aicoder.util.UITools.insertString
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getAll
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getSmallestIntersecting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.simiacryptus.openai.APIClientBase
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil

/**
 * The MarkdownListAction class is an action that allows users to quickly expand a list of items in IntelliJ.
 * It is triggered when the user selects a list in the markdown editor and then invokes the action.
 * The action will then use current list items to generate further items via OpenAI's GPT-3 API.
 * These new items will be inserted into the document at the end of the list.
 */
class MarkdownListAction : BaseAction() {

    interface VirtualAPI {
        fun newListItems(
            items: List<String?>?,
            count: Int,
        ): Items

        data class Items(
            val items: List<String?>? = null,
        )
    }

    val proxy: VirtualAPI
        get() {
            val chatProxy = ChatProxy(
                clazz = VirtualAPI::class.java,
                api = api,
                model = AppSettingsState.instance.defaultChatModel(),
                deserializerRetries = 5,
            )
            chatProxy.addExample(
                returnValue = VirtualAPI.Items(
                    items = listOf("Item 4", "Item 5", "Item 6")
                )
            ) {
                it.newListItems(
                    items = listOf("Item 1", "Item 2", "Item 3"),
                    count = 6
                )
            }
            return chatProxy.create()
        }

    override fun handle(event: AnActionEvent) {
        val caret = event.getData(CommonDataKeys.CARET) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val list =
            getSmallestIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, "MarkdownListImpl") ?: return
        val items = StringUtil.trim(
            getAll(list, "MarkdownListItemImpl")
                .map {
                    getAll(it, "MarkdownParagraphImpl")[0].text
                }.toList(), 10, false
        )
        val indent = getIndent(caret)
        val endOffset = list.textRange.endOffset
        val bulletTypes = listOf("- [ ] ", "- ", "* ")
        val document = (event.getData(CommonDataKeys.EDITOR) ?: return).document
        val rawItems = items.map(CharSequence::trim).map {
            val bulletType = bulletTypes.find(it::startsWith)
            if (null != bulletType) StringUtil.stripPrefix(it, bulletType).toString()
            else it.toString()
        }

        UITools.redoableTask(event) {
            val newItems = UITools.run(
                event.project, "Generating New Items", true
            ) {
                proxy.newListItems(
                    rawItems,
                    (items.size * 2)
                ).items
            }
            val strippedList = list.text.split("\n")
                .map(String::trim).filter(String::isNotEmpty)
                .joinToString("\n")
            val bulletString = bulletTypes.find(strippedList::startsWith) ?: "1. "
            val newList = newItems?.joinToString("\n") { indent.toString() + bulletString + it } ?: ""
            UITools.writeableFn(event) {
                insertString(document, endOffset, "\n" + newList)
            }
        }
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (APIClientBase.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        if (ComputerLanguage.Markdown != computerLanguage) return false
        val caret = event.getData(CommonDataKeys.CARET) ?: return false
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return false
        getSmallestIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, "MarkdownListImpl") ?: return false
        return true
    }
}



