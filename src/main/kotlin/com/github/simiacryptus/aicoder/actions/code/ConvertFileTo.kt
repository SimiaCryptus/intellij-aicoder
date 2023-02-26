package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import java.util.*

/**
 * The ConvertFileTo ActionGroup provides a way to quickly convert a file from one language to another.
 * It is enabled when the current file is in one of the supported languages,
 * and provides a list of available languages to convert to.
 */
class ConvertFileTo : ActionGroup() {
    private var supportedLanguages = listOf(
        ComputerLanguage.Java,
        ComputerLanguage.JavaScript,
        ComputerLanguage.Scala,
        ComputerLanguage.Kotlin,
        ComputerLanguage.Go,
        ComputerLanguage.Rust,
        ComputerLanguage.Python
    )

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    private fun isEnabled(e: AnActionEvent): Boolean {
        val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
        if(computerLanguage == ComputerLanguage.Text) return false
        return supportedLanguages.contains(computerLanguage)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val computerLanguage = ComputerLanguage.getComputerLanguage(e!!)
        val actions = ArrayList<AnAction>()
        for (language in supportedLanguages) {
            if (computerLanguage == language) continue
            actions.add(ConvertFileToLanguage(language))
        }
        return actions.toArray(arrayOf())
    }

    companion object {
        private val log = Logger.getInstance(
            ConvertFileTo::class.java
        )
    }
}