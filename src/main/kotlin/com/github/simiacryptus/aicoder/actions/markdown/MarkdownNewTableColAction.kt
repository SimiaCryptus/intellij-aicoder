package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.CompletionRequest
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
import javax.swing.JOptionPane

class MarkdownNewTableColAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val markdownNewTableColParams = getMarkdownNewTableColParams(event)
        val settings = AppSettingsState.getInstance()
        val columnName: CharSequence =
            JOptionPane.showInputDialog(null, "Column Name:", "Add Column", JOptionPane.QUESTION_MESSAGE)
                .trim { it <= ' ' }
        val request = newRowsRequest(
            settings, Objects.requireNonNull(markdownNewTableColParams)!!.n, markdownNewTableColParams!!.rows,
            "| $columnName | "
        )
        val document = event.getRequiredData(CommonDataKeys.EDITOR).document
        val textRange = markdownNewTableColParams.table.textRange
        val startOffset = textRange.startOffset
        val endOffset = textRange.endOffset
        UITools.redoableRequest(request, "", event,
            { newText: CharSequence? ->
                transformCompletion(
                    markdownNewTableColParams, newText, columnName
                )
            }
        ) { newText: CharSequence? ->
            UITools.replaceString(
                document, startOffset, endOffset,
                newText!!
            )
        }
    }

    class MarkdownNewTableColParams constructor(
        val caret: Caret,
        val table: PsiElement,
        val rows: List<CharSequence>,
        val n: CharSequence
    )

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            return if (ComputerLanguage.Markdown !== computerLanguage) false else null != getMarkdownNewTableColParams(e)
        }

        fun getMarkdownNewTableColParams(e: AnActionEvent): MarkdownNewTableColParams? {
            val caret = e.getData(CommonDataKeys.CARET)
                ?: return null
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)
                ?: return null
            val table = PsiUtil.getSmallestIntersecting(
                psiFile,
                caret.selectionStart,
                caret.selectionEnd,
                "MarkdownTableImpl"
            )
                ?: return null
            val rows = Arrays.asList<CharSequence>(*StringTools.transposeMarkdownTable(
                PsiUtil.getAll(table, "MarkdownTableRowImpl")
                    .stream().map { obj: PsiElement -> obj.text }.collect(Collectors.joining("\n")), false, false
            ).split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
            val n: CharSequence = Integer.toString(rows.size * 2)
            return MarkdownNewTableColParams(caret, table, rows, n)
        }

        fun newRowsRequest(
            settings: AppSettingsState,
            n: CharSequence,
            rows: List<CharSequence>,
            rowPrefix: CharSequence
        ): CompletionRequest {
            return settings.createTranslationRequest()
                .setInstruction(UITools.getInstruction("Output $n rows"))
                .setInputType("instruction")
                .setInputText("Output $n rows")
                .setOutputType("markdown")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest()
                .appendPrompt(
                    """
                    
                    ${java.lang.String.join("\n", rows)}
                    $rowPrefix
                    """.trimIndent()
                )
        }

        private fun transformCompletion(
            markdownNewTableColParams: MarkdownNewTableColParams,
            complete: CharSequence?,
            columnName: CharSequence
        ): String {
            var complete = complete
            complete = "| $columnName | $complete"
            val indent = UITools.getIndent(markdownNewTableColParams.caret)
            val newRows = Arrays.stream(("" + complete).split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
                .map { obj: String -> obj.trim { it <= ' ' } }.filter { x: String -> x.length > 0 }
                .collect(Collectors.toList())
            val newTableTxt = StringTools.transposeMarkdownTable(
                Stream.concat(
                    markdownNewTableColParams.rows.stream(),
                    newRows.stream()
                ).collect(Collectors.joining("\n")), false, true
            )
            return newTableTxt.replace("\n", "\n$indent")
        }
    }
}