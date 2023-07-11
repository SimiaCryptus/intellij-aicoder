package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil
import static java.lang.Math.ceil
import static java.lang.Math.log
import static java.lang.Math.pow

class ReplaceOptionsAction extends SelectionAction {
    interface VirtualAPI {
        Suggestions suggestText(String template, List<String> examples)
        public class Suggestions {
            public List<String> choices = null
            public Suggestions() {}
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
    String processSelection(SelectionState state) {
        String selectedText = state.selectedText
        int idealLength = pow(2, 2 + ceil(log(selectedText.length()))).intValue()
        int selectionStart = state.selectionOffset
        String allBefore = state.entireDocument?.substring(0, selectionStart) ?: ""
        int selectionEnd = state.selectionOffset + (state.selectionLength ?: 0)
        String allAfter = state.entireDocument?.substring(selectionEnd, state.entireDocument.length()) ?: ""
        String before = StringUtil.getSuffixForContext(allBefore, idealLength).replaceAll("\n", " ")
        String after = StringUtil.getPrefixForContext(allAfter, idealLength).replaceAll("\n", " ")
        List<String> choices = proxy.suggestText(
            "$before _____ $after",
            [selectedText]
        ).choices
        return choose(choices)
    }

    def choose(List<String> choices) {
        return UITools.showRadioButtonDialog("Select an option to fill in the blank:", choices.toArray(CharSequence[]::new))?.toString() ?: ""
    }

}