package com.github.simiacryptus.aicoder.actions.legacy

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

open class ReplaceWithSuggestionsAction : SelectionAction<String>() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

    interface VirtualAPI {
        fun suggestText(template: String, examples: List<String>): Suggestions

        class Suggestions {
            var choices: List<String>? = null
        }
    }

    val proxy: VirtualAPI
        get() {
            return ChatProxy(
                clazz = VirtualAPI::class.java,
                api = api,
                model = AppSettingsState.instance.smartModel.chatModel(),
                temperature = AppSettingsState.instance.temperature,
                deserializerRetries = 5
            ).create()
        }

    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun processSelection(event: AnActionEvent?, state: SelectionState, config: String?): String {
        val choices = UITools.run(event?.project, templateText, true, true) {
            val selectedText = state.selectedText
            val idealLength = 2.0.pow(2 + ceil(ln(selectedText?.length?.toDouble() ?: 1.0))).toInt()
            val selectionStart = state.selectionOffset
            val allBefore = state.entireDocument?.substring(0, selectionStart) ?: ""
            val selectionEnd = state.selectionOffset + (state.selectionLength ?: 0)
            val allAfter = state.entireDocument?.substring(selectionEnd, state.entireDocument.length) ?: ""
            val before = StringUtil.getSuffixForContext(allBefore, idealLength).toString().replace('\n', ' ')
            val after = StringUtil.getPrefixForContext(allAfter, idealLength).toString().replace('\n', ' ')
            proxy.suggestText(
                "$before _____ $after",
                listOf(selectedText.toString())
            ).choices
        }
        return choose(choices ?: listOf())
    }

    open fun choose(choices: List<String>): String {
        return UITools.showRadioButtonDialog("Select an option to fill in the blank:", *choices.toTypedArray())
            ?.toString() ?: ""
    }
}