package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.TextBlockFactory
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil

class DescribeAction extends SelectionAction {

    interface DescribeAction_VirtualAPI {
        DescribeAction_ConvertedText describeCode(
                String code,
                String computerLanguage,
                String humanLanguage
        )

    }

    class DescribeAction_ConvertedText {
        String text = null
        String language = null
    }

    def getProxy() {
        return new ChatProxy<DescribeAction_VirtualAPI>(
                clazz: DescribeAction_VirtualAPI.class,
                api: api,
                temperature: AppSettingsState.instance.temperature,
                model: AppSettingsState.instance.defaultChatModel(),
                deserializerRetries: 5
        ).create()
    }

    @Override
    String processSelection(SelectionState state) {
        def description = proxy.describeCode(
                IndentedText.fromString(state.selectedText).textBlock.toString().trim(),
                state.language?.name() ?: "",
                AppSettingsState.instance.humanLanguage,
        ).text ?: ""
        def wrapping = StringUtil.lineWrapping(description.trim(), 120)
        def numberOfLines = wrapping.trim().split("\n").reverse().dropWhile { it.isEmpty() }.size()
        TextBlockFactory commentStyle = null
        if (numberOfLines == 1) {
            state.language?.lineComment
        } else {
            state.language?.blockComment
        }
        return """
            ${state.indent}${commentStyle?.fromString(wrapping)?.withIndent(state.indent) ?: wrapping}
            ${state.indent}${state.selectedText}
        """
    }
}