package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.getIndent
import com.github.simiacryptus.aicoder.util.UITools.insertString
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getAll
import com.github.simiacryptus.aicoder.util.psi.PsiUtil.getSmallestIntersecting
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil

class MarkdownListAction : BaseAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    interface ListAPI {
        fun newListItems(
            items: List<String?>?,
            count: Int,
        ): Items

        data class Items(
            val items: List<String?>? = null,
        )
    }

    val proxy: ListAPI
        get() {
            val chatProxy = ChatProxy(
                clazz = ListAPI::class.java,
                api = api,
                model = AppSettingsState.instance.smartModel.chatModel(),
                deserializerRetries = 5,
            )
            chatProxy.addExample(
                returnValue = ListAPI.Items(
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

    override fun handle(e: AnActionEvent) {
        val caret = e.getData(CommonDataKeys.CARET) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val list =
            getSmallestIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, "MarkdownListImpl") ?: return
        val items = StringUtil.trim(
            getAll(list, "MarkdownListItemImpl")
                .map {
                    val all = getAll(it, "MarkdownParagraphImpl")
                    if (all.isEmpty()) it.text else all[0].text
                }.toList(), 10, false
        )
        val indent = getIndent(caret)
        val endOffset = list.textRange.endOffset
        val bulletTypes = listOf("- [ ] ", "- ", "* ")
        val document = (e.getData(CommonDataKeys.EDITOR) ?: return).document
        val rawItems = items.map(CharSequence::trim).map {
            val bulletType = bulletTypes.find(it::startsWith)
            if (null != bulletType) StringUtil.stripPrefix(it, bulletType).toString()
            else it.toString()
        }

        UITools.redoableTask(e) {
            var newItems: List<String?>? = null
            UITools.run(
                e.project, "Generating New Items", true
            ) {
                newItems = proxy.newListItems(
                    rawItems,
                    (items.size * 2)
                ).items
            }
            var newList = ""
            ApplicationManager.getApplication().runReadAction {
                val strippedList = list.text.split("\n")
                    .map(String::trim).filter(String::isNotEmpty)
                    .joinToString("\n")
                val bulletString = bulletTypes.find(strippedList::startsWith) ?: "1. "
                newList = newItems?.joinToString("\n") { indent.toString() + bulletString + it } ?: ""
            }
            UITools.writeableFn(e) {
                insertString(document, endOffset, "\n" + newList)
            }
        }
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        if (ComputerLanguage.Markdown != computerLanguage) return false
        val caret = event.getData(CommonDataKeys.CARET) ?: return false
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return false
        getSmallestIntersecting(psiFile, caret.selectionStart, caret.selectionEnd, "MarkdownListImpl") ?: return false
        return true
    }
}



