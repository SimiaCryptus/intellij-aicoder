package com.github.simiacryptus.aicoder.actions.markdown

import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.hasSelection
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger

/**
 * The ConvertFileTo ActionGroup provides a way to quickly insert code snippets into markdown documents in various languages.
 */
class MarkdownImplementActionGroup : ActionGroup() {
    var markdownLanguages: List<String> = mutableListOf(
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
    )

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    private fun isEnabled(e: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
        if (computerLanguage == ComputerLanguage.Text) return false
        if (ComputerLanguage.Markdown != computerLanguage) return false
        return hasSelection(e)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if(null == e) return arrayOf()
        val computerLanguage = ComputerLanguage.getComputerLanguage(e)
        val actions = ArrayList<AnAction>()
        for (language in markdownLanguages) {
            if (computerLanguage!!.name == language) continue
            actions.add(MarkdownImplementAction(language))
        }
        return actions.toArray(arrayOf())
    }

    companion object {
        private val log = Logger.getInstance(
            MarkdownImplementActionGroup::class.java
        )
    }
}