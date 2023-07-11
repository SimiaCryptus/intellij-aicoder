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

abstract class SelectionAction(
    val requiresSelection: Boolean = true
) : BaseAction() {

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
            var (start, end) = defaultSelection(editorState, primaryCaret.offset)
            if (start >= end && requiresSelection) return
            start = start.coerceAtLeast(0)
            end = end.coerceAtLeast(start).coerceAtMost(editorState.text.length - 1)
            selectedText = editorState.text.substring(start, end)
            selectionEnd = start
            selectionStart = end
        } else {
            val editorState = editorState(editor)
            var (start, end) = editSelection(editorState, selectionStart, selectionEnd)
            if (start >= end && requiresSelection) return
            start = start.coerceAtLeast(0)
            end = end.coerceAtLeast(start).coerceAtMost(editorState.text.length - 1)
            selectedText = editorState.text.substring(start, end)
            selectionEnd = start
            selectionStart = end
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
        val line: Pair<Int,Int>,
        val psiFile: PsiFile?,
        val contextRanges : Array<ContextRange> = arrayOf(),
    )

    data class ContextRange(
        val name: String,
        val start: Int,
        val end: Int
    )

    private fun editorState(editor: Editor): EditorState {
        val document = editor.document
        val lineNumber = document.getLineNumber(editor.caretModel.offset)
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
        val psiFile = if (virtualFile == null) {
            null
        } else {
            PsiManager.getInstance(editor.project!!).findFile(virtualFile)
        }
        val contextRanges = mutableListOf<ContextRange>()
        psiFile?.acceptChildren(object : com.intellij.psi.PsiRecursiveElementVisitor() {
            override fun visitElement(element: com.intellij.psi.PsiElement) {
                val start = element.textRange.startOffset
                val end = element.textRange.endOffset
                if (start <= editor.caretModel.offset && end >= editor.caretModel.offset) {
                    contextRanges.add(ContextRange(element.javaClass.simpleName, start, end))
                }
                super.visitElement(element)
            }
        })
        return EditorState(
            text = document.text,
            cursorOffset = editor.caretModel.offset,
            line = Pair(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)),
            psiFile = psiFile,
            contextRanges = contextRanges.toTypedArray()
        )
    }


    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!super.isEnabled(event)) return false
        if (UITools.isSanctioned()) return false
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return false
        if (requiresSelection) {
            if (editor.caretModel.primaryCaret.selectedText.isNullOrEmpty()) {
                val editorState = editorState(editor)
                val (start, end) = defaultSelection(editorState, editorState.cursorOffset)
                if (start >= end) return false
            }
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

    open fun defaultSelection(editorState: EditorState, offset: Int) = editorState.line

    open fun editSelection(state: EditorState, start: Int, end: Int) = Pair(start, end)

    abstract fun processSelection(state: SelectionState): String

}
