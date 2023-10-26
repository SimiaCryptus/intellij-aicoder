package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.psi.PsiClassContext
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.proxy.ChatProxy
import kotlin.Pair
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class InsertImplementationAction extends SelectionAction<String> {

    interface VirtualAPI {
        ConvertedText implementCode(
                String specification,
                String prefix,
                String computerLanguage,
                String humanLanguage
        )

        class ConvertedText {
            public String code
            public String language

            ConvertedText() {}
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
    String getConfig(@Nullable Project project) {
        return ""
    }

    @Override
    Pair<Integer, Integer> defaultSelection(@NotNull EditorState editorState, int offset) {
        def foundItem = editorState.contextRanges.findAll {
            PsiUtil.matchesType(
                    it.name,
                    PsiUtil.ELEMENTS_COMMENTS
            )
        }.min({ it.length() })
        return foundItem?.range() ?: editorState.line
    }

    @Override
    Pair<Integer, Integer> editSelection(@NotNull EditorState state, int start, int end) {
        def foundItem = state.contextRanges.findAll {
            PsiUtil.matchesType(
                    it.name,
                    PsiUtil.ELEMENTS_COMMENTS
            )
        }.min({ it.length() })
        return foundItem?.range() ?: new Pair<>(start, end)
    }

    @Override
    String processSelection(SelectionState state, String config) {
        def humanLanguage = AppSettingsState.instance.humanLanguage
        def computerLanguage = state.language
        def psiClassContextActionParams = getPsiClassContextActionParams(state)
        def selectedText = state.selectedText ?: ""

        def comment = psiClassContextActionParams.largestIntersectingComment
        def instruct = (null == comment) ? selectedText : comment.subString(state.entireDocument ?: "").trim()
        if (selectedText.split(" ").reverse().dropWhile { it.isEmpty() }.reverse().length > 4) {
            instruct = selectedText.trim()
        }
        def specification = Objects.requireNonNull(computerLanguage.getCommentModel(instruct))
                .fromString(instruct).stream()
                .map { obj -> obj.toString() }
                .map { obj -> obj.trim() }
                .filter { x -> !x.isEmpty() }
                .reduce { a, b -> "$a $b" }.get()
        if(null != state.psiFile) {
            def code = UITools.run(state.project, "Insert Implementation", true, true, {
                def psiClassContext = PsiClassContext.getContext(
                            state.psiFile,
                            psiClassContextActionParams.selectionStart,
                            psiClassContextActionParams.selectionEnd,
                            computerLanguage
                    ).toString()
                proxy.implementCode(
                        specification,
                        psiClassContext,
                        computerLanguage.name(),
                        humanLanguage,
                ).code
            })
            if(null != code) return selectedText + "\n${state.indent}" + code
        } else {
            def code = proxy.implementCode(
                    specification,
                    "",
                    computerLanguage.name(),
                    humanLanguage,
            ).code
            if(null != code) return selectedText + "\n${state.indent}" + code
        }
        return selectedText
    }

    static class PsiClassContextActionParams {
        int selectionStart
        int selectionEnd
        SelectionAction.ContextRange largestIntersectingComment

        PsiClassContextActionParams(int selectionStart, int selectionEnd, SelectionAction.ContextRange largestIntersectingComment) {
            this.selectionStart = selectionStart
            this.selectionEnd = selectionEnd
            this.largestIntersectingComment = largestIntersectingComment
        }
    }

    static PsiClassContextActionParams getPsiClassContextActionParams(SelectionState state) {
        int selectionStart = state.selectionOffset
        return new PsiClassContextActionParams(
                selectionStart,
                selectionStart + (state.selectionLength ?: 0),
                state.contextRanges.find { PsiUtil.matchesType(it.name, PsiUtil.ELEMENTS_COMMENTS) }
        )
    }

    @Override
    boolean isLanguageSupported(@Nullable ComputerLanguage computerLanguage) {
        if (computerLanguage == null) return false
        if (computerLanguage == ComputerLanguage.Text) return false
        if (computerLanguage == ComputerLanguage.Markdown) return false
        return super.isLanguageSupported(computerLanguage)
    }
}