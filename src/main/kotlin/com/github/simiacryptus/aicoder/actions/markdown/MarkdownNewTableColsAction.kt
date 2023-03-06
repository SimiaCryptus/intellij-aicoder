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
import java.util.stream.Stream

class MarkdownNewTableColsAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val markdownNewTableColsParams = getMarkdownNewTableColsParams(event)
        val settings = AppSettingsState.getInstance()
        val indent = UITools.getIndent(Objects.requireNonNull(markdownNewTableColsParams)!!.caret)
        val request = MarkdownNewTableColAction.newRowsRequest(
            settings,
            markdownNewTableColsParams!!.n,
            markdownNewTableColsParams.rows,
            ""
        )
        UITools.redoableRequest(request, "", event,
            { newText ->
                transformCompletion(
                    markdownNewTableColsParams, indent, newText
                )
            },
            { newText ->
                UITools.replaceString(
                    event.getRequiredData(CommonDataKeys.EDITOR)
                        .document,
                    markdownNewTableColsParams.table.textRange.startOffset,
                    markdownNewTableColsParams.table.textRange.endOffset, newText!!
                )
            }
        )
    }

    class MarkdownNewTableColsParams constructor(
        val caret: Caret,
        val table: PsiElement,
        val rows: List<CharSequence>,
        val n: CharSequence
    )

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            if(UITools.isSanctioned()) return false
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            return if (ComputerLanguage.Markdown !== computerLanguage) false else null != getMarkdownNewTableColsParams(
                    e
                )
        }

        fun getMarkdownNewTableColsParams(e: AnActionEvent): MarkdownNewTableColsParams? {
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
                        val rows =
                            Arrays.asList<CharSequence>(*StringTools.transposeMarkdownTable(
                                PsiUtil.getAll(table, "MarkdownTableRowImpl")
                                    .stream().map { obj: PsiElement -> obj.text }.collect(Collectors.joining("\n")),
                                false,
                                false
                            ).split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray())
                        val n: CharSequence = Integer.toString(rows.size * 2)
                        return MarkdownNewTableColsParams(caret, table, rows, n)
                    }
                }
            }
            return null
        }

        private fun transformCompletion(
            markdownNewTableColsParams: MarkdownNewTableColsParams,
            indent: CharSequence,
            complete: CharSequence?
        ): String {
            val newRows = Arrays.stream(("" + complete).split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()).map { obj: String -> obj.trim { it <= ' ' } }
                .filter { x: String -> x.length > 0 }.collect(Collectors.toList())
            val newTableTxt = StringTools.transposeMarkdownTable(
                Stream.concat(markdownNewTableColsParams.rows.stream(), newRows.stream())
                    .collect(Collectors.joining("\n")), false, true
            )
            return newTableTxt.replace("\n", "\n$indent")
        }
    }
}