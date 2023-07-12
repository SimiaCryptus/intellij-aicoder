package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.openai.proxy.ChatProxy

class RenameVariablesAction extends SelectionAction {

    interface VirtualAPI {
        SuggestionResponse suggestRenames(
            String code,
            String computerLanguage,
            String humanLanguage
        )

        public class SuggestionResponse {
            public List<Suggestion> suggestions = []
            public SuggestionResponse() {}
        }

        public class Suggestion {
            public String originalName = null
            public String suggestedName = null
            public Suggestion() {}
        }
    }

    def getProxy() {
        return new ChatProxy<VirtualAPI>(
            clazz: VirtualAPI.class,
            api: api,
            model: AppSettingsState.instance.defaultChatModel(),
            temperature: AppSettingsState.instance.temperature,
            deserializerRetries: 5,
        ).create()
    }

    String processSelection(SelectionState state) {
        def renameSuggestions = proxy.suggestRenames(
            state.selectedText,
            state.language?.name(),
            AppSettingsState.instance.humanLanguage
        ).suggestions
            .findAll { it.originalName != null && it.suggestedName != null }
            .collectEntries { [(it.originalName): it.suggestedName] }

        def selectedSuggestions = choose(renameSuggestions)


        def selectedText = state.selectedText
        def filter = renameSuggestions.findAll { x -> selectedSuggestions.contains(x.key) }
        def txt = selectedText
        for (entry in filter) {
            txt = txt.replace(entry.key, entry.value)
        }
        return txt
    }

    def choose(Map<String, String> renameSuggestions) {
        return showCheckboxDialog(
            "Select which items to rename",
            renameSuggestions.keySet().toArray(),
            renameSuggestions.collect { kv -> "${kv.key} -> ${kv.value}" }.toArray()
        )
    }

    boolean isLanguageSupported(ComputerLanguage computerLanguage) {
        return computerLanguage != ComputerLanguage.Text
    }

}