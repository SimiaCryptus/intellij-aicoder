package com.github.simiacryptus.aicoder.actions.legacy

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil
import java.util.*

class ImplementStubAction : SelectionAction<String>() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isEnabled(event: AnActionEvent) = AppSettingsState.instance.enableLegacyActions

    interface VirtualAPI {
        fun editCode(
            code: String,
            operation: String,
            computerLanguage: String,
            humanLanguage: String
        ): ConvertedText

        class ConvertedText {
            var code: String? = null
            var language: String? = null
        }
    }

    private fun getProxy(): VirtualAPI {
        return ChatProxy(
            clazz = VirtualAPI::class.java,
            api = api,
            model = AppSettingsState.instance.smartModel.chatModel(),
            temperature = AppSettingsState.instance.temperature,
            deserializerRetries = 5
        ).create()
    }

    override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
        if (computerLanguage == null) return false
        return computerLanguage != ComputerLanguage.Text
    }

    override fun defaultSelection(editorState: EditorState, offset: Int): Pair<Int, Int> {
        val codeRanges = editorState.contextRanges.filter { PsiUtil.matchesType(it.name, PsiUtil.ELEMENTS_CODE) }
        if (codeRanges.isEmpty()) return editorState.line
        return codeRanges.minByOrNull { it.length() }?.range() ?: editorState.line
    }

    override fun getConfig(project: Project?): String {
        return ""
    }

    override fun processSelection(state: SelectionState, config: String?): String {
        val code = state.selectedText ?: ""
        val settings = AppSettingsState.instance
        val outputHumanLanguage = settings.humanLanguage
        val computerLanguage = state.language

        val codeContext = state.contextRanges.filter {
            PsiUtil.matchesType(
                it.name,
                PsiUtil.ELEMENTS_CODE
            )
        }
        var smallestIntersectingMethod = ""
        if (codeContext.isNotEmpty()) smallestIntersectingMethod =
            codeContext.minByOrNull { it.length() }?.subString(state.entireDocument ?: "") ?: ""

        var declaration = code
        declaration = StringUtil.stripSuffix(declaration.trim(), smallestIntersectingMethod)
        declaration = declaration.trim()

        return getProxy().editCode(
            declaration,
            "Implement Stub",
            computerLanguage?.name?.lowercase(Locale.ROOT) ?: "",
            outputHumanLanguage
        ).code ?: ""
    }

}