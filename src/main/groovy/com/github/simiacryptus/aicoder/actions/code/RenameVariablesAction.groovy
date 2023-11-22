package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy
import org.jetbrains.annotations.Nullable

class RenameVariablesAction extends SelectionAction<String> {

    interface RenameAPI {
        SuggestionResponse suggestRenames(
                String code,
                String computerLanguage,
                String humanLanguage
        )

        class SuggestionResponse {
            public List<Suggestion> suggestions = []

            SuggestionResponse() {}
        }

        class Suggestion {
            public String originalName = null
            public String suggestedName = null

            Suggestion() {}
        }
    }

    def getProxy() {
        return new ChatProxy<RenameAPI>(
                clazz: RenameAPI.class,
                api: api,
                model: AppSettingsState.instance.defaultChatModel(),
                temperature: AppSettingsState.instance.temperature,
                deserializerRetries: 5,
        ).create()
    }

    @Override
    String getConfig(@Nullable Project project) {
        return ""
    }


    @Override
    String processSelection(AnActionEvent event, SelectionState state, String config) {
        def renameSuggestions = UITools.run(event == null ? null : event.project, templateText, true, true, {
            return proxy
                    .suggestRenames(
                            state.selectedText,
                            state.language?.name(),
                            AppSettingsState.instance.humanLanguage
                    )
                    .suggestions
                    .findAll { it.originalName != null && it.suggestedName != null }
                    .<String,String, RenameAPI.Suggestion>collectEntries { [(it.originalName): it.suggestedName] }
        })
        def selectedSuggestions = choose(renameSuggestions)
        return UITools.run(event == null ? null : event.project, templateText, true, true, {
            def selectedText = state.selectedText
            def filter = renameSuggestions.findAll { x -> selectedSuggestions.contains(x.key) }
            def txt = selectedText
            for (entry in filter) {
                txt = txt.replace(entry.key, entry.value)
            }
            return txt
        })

    }

    def choose(Map<String, String> renameSuggestions) {
        return UITools.showCheckboxDialog(
                "Select which items to rename",
                renameSuggestions.keySet().toArray(String[]::new),
                renameSuggestions.collect { kv -> "${kv.key} -> ${kv.value}".toString() }.toArray(String[]::new)
        )
    }

    boolean isLanguageSupported(ComputerLanguage computerLanguage) {
        return computerLanguage != ComputerLanguage.Text
    }

}