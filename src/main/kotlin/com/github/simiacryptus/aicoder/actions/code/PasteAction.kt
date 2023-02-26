package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.DataFlavor
import java.util.*

/**
 * The PasteAction class is an action that is used to paste text into an IntelliJ editor with GPT translation.
 * The action first checks if there is any text in the clipboard,
 * and if there is, it will attempt to translate the text into the language of the current editor.
 * If the text is successfully translated, it will be inserted into the editor at the current cursor position.
 * If there is already text selected, the translated text will replace the selected text.
 */
class PasteAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectionStart = primaryCaret.selectionStart
        val selectionEnd = primaryCaret.selectionEnd
        val selectedText = primaryCaret.selectedText
        val language = ComputerLanguage.getComputerLanguage(event)!!.name
        val text =
            CopyPasteManager.getInstance().getContents<Any>(DataFlavor.stringFlavor)!!.toString()
                .trim { it <= ' ' }
        val request = AppSettingsState.getInstance().createTranslationRequest()
            .setInputType("source")
            .setOutputType("translated")
            .setInstruction("Translate this input into $language")
            .setInputAttribute("language", "autodetect")
            .setOutputAttrute("language", language)
            .setInputText(text)
            .buildCompletionRequest()
        val caret = event.getData(CommonDataKeys.CARET)
        val indent = UITools.getIndent(caret)
        UITools.redoableRequest(
            request, indent, event
        ) { newText: CharSequence? ->
            if (selectedText == null) {
                return@redoableRequest UITools.insertString(editor.document, selectionStart, newText!!)
            } else {
                return@redoableRequest UITools.replaceString(
                    editor.document, selectionStart, selectionEnd,
                    newText!!
                )
            }
        }
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            return if (CopyPasteManager.getInstance()
                    .getContents<Any?>(DataFlavor.stringFlavor) == null
            ) false else {
                val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
                computerLanguage != ComputerLanguage.Text
            }
        }
    }
}