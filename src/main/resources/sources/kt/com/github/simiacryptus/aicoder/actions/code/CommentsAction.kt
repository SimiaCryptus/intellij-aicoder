package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy

class CommentsAction : SelectionAction<String>() {

    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        return computerLanguage != null && computerLanguage != ComputerLanguage.Text
    }

    override fun processSelection(state: SelectionState, config: String?): String {
        return ChatProxy(
            clazz = CommentsAction_VirtualAPI::class.java,
            api = api,
            temperature = AppSettingsState.instance.temperature,
            model = AppSettingsState.instance.defaultChatModel(),
            deserializerRetries = 5
        ).create().editCode(
            state.selectedText ?: "",
            "Add comments to each line explaining the code",
            state.language.toString(),
            AppSettingsState.instance.humanLanguage
        ).code ?: ""
    }

    interface CommentsAction_VirtualAPI {
        fun editCode(
            code: String,
            operations: String,
            computerLanguage: String,
            humanLanguage: String
        ): CommentsAction_ConvertedText

        class CommentsAction_ConvertedText {
            var code: String? = null
            var language: String? = null
        }
    }
}