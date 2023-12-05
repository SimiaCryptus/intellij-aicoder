package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy

class RenameVariablesAction : SelectionAction<String>() {

    interface RenameAPI {
        fun suggestRenames(
            code: String,
            computerLanguage: String,
            humanLanguage: String
        ): SuggestionResponse

        class SuggestionResponse {
            var suggestions: MutableList<Suggestion> = mutableListOf()

            class Suggestion {
                var originalName: String? = null
                var suggestedName: String? = null
            }
        }
    }

    val proxy: RenameAPI get() {
        return ChatProxy(
            clazz = RenameAPI::class.java,
            api = api,
            model = AppSettingsState.instance.defaultChatModel(),
            temperature = AppSettingsState.instance.temperature,
            deserializerRetries = 5
        ).create()
    }

    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun processSelection(event: AnActionEvent?, state: SelectionState, config: String?): String {
        val renameSuggestions = UITools.run(event?.project, templateText, true, true) {
            proxy
                .suggestRenames(
                    state.selectedText ?: "",
                    state.language?.name ?: "",
                    AppSettingsState.instance.humanLanguage
                )
                .suggestions
                .filter { it.originalName != null && it.suggestedName != null }
                .associate { it.originalName!! to it.suggestedName!! }
        }
        val selectedSuggestions = choose(renameSuggestions)
        return UITools.run(event?.project, templateText, true, true) {
            var selectedText = state.selectedText
            val filter = renameSuggestions.filter { it.key in selectedSuggestions }
            filter.forEach { (key, value) ->
                selectedText = selectedText?.replace(key, value)
            }
            selectedText ?: ""
        }
    }

    private fun choose(renameSuggestions: Map<String, String>): Set<String> {
        return UITools.showCheckboxDialog(
            "Select which items to rename",
            renameSuggestions.keys.toTypedArray(),
            renameSuggestions.map { (key, value) -> "$key -> $value" }.toTypedArray()
        ).toSet()
    }

    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        return computerLanguage != ComputerLanguage.Text
    }
}