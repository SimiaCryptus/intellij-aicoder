package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.psi.PsiElement
import java.util.*
import java.util.stream.Collectors

class MarkdownNewTableRowsAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val markdownNewTableRowsParams = getMarkdownNewTableRowsParams(event)
        val rows = StringTools.trim(
            PsiUtil.getAll(
                markdownNewTableRowsParams!!.table, "MarkdownTableRowImpl"
            ).stream().map { obj: PsiElement -> obj.text!! }.collect(Collectors.toList()), 10, true
        )
        val n: CharSequence = Integer.toString(rows.size * 2)
        val settings = AppSettingsState.instance
        val endOffset = markdownNewTableRowsParams.table.textRange.endOffset
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        UITools.redoableRequest(
            MarkdownNewTableColAction.newRowsRequest(settings, n, rows, ""),
            "",
            event,
            { transformCompletion(markdownNewTableRowsParams, it) },
            { UITools.insertString(document, endOffset, it) })
    }

    class MarkdownNewTableRowsParams constructor(val caret: Caret, val table: PsiElement)
    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if (UITools.isSanctioned()) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            return if (ComputerLanguage.Markdown !== computerLanguage) false else null != getMarkdownNewTableRowsParams(
                e
            )
        }

        fun getMarkdownNewTableRowsParams(e: AnActionEvent): MarkdownNewTableRowsParams? {
            val caret = e.getData(CommonDataKeys.CARET)
            if (null != caret) {
                val psiFile = e.getData(CommonDataKeys.PSI_FILE)
                if (null != psiFile) {
                    val table =
                        PsiUtil.getSmallestIntersecting(
                            psiFile,
                            caret.selectionStart,
                            caret.selectionEnd,
                            "MarkdownTableImpl"
                        )
                    if (null != table) {
                        return MarkdownNewTableRowsParams(caret, table)
                    }
                }
            }
            return null
        }

        private fun transformCompletion(
            markdownNewTableRowsParams: MarkdownNewTableRowsParams,
            complete: CharSequence?
        ): String {
            val indent = UITools.getIndent(markdownNewTableRowsParams.caret)
            val newRows = Arrays.stream(("" + complete).split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
                .map { obj: String -> obj.trim { it <= ' ' } }.filter { x: String -> x.length > 0 }
                .collect(Collectors.toList())
            return "\n$indent" + newRows.stream().collect(Collectors.joining("\n$indent"))
        }
    }
}