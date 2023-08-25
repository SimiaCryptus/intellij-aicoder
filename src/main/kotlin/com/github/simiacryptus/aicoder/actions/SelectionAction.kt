package com.github.simiacryptus.aicoder.actions

import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor

abstract class SelectionAction<T : Any>(
    val requiresSelection: Boolean = true
) : BaseAction() {

    open fun getConfig(project: Project?): T? = null

    fun retarget(
        editorState: EditorState,
        selectedText: @NlsSafe String?,
        selectionStart: Int,
        selectionEnd: Int
    ): Pair<Int, Int>? {
        if (selectedText.isNullOrEmpty()) {
            var (start, end) = defaultSelection(editorState, selectionStart)
            if (start >= end && requiresSelection) return null
            start = start.coerceAtLeast(0)
            end = end.coerceAtLeast(start).coerceAtMost(editorState.text.length - 1)
            return Pair(start, end)
        } else {
            var (start, end) = editSelection(editorState, selectionStart, selectionEnd)
            if (start >= end && requiresSelection) return null
            start = start.coerceAtLeast(0)
            end = end.coerceAtLeast(start).coerceAtMost(editorState.text.length - 1)
            return Pair(start, end)
        }
    }

    final override fun handle(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val config = getConfig(event.project)
        val indent = UITools.getIndent(event)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        var selectionStart = primaryCaret.selectionStart
        var selectionEnd = primaryCaret.selectionEnd
        var selectedText = primaryCaret.selectedText
        val editorState = editorState(editor)
        val (start, end) = retarget(editorState, selectedText, selectionStart, selectionEnd) ?: return
        selectedText = editorState.text.substring(start, end)
        selectionEnd = end
        selectionStart = start

        UITools.redoableTask(event) {
            val document = event.getData(CommonDataKeys.EDITOR)?.document
            var rangeMarker: RangeMarker?  = null
            WriteCommandAction.runWriteCommandAction(event.project) {
                rangeMarker = document?.createGuardedBlock(selectionStart, selectionEnd)
            }

            val newText = try {
                processSelection(
                    event = event,
                    SelectionState(
                        selectedText = selectedText,
                        selectionOffset = selectionStart,
                        selectionLength = selectionEnd - selectionStart,
                        entireDocument = editor.document.text,
                        language = ComputerLanguage.getComputerLanguage(event),
                        indent = indent,
                        contextRanges = editorState.contextRanges,
                        psiFile = editorState.psiFile,
                    ),
                    config = config
                )
            } finally {
                if(null != rangeMarker)
                    WriteCommandAction.runWriteCommandAction(event.project) {
                        document?.removeGuardedBlock(rangeMarker!!)
                    }
            }
            UITools.writeableFn(event) {
                UITools.replaceString(editor.document, selectionStart, selectionEnd, newText)
            }
        }
    }

    data class EditorState(
        val text: @NlsSafe String,
        val cursorOffset: Int,
        val line: Pair<Int, Int>,
        val psiFile: PsiFile?,
        val contextRanges: Array<ContextRange> = arrayOf(),
    )

    data class ContextRange(
        val name: String,
        val start: Int,
        val end: Int
    ) {
        fun length() = end - start
        fun range() = Pair(start, end)

        fun subString(text: String) = text.substring(start, end)
    }

    private fun editorState(editor: Editor): EditorState {
        val document = editor.document
        val lineNumber = document.getLineNumber(editor.caretModel.offset)
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
        val psiFile = if (virtualFile == null) {
            null
        } else {
            PsiManager.getInstance(editor.project!!).findFile(virtualFile)
        }
        return EditorState(
            text = document.text,
            cursorOffset = editor.caretModel.offset,
            line = Pair(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)),
            psiFile = psiFile,
            contextRanges = contextRanges(psiFile, editor)
        )
    }

    fun contextRanges(
        psiFile: PsiFile?,
        editor: Editor
    ): Array<ContextRange> {
        val contextRanges = mutableListOf<ContextRange>()
        psiFile?.acceptChildren(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val start = element.textRange.startOffset
                val end = element.textRange.endOffset
                if (start <= editor.caretModel.offset && end >= editor.caretModel.offset) {
                    contextRanges.add(ContextRange(element.javaClass.simpleName, start, end))
                }
                super.visitElement(element)
            }
        })
        return contextRanges.toTypedArray()
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
        val selectedText: String? = null,
        val selectionOffset: Int = 0,
        val selectionLength: Int? = null,
        val entireDocument: String? = null,
        val language: ComputerLanguage? = null,
        val indent: CharSequence? = null,
        val contextRanges: Array<ContextRange> = arrayOf(),
        val psiFile: PsiFile?,
    )

    open fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        computerLanguage ?: return false
        return true
    }

    open fun defaultSelection(editorState: EditorState, offset: Int) = editorState.line

    open fun editSelection(state: EditorState, start: Int, end: Int) = Pair(start, end)


    open fun processSelection(
        event: AnActionEvent?,
        selectionState: SelectionState,
        config: T?
    ): String {
        return UITools.run(event?.project, templateText ?: "", true) {
            processSelection(selectionState, config)
        }
    }

    open fun processSelection(state: SelectionState, config: T?): String {
        throw NotImplementedError()
    }

}
