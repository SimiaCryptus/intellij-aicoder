package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.util.StringUtil
import kotlin.Pair
import org.jetbrains.annotations.Nullable

class ImplementStubAction extends SelectionAction<String> {

    static interface VirtualAPI {
        ConvertedText editCode(
            String code,
            String operation,
            String computerLanguage,
            String humanLanguage
        )

        static class ConvertedText {
            public String code
            public String language
            public ConvertedText() {}
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

    boolean isLanguageSupported(ComputerLanguage computerLanguage) {
        if (computerLanguage == null) return false
        return computerLanguage != ComputerLanguage.Text
    }

    Pair<Integer, Integer> defaultSelection(EditorState editorState, int offset) {
        def codeRanges = editorState.contextRanges.findAll { PsiUtil.matchesType(it.name, PsiUtil.ELEMENTS_CODE) }
        if (codeRanges.isEmpty()) return editorState.line
        return codeRanges.min { it.length() }.range()
    }
    @Override
    String getConfig(@Nullable Project project) {
        return ""
    }


    String processSelection(SelectionState state, String config) {
        def code = state.selectedText ?: ""
        def settings = AppSettingsState.instance
        def outputHumanLanguage = settings.humanLanguage
        def computerLanguage = state.language

        def codeContext = state.contextRanges.findAll {
            PsiUtil.matchesType(
                it.name,
                PsiUtil.ELEMENTS_CODE
            )
        }
        def smallestIntersectingMethod = ""
        if(!codeContext.isEmpty()) smallestIntersectingMethod = codeContext.min { it.length() }.subString(state.entireDocument)

        def declaration = code
        declaration = StringUtil.stripSuffix(declaration.toString().trim(), smallestIntersectingMethod)
        declaration = declaration.toString().trim()

        return proxy.editCode(
            declaration,
            "Implement Stub",
            computerLanguage.name().toLowerCase(Locale.ROOT),
            outputHumanLanguage
        ).code ?: ""
    }

}