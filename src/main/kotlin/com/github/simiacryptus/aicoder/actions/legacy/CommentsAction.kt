package com.github.simiacryptus.aicoder.actions.legacy

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.AppSettingsState.Companion.chatModel
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy

class CommentsAction : SelectionAction<String>() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

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
            model = AppSettingsState.instance.smartModel.chatModel(),
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