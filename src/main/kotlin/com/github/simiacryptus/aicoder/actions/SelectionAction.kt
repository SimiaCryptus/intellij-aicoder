package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.simiacryptus.openai.APIClientBase

abstract class SelectionAction : BaseAction() {

    final override fun handle(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val indent = UITools.getIndent(event)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        var selectionStart = primaryCaret.selectionStart
        var selectionEnd = primaryCaret.selectionEnd
        var selectedText = primaryCaret.selectedText

        if (selectedText.isNullOrEmpty()) {
            val editorState = editorState(editor)
            val (start, end) = defaultSelection(editorState)
            if (start >= end) return
            selectedText = editorState.text.substring(start, end)
            selectionEnd = start
            selectionStart = end
        } else {
            val editorState = editorState(editor)
            val (start, end) = editSelection(editorState, selectionStart, selectionEnd)
            if (start >= end) return
            selectedText = editorState.text.substring(start, end)
        }

        val language = ComputerLanguage.getComputerLanguage(event)
        UITools.redoableTask(event) {
            val newText = UITools.run(event.project, this.templateText!!, true) {
                processSelection(
                    SelectionState(
                        selectedText = selectedText,
                        language = language,
                        indent = indent
                    )
                )
            }
            UITools.writeableFn(event) {
                UITools.replaceString(editor.document, selectionStart, selectionEnd, newText)
            }
        }
    }

    data class EditorState(
        val text: @NlsSafe String,
        val cursorOffset: Int,
        val lineStartOffset: Int,
        val lineEndOffset: Int,
        val psiFile: PsiFile?
    )

    private fun editorState(editor: Editor): EditorState {
        val document = editor.document
        val lineNumber = document.getLineNumber(editor.caretModel.offset)
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
        return EditorState(
            document.text,
            editor.caretModel.offset,
            document.getLineStartOffset(lineNumber),
            document.getLineEndOffset(lineNumber),
            if (virtualFile == null) {
                null
            } else {
                PsiManager.getInstance(editor.project!!).findFile(virtualFile)
            }
        )
    }

    final override fun isEnabled(event: AnActionEvent): Boolean {
        if (APIClientBase.isSanctioned()) return false

        val editor = event.getData(CommonDataKeys.EDITOR) ?: return false
        if (editor.caretModel.primaryCaret.selectedText.isNullOrEmpty()) {
            val editorState = editorState(editor)
            val (start, end) = defaultSelection(editorState)
            if (start >= end) return false
        }

        val computerLanguage = ComputerLanguage.getComputerLanguage(event)
        return isLanguageSupported(computerLanguage)
    }

    data class SelectionState(
        val selectedText: String?,
        val language: ComputerLanguage?,
        val indent: CharSequence?
    )

    open fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        computerLanguage ?: return false
        return true
    }

    open fun defaultSelection(editorState: EditorState) =
        Pair(editorState.lineEndOffset, editorState.lineStartOffset)

    open fun editSelection(state: EditorState, start: Int, end: Int) = Pair(start, end)

    abstract fun processSelection(state: SelectionState): String

}
