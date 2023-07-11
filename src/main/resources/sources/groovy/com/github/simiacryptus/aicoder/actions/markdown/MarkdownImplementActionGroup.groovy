package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.openai.proxy.ChatProxy

class MarkdownImplementActionGroup extends ActionGroup {
    List<String> markdownLanguages = [
            "sql",
            "java",
            "asp",
            "c",
            "clojure",
            "coffee",
            "cpp",
            "csharp",
            "css",
            "bash",
            "go",
            "java",
            "javascript",
            "less",
            "make",
            "matlab",
            "objectivec",
            "pascal",
            "PHP",
            "Perl",
            "python",
            "rust",
            "scss",
            "sql",
            "svg",
            "swift",
            "ruby",
            "smalltalk",
            "vhdl"
    ]

    void update(AnActionEvent e) {
        e.presentation.setEnabledAndVisible(isEnabled(e))
        super.update(e)
    }

    static boolean isEnabled(AnActionEvent e) {
        def computerLanguage = ComputerLanguage.getComputerLanguage(e)
        if (null == computerLanguage) return false
        if (computerLanguage == ComputerLanguage.Text) return false
        if (ComputerLanguage.Markdown != computerLanguage) return false
        return UITools.hasSelection(e)
    }

    AnAction[] getChildren(AnActionEvent e) {
        if (null == e) return []
        def computerLanguage = ComputerLanguage.getComputerLanguage(e)
        if (null == computerLanguage) return []
        ArrayList<AnAction> actions = []
        for (language in markdownLanguages) {
            actions.add(new MarkdownImplementAction(language))
        }
        return actions.toArray(AnAction[]::new)
    }


    static class MarkdownImplementAction extends SelectionAction {
        String language

        MarkdownImplementAction(String language) {
            super(true)
            this.language = language
            this.templatePresentation.text = language
            this.templatePresentation.description = language
        }

        interface VirtualAPI {
            ConvertedText implement(String text, String humanLanguage, String computerLanguage)

            class ConvertedText {
                public String code
                public String language
                public ConvertedText() {
                }
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

        String processSelection(SelectionState state) {
            def code = proxy.implement(state.selectedText ?: "", "autodetect", language).code ?: ""
            return """
            |
            |```$language
            |$code
            |```
            |
            |""".stripMargin()
        }

        boolean isLanguageSupported(ComputerLanguage computerLanguage) {
            return ComputerLanguage.Markdown == computerLanguage
        }
    }
}