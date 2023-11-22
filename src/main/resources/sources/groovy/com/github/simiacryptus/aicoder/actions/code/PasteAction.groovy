package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy
import org.jetbrains.annotations.Nullable

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

class PasteAction extends SelectionAction<String> {
    PasteAction() {
        super(false)
    }

    interface VirtualAPI {
        ConvertedText convert(String text, String from_language, String to_language)

        class ConvertedText {
            public String code
            public String language

            ConvertedText() {}
        }
    }
    @Override
    String getConfig(@Nullable Project project) {
        return ""
    }


    @Override
    String processSelection(SelectionState state, String config) {
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
        def contents = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
        if (contents?.isDataFlavorSupported(DataFlavor.stringFlavor) == true) contents?.getTransferData(DataFlavor.stringFlavor)
        else null
    }
}