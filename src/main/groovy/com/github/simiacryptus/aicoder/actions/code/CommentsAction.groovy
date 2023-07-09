package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.openai.proxy.ChatProxy

class CommentsAction extends SelectionAction {

    boolean isLanguageSupported(ComputerLanguage computerLanguage) {
        if (computerLanguage == null) return false
        return computerLanguage != ComputerLanguage.Text
    }

    String processSelection(SelectionState state) {
        return new ChatProxy<CommentsAction_VirtualAPI>(
                clazz: CommentsAction_VirtualAPI.class,
                api: api,
                temperature: AppSettingsState.instance.temperature,
                model: AppSettingsState.instance.defaultChatModel(),
                deserializerRetries: 5,
        ).create().editCode(
                state.selectedText,
                "Add comments to each line explaining the code",
                state.language.toString(),
                AppSettingsState.instance.humanLanguage
        ).code ?: ""
    }

    interface CommentsAction_VirtualAPI {
        CommentsAction_ConvertedText editCode(
                String code,
                String operations,
                String computerLanguage,
                String humanLanguage
        )

        public class CommentsAction_ConvertedText {
            public String code;
            public String language;

            public ConvertedText() {}
        }
    }

}
