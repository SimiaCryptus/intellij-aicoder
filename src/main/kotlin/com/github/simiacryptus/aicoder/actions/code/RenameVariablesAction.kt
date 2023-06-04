package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.UITools.showCheckboxDialog
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.simiacryptus.openai.proxy.ChatProxy
import org.jetbrains.annotations.NotNull

class RenameVariablesAction : BaseAction() {

    interface VirtualAPI {
        fun suggestRenames(
            code: String,
            computerLanguage: String? = null,
            humanLanguage: String? = null,
        ): SuggestionResponse
        data class SuggestionResponse(
            val suggestions: List<Suggestion> = emptyList(),
        )
        data class Suggestion(
            val identifier: String? = null,
            val suggestion: String? = null,
        )
    }

    val proxy: VirtualAPI
        get() = ChatProxy(
            clazz = VirtualAPI::class.java,
            api = api,
            maxTokens = AppSettingsState.instance.maxTokens,
            deserializerRetries = 5,
        ).create()


    override fun actionPerformed(@NotNull event: AnActionEvent) {
        @NotNull val textEditor = event.getData(CommonDataKeys.EDITOR) ?: return
        @NotNull val caretModel = textEditor.caretModel
        @NotNull val mainCursor = caretModel.primaryCaret
        @NotNull val outputLanguage = AppSettingsState.instance.humanLanguage
        val sourceFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val codeElement =
            PsiUtil.getSmallestIntersectingMajorCodeElement(sourceFile, mainCursor) ?: throw IllegalStateException()
        @NotNull val programmingLanguage = ComputerLanguage.getComputerLanguage(event)

        UITools.redoableTask(event) {

            val renameSuggestions = UITools.run(
                event.project, "Converting for Paste", true
            ) {
                proxy.suggestRenames(
                    code = codeElement.text,
                    computerLanguage = programmingLanguage?.name,
                    humanLanguage = outputLanguage,
                ).suggestions
                    .filter { it.identifier != null }
                    .filter { it.suggestion != null }
                    .map { it.identifier!! to it.suggestion!! }
                    .toMap()
            }

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

            UITools.writeableFn(event) {
                replaceString(textEditor.document, codeElement.startOffset, codeElement.endOffset, modifiedText)
            }
        }
    }

    override fun isEnabled(@NotNull event: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        val computerLanguage = ComputerLanguage.getComputerLanguage(event) ?: return false
        return computerLanguage != ComputerLanguage.Text
    }
}









































