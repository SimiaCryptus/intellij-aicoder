package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.simiacryptus.jopenai.util.StringUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import static java.lang.Math.*

class ReplaceOptionsAction extends SelectionAction<String> {
    interface VirtualAPI {
        Suggestions suggestText(String template, List<String> examples)

        class Suggestions {
            public List<String> choices = null

            Suggestions() {}
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
    @Override
    String getConfig(@Nullable Project project) {
        return ""
    }

    @Override
    String processSelection(@Nullable AnActionEvent event, @NotNull SelectionState state, @Nullable String config) {
        List<String> choices = UITools.run(event==null?null:event.project, templateText, true, true, {
            String selectedText = state.selectedText
            int idealLength = pow(2, 2 + ceil(log(selectedText.length()))).intValue()
            int selectionStart = state.selectionOffset
            String allBefore = state.entireDocument?.substring(0, selectionStart) ?: ""
            int selectionEnd = state.selectionOffset + (state.selectionLength ?: 0)
            String allAfter = state.entireDocument?.substring(selectionEnd, state.entireDocument.length()) ?: ""
            String before = StringUtil.getSuffixForContext(allBefore, idealLength).replaceAll("\n", " ")
            String after = StringUtil.getPrefixForContext(allAfter, idealLength).replaceAll("\n", " ")
            return proxy.suggestText(
                "$before _____ $after",
                [selectedText]
            ).choices
        })
        return choose(choices)
    }

    def choose(List<String> choices) {
        return UITools.showRadioButtonDialog("Select an option to fill in the blank:", choices.toArray(CharSequence[]::new))?.toString() ?: ""
    }

}