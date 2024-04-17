package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

open class PasteAction : SelectionAction<String>(false) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    interface VirtualAPI {
        fun convert(text: String, from_language: String, to_language: String): ConvertedText

        class ConvertedText {
            var code: String? = null
            var language: String? = null
        }
    }

    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun processSelection(state: SelectionState, config: String?): String {
        return ChatProxy(
            VirtualAPI::class.java,
            api,
            AppSettingsState.instance.smartModel.chatModel(),
            AppSettingsState.instance.temperature,
        ).create().convert(
            getClipboard().toString().trim(),
            "autodetect",
            state.language?.name ?: ""
        ).code ?: ""
    }

    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        if (computerLanguage == null) return false
        return computerLanguage != ComputerLanguage.Text
    }

    override fun isEnabled(event: AnActionEvent): Boolean {
        if (!hasClipboard()) return false
        return super.isEnabled(event)
    }

    private fun hasClipboard() = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)?.let { contents ->
        return when {
            contents.isDataFlavorSupported(DataFlavor.stringFlavor) -> true
            contents.isDataFlavorSupported(DataFlavor.getTextPlainUnicodeFlavor()) -> true
            else -> false
        }
    } ?: false

    private fun getClipboard(): Any? = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)?.let { contents ->
        return when {
            contents.isDataFlavorSupported(DataFlavor.stringFlavor) -> contents.getTransferData(DataFlavor.stringFlavor)
            contents.isDataFlavorSupported(DataFlavor.getTextPlainUnicodeFlavor()) -> contents.getTransferData(
                DataFlavor.getTextPlainUnicodeFlavor()
            )

            else -> null
        }
    }
}