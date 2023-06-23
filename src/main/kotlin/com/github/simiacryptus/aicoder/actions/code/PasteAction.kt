package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.simiacryptus.openai.proxy.ChatProxy
import java.awt.datatransfer.DataFlavor

/**
 * The PasteAction class is an action that is used to paste text into an IntelliJ editor with GPT translation.
 * The action first checks if there is any text in the clipboard,
 * and if there is, it will attempt to translate the text into the language of the current editor.
 * If the text is successfully translated, it will be inserted into the editor at the current cursor position.
 * If there is already text selected, the translated text will replace the selected text.
 */
class PasteAction : BaseAction() {
    interface VirtualAPI {
        fun convert(text: String, from_language: String, to_language: String): ConvertedText
        data class ConvertedText(
            val code: String? = null,
            val language: String? = null
        )
    }

    val proxy: VirtualAPI
        get() = ChatProxy(
            clazz = VirtualAPI::class.java,
            api = api,
            model = AppSettingsState.instance.defaultChatModel(),
            deserializerRetries = 5,
        ).create()

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        val selectionStart = primaryCaret.selectionStart
        val selectionEnd = primaryCaret.selectionEnd
        val selectedText = primaryCaret.selectedText
        val language = ComputerLanguage.getComputerLanguage(event)!!.name
        val text =
            CopyPasteManager.getInstance().getContents<Any>(DataFlavor.stringFlavor)!!.toString()
                .trim { it <= ' ' }

        UITools.redoableTask(event) {
            val newText = UITools.run(
                event.project, "Converting for Paste", true
            ) {
                proxy.convert(text = text, "autodetect", language).code ?: ""
            }
            UITools.writeableFn(event) {
                if (selectedText == null) {
                    UITools.insertString(editor.document, selectionStart, newText)
                } else {
                    UITools.replaceString(
                        editor.document, selectionStart, selectionEnd,
                        newText
                    )
                }
            }
        }
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        return if (CopyPasteManager.getInstance()
                .getContents<Any?>(DataFlavor.stringFlavor) == null
        ) false else {
            val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
            computerLanguage != ComputerLanguage.Text
        }
    }

}


