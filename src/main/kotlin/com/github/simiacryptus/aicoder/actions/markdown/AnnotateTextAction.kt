package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*

/**
 * The RecentCodeEditsAction is an IntelliJ action that allows users to quickly access and apply recent code edits.
 * This action is triggered when a user selects a piece of code and then right-clicks to bring up the context menu.
 * The RecentCodeEditsAction will then display a list of recent code edits that the user can select from and apply to the selected code.
 * When the user selects a code edit, the action will generate a new version of the selected code with the code edit applied.
 * Finally, the new version of the code will be inserted into the document, replacing the original code.
 */
class AnnotateTextAction : ActionGroup() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val children = ArrayList<AnAction>()
        for (encoding in listOf(
            "Universal Dependencies (UD)",
            "Penn Treebank (PTB)",
            "Stanford Dependencies (SD)",
            "CoNLL-X",
            "ISO 24613:2008"
        )) {
            children.add(object : AnAction(encoding, encoding, null) {
                override fun actionPerformed(event: AnActionEvent) {
                    val computerLanguage = ComputerLanguage.getComputerLanguage(event)!!.name
                    val editor = event.getRequiredData(CommonDataKeys.EDITOR)
                    val caretModel = editor.caretModel
                    val primaryCaret = caretModel.primaryCaret
                    val selectionStart = primaryCaret.selectionStart
                    val selectionEnd = primaryCaret.selectionEnd
                    val selectedText = primaryCaret.selectedText
                    val settings = AppSettingsState.getInstance()
                    settings.addInstructionToHistory(encoding)
                    val humanLanguage = AppSettingsState.getInstance().humanLanguage
                    val request = settings.createTranslationRequest()
                        .setInstruction("Parse and output as $encoding")
                        .setInputType("text")
                        .setOutputAttrute("format", computerLanguage)
                        .setInputText(IndentedText.fromString(selectedText).textBlock)
                        .setOutputType("parsed")
                        .setOutputAttrute("format", encoding)
                        .buildCompletionRequest()
                    val caret = event.getData(CommonDataKeys.CARET)
                    val indent = UITools.getIndent(caret)
                    val document = editor.document
                    UITools.redoableRequest(
                        request, indent, event
                    ) { newText: CharSequence? ->
                        UITools.replaceString(
                            document, selectionStart, selectionEnd,
                            newText!!
                        )
                    }
                }
            })
        }
        return children.toTypedArray()
    }
    private fun isEnabled(e: AnActionEvent): Boolean {
        if (!UITools.hasSelection(e)) return false
        if(!setOf(
                ComputerLanguage.Markdown,
                ComputerLanguage.Text
            ).contains(ComputerLanguage.getComputerLanguage(e))) return false
        return null != ComputerLanguage.getComputerLanguage(e)
    }
}