package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.IndentedText
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.simiacryptus.jopenai.util.StringUtil

class DescribeAction : SelectionAction<String>() {

    interface DescribeAction_VirtualAPI {
        fun describeCode(
            code: String,
            computerLanguage: String,
            humanLanguage: String
        ): DescribeAction_ConvertedText

        class DescribeAction_ConvertedText {
            var text: String? = null
            var language: String? = null
        }
    }

    private val proxy: DescribeAction_VirtualAPI
        get() = ChatProxy(
            clazz = DescribeAction_VirtualAPI::class.java,
            api = api,
            temperature = AppSettingsState.instance.temperature,
            model = AppSettingsState.instance.smartModel.chatModel(),
            deserializerRetries = 5
        ).create()

    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun processSelection(state: SelectionState, config: String?): String {
        val description = proxy.describeCode(
            IndentedText.fromString(state.selectedText).textBlock.toString().trim(),
            state.language?.name ?: "",
            AppSettingsState.instance.humanLanguage
        ).text ?: ""
        val wrapping = StringUtil.lineWrapping(description.trim(), 120)
        val numberOfLines = wrapping.trim().split("\n").reversed().dropWhile { it.isEmpty() }.size
        val commentStyle = if (numberOfLines == 1) {
            state.language?.lineComment
        } else {
            state.language?.blockComment
        }
        return buildString {
            append(state.indent)
            append(commentStyle?.fromString(wrapping)?.withIndent(state.indent) ?: wrapping)
            append("\n")
            append(state.indent)
            append(state.selectedText)
        }
    }
}