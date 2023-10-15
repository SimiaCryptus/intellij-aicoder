package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.proxy.ChatProxy
import org.jetbrains.annotations.Nullable

import javax.swing.JOptionPane

class QuestionAction extends SelectionAction<String> {

    interface VirtualAPI {
        Answer questionCode(
            String code,
            String question
        )

        class Answer {
            public String text = null

            Answer() {}
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
        return JOptionPane.showInputDialog(null, "Question:", "Question", JOptionPane.QUESTION_MESSAGE)
    }


    @Override
    String processSelection(SelectionState state, String question) {

        if (question.isBlank()) return ""

        def newText = proxy.questionCode(
            state.selectedText ?: "",
            question,
        ).text ?: ""

        def answer = """
                    |Question: $question
                    |Answer: ${newText.trim()}
                    |""".stripMargin().trim()

        def blockComment = state.language?.blockComment
        def fromString = blockComment?.fromString(answer)
        return "${state.indent}${fromString.withIndent(state.indent)}\n${state.indent}" + state.selectedText
    }

    @Override
    boolean isLanguageSupported(ComputerLanguage computerLanguage) {
        return computerLanguage != ComputerLanguage.Text
    }
}