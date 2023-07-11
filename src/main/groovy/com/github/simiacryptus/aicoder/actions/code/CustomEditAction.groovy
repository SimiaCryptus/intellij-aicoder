package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.openai.proxy.ChatProxy

import javax.swing.*

class CustomEditAction extends SelectionAction {

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
    String processSelection(SelectionState state) {
        def instruction = getInstruction()
        if (null == instruction) return (state.selectedText ?: "")
        if (instruction.isBlank()) return state.selectedText ?: ""
        def settings = AppSettingsState.instance
        def outputHumanLanguage = AppSettingsState.instance.humanLanguage
        settings.recentCustomEdits("customEdits").addInstructionToHistory(instruction)
        return proxy.editCode(
                state.selectedText,
                instruction.toString(),
                state.language.name(),
                outputHumanLanguage
        ).code ?: state.selectedText ?: ""
    }

    def getInstruction() {
        return JOptionPane.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE)
    }

}