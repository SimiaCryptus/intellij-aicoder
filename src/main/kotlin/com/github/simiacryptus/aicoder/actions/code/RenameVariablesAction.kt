package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.*
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.UITools.showCheckboxDialog
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.simiacryptus.util.StringTools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.*
import java.util.stream.Collectors

class RenameVariablesAction : AnAction() {

    override fun update(@NotNull e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    private fun isEnabled(@NotNull e: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(e) ?: return false
        if (computerLanguage == ComputerLanguage.Text) return false
        return true
    }

    override fun actionPerformed(@NotNull actionEvent: AnActionEvent) {
        @NotNull val textEditor = actionEvent.getRequiredData(CommonDataKeys.EDITOR)
        @NotNull val caretModel = textEditor.caretModel
        @NotNull val mainCursor = caretModel.primaryCaret
        @NotNull val outputLanguage = AppSettingsState.instance.humanLanguage
        val sourceFile = actionEvent.getRequiredData(CommonDataKeys.PSI_FILE)
        val codeElement =
            PsiUtil.getSmallestIntersectingMajorCodeElement(sourceFile, mainCursor) ?: throw IllegalStateException()
        @NotNull val programmingLanguage = ComputerLanguage.getComputerLanguage(actionEvent)
        val appSettings = AppSettingsState.instance

        @Language("Markdown")
        @NotNull val completionRequest = appSettings.createCompletionRequest()
            .appendPrompt(
                """
                Extract Parameters and Local Variable names and suggest new names in $outputLanguage
                ```${programmingLanguage!!.name}
                  ${codeElement.text.replace("\n", "\n  ", false)}
                ```
                
                | Identifier | Rename Suggestion |
                |------------|-------------------|
                """.trimIndent().trim()
            )
        @Nullable val textCursor = actionEvent.getData(CommonDataKeys.CARET)
        val textIndent = UITools.getIndent(textCursor)
        UITools.redoableRequest(completionRequest, textIndent, actionEvent) { completionText ->

            val renameSuggestions = completionText.split('\n').stream().map { x ->
                val kv = StringTools.stripSuffix(StringTools.stripPrefix(x.trim(), "|"), "|").split('|')
                    .map { x -> x.trim() }
                kv[0] to kv[1]
            }.collect(Collectors.toMap({ x -> x.first }, { x -> x.second }))

            val selectedSuggestions = showCheckboxDialog(
                "Select which items to rename",
                renameSuggestions.keys.toTypedArray(),
                renameSuggestions.map { kv -> "${kv.key} -> ${kv.value}" }.toTypedArray()
            )

            var modifiedText = codeElement.text
            renameSuggestions.filter { x -> selectedSuggestions.contains(x.key) }
                .forEach { kv ->
                    modifiedText = modifiedText.replace(Regex("(?<![01-9a-zA-Z_])${kv.key}(?![01-9a-zA-Z_])"), kv.value)
                }
            replaceString(textEditor.document, codeElement.startOffset, codeElement.endOffset, modifiedText)

        }
    }

}









































