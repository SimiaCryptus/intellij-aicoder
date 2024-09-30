package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.actions.SelectionAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.jopenai.models.chatModel
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.jopenai.proxy.ChatProxy

class MarkdownImplementActionGroup : ActionGroup() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    private val markdownLanguages = listOf(
        "sql", "java", "asp", "c", "clojure", "coffee", "cpp", "csharp", "css", "bash", "go", "java", "javascript",
        "less", "make", "matlab", "objectivec", "pascal", "PHP", "Perl", "python", "rust", "scss", "sql", "svg",
        "swift", "ruby", "smalltalk", "vhdl"
    )

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    companion object {
        fun isEnabled(e: AnActionEvent): Boolean {
            val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
            if (ComputerLanguage.Markdown != computerLanguage) return false
            return UITools.hasSelection(e)
        }
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return emptyArray()
        val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return emptyArray()
        val actions = markdownLanguages.map { language -> MarkdownImplementAction(language) }
        return actions.toTypedArray()
    }

    open class MarkdownImplementAction(private val language: String) : SelectionAction<String>(true) {
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        init {
            templatePresentation.text = language
            templatePresentation.description = language
        }

        interface ConversionAPI {
            fun implement(text: String, humanLanguage: String, computerLanguage: String): ConvertedText

            class ConvertedText {
                var code: String? = null
                var language: String? = null
            }
        }

        private fun getProxy(): ConversionAPI {
            return ChatProxy(
                clazz = ConversionAPI::class.java,
                api = api,
                model = AppSettingsState.instance.smartModel.chatModel(),
                temperature = AppSettingsState.instance.temperature,
                deserializerRetries = 5
            ).create()
        }

        override fun getConfig(project: Project?): String {
            return ""
        }

        override fun processSelection(state: SelectionState, config: String?): String {
            val code = getProxy().implement(state.selectedText ?: "", "autodetect", language).code ?: ""
            return """
                |
                |
                |```$language
                |${code.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
                |```
                |
                |""".trimMargin()
        }
    }
}