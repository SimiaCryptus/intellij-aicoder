package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.proxy.ChatProxy
import kotlin.Pair
import org.jetbrains.annotations.Nullable

class DocAction extends SelectionAction<String> {

    interface DocAction_VirtualAPI {
        DocAction_ConvertedText processCode(
                String code,
                String operation,
                String computerLanguage,
                String humanLanguage
        )

        public class DocAction_ConvertedText {
            public String text;
            public String language;

            public ConvertedText() {}
        }
    }

    DocAction_VirtualAPI getProxy() {
        ChatProxy<DocAction_VirtualAPI> chatProxy = new ChatProxy(
                clazz: DocAction_VirtualAPI,
                api: api,
                model: AppSettingsState.instance.defaultChatModel(),
                temperature: AppSettingsState.instance.temperature,
                deserializerRetries: 5
        )
        chatProxy.addExample(
                new DocAction_VirtualAPI.DocAction_ConvertedText(
                        text: '''
                    /**
                     *  Prints "Hello, world!" to the console
                     */
                    '''.trim(),
                        language: "English"
                )
        ) {
            (DocAction_VirtualAPI x) ->
                    x.processCode(
                            '''
                    fun hello() {
                        println("Hello, world!")
                    }
                    '''.trim(),
                            "Write detailed KDoc prefix for code block",
                            "Kotlin",
                            "English"
                    )
        }
        return chatProxy.create() as DocAction_VirtualAPI
    }

    @Override
    String getConfig(@Nullable Project project) {
        return ""
    }


    @Override
    String processSelection(SelectionState state, String config) {
        CharSequence code = state.selectedText
        IndentedText indentedInput = IndentedText.fromString3(code.toString() as java.lang.CharSequence)
        String docString = proxy.processCode(
                indentedInput.textBlock.toString(),
                "Write detailed " + (state.language?.docStyle ?: "documentation") + " prefix for code block",
                state.language.name(),
                AppSettingsState.instance.humanLanguage
        ).text ?: ""
        return docString + code
    }

    @Override
    boolean isLanguageSupported(ComputerLanguage computerLanguage) {
        if (computerLanguage == ComputerLanguage.Text) return false
        if (computerLanguage?.docStyle == null) return false
        if (computerLanguage?.docStyle?.isBlank()) return false
        return true
    }

    @Override
    Pair<Integer, Integer> editSelection(EditorState state, int start, int end) {
        if (null == state.psiFile) return super.editSelection(state, start, end)
        def codeBlock = PsiUtil.getCodeElement(state.psiFile, start, end)
        if (null == codeBlock) return super.editSelection(state, start, end)
        def textRange = codeBlock.textRange
        return new Pair<>(textRange.startOffset, textRange.endOffset)
    }
}
