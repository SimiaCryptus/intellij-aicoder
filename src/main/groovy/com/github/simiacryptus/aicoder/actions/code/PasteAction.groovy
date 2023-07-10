package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.openai.proxy.ChatProxy
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

class PasteAction extends SelectionAction {
    PasteAction() {
        super(false)
    }

    interface VirtualAPI {
        ConvertedText convert(String text, String from_language, String to_language)
        public class ConvertedText {
            public String code
            public String language
            public ConvertedText() {}
        }
    }

    @Override
    String processSelection(SelectionState state) {
        return new ChatProxy<VirtualAPI>(
            clazz: VirtualAPI.class,
            api: api,
            model: AppSettingsState.instance.defaultChatModel(),
            temperature: AppSettingsState.instance.temperature,
            deserializerRetries: 5,
        ).create().convert(
            getClipboard().toString().trim(),
            "autodetect",
            state.language.name()
        ).code ?: ""
    }

    @Override
    boolean isLanguageSupported(ComputerLanguage computerLanguage) {
        if (computerLanguage == null) return false
        return computerLanguage != ComputerLanguage.Text
    }

    @Override
    boolean isEnabled(AnActionEvent event) {
        if (getClipboard() == null) return false
        return super.isEnabled(event)
    }

    private Object getClipboard() {
        return Toolkit.getDefaultToolkit().systemClipboard.getContents(null)?.getTransferData(DataFlavor.stringFlavor)
    }
}