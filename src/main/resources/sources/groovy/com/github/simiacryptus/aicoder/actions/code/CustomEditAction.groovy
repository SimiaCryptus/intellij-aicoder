package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.proxy.ChatProxy
import org.jetbrains.annotations.Nullable

import javax.swing.*

class CustomEditAction extends SelectionAction<String> {

    interface VirtualAPI {
        EditedText editCode(
                String code,
                String operation,
                String computerLanguage,
                String humanLanguage
        )

        class EditedText {
            public String code = null
            public String language = null

            public EditedText() {}

            public EditedText(String code, String language) {
                this.code = code
                this.language = language
            }

        }
    }

    def getProxy() {
        def chatProxy = new ChatProxy<VirtualAPI>(
                clazz: VirtualAPI.class,
                api: api,
                temperature: AppSettingsState.instance.temperature,
                model: AppSettingsState.instance.defaultChatModel(),
        )
        chatProxy.addExample(
                new VirtualAPI.EditedText(
                        """
                        // Print Hello, World! to the console
                        println("Hello, World!")
                        """.stripIndent(),
                        "java"
                )
        ) {
            it.editCode(
                    """println("Hello, World!")""",
                    "Add code comments",
                    "java",
                    "English"
            )
        }
        return chatProxy.create()
    }

    @Override
    String getConfig(@Nullable Project project) {
        return UITools.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE
                //, AppSettingsState.instance.getRecentCommands("customEdits").mostRecentHistory
        )
    }


    @Override
    String processSelection(SelectionState state, String instruction) {
        if (null == instruction) return (state.selectedText ?: "")
        if (instruction.isBlank()) return state.selectedText ?: ""
        def settings = AppSettingsState.instance
        def outputHumanLanguage = AppSettingsState.instance.humanLanguage
        settings.getRecentCommands("customEdits").addInstructionToHistory(instruction)
        return proxy.editCode(
                state.selectedText,
                instruction.toString(),
                state.language.name(),
                outputHumanLanguage
        ).code ?: state.selectedText ?: ""
    }


}