package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.IndentedText
import com.simiacryptus.util.StringUtil
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy

class DescribeAction : SelectionAction() {

    interface VirtualAPI {
        fun describeCode(
            code: String,
            computerLanguage: String,
            humanLanguage: String,
        ): ConvertedText

        data class ConvertedText(
            val text: String? = null,
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

    override fun processSelection(state: SelectionState): String {
        val description = proxy.describeCode(
            code = IndentedText.fromString(state.selectedText).textBlock.toString().trim(),
            computerLanguage = state.language?.name ?: "",
            humanLanguage = AppSettingsState.instance.humanLanguage,
        ).text ?: ""
        val wrapping = StringUtil.lineWrapping(description.trim(), 120)
        val numberOfLines = wrapping.trim().split("\n")
            .dropLastWhile { it.isEmpty() }.toTypedArray<String>().size
        val commentStyle =
            if (numberOfLines == 1) {
                state.language?.lineComment
            } else {
                state.language?.blockComment
            }
        return """
            ${state.indent}${commentStyle?.fromString(wrapping)?.withIndent(state.indent) ?: wrapping}
            ${state.indent}${state.selectedText}
        """.trimIndent()
    }
}

