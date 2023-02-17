package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.ComputerLanguage
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.util.UITools.replaceString
import com.github.simiacryptus.aicoder.util.UITools.showOptionDialog
import com.github.simiacryptus.aicoder.util.psi.PsiUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.ui.FormBuilder
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.stream.Collectors
import javax.swing.Icon
import javax.swing.JCheckBox

class RenameVariablesAction : AnAction() {

    override fun update(@NotNull e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    private fun isEnabled(@NotNull e: AnActionEvent): Boolean {
        if (ComputerLanguage.getComputerLanguage(e) == null) return false
        return true
    }

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        @NotNull val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        @NotNull val caretModel = editor.caretModel
        @NotNull val primaryCaret = caretModel.primaryCaret
        @NotNull val outputHumanLanguage = AppSettingsState.getInstance().humanLanguage
        val psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE)
        val codeElement = PsiUtil.getSmallestIntersectingMajorCodeElement(psiFile, primaryCaret);
        @NotNull val language = ComputerLanguage.getComputerLanguage(event)
        val settings = AppSettingsState.getInstance()

        @Language("Markdown")
        @NotNull val request = settings.createCompletionRequest()
                .appendPrompt("""
Extract Parameters and Local Variable names and suggest new names in $outputHumanLanguage
```${language!!.name}
  ${codeElement.text.replace("\n", "\n  ", false)}
```

| Identifier | Rename Suggestion |
|------------|-------------------|
""".trim())
        @Nullable val caret = event.getData(CommonDataKeys.CARET)
        val indent = UITools.getIndent(caret)
        UITools.redoableRequest(request, indent, event) { newText ->

            val renames = newText!!.split('\n').stream().map { x ->
                val kv = StringTools.stripSuffix(StringTools.stripPrefix(x.trim(), "|"), "|").split('|').map { x -> x.trim() }
                kv[0] to kv[1]
            }.collect(Collectors.toMap({ x -> x.first }, { x -> x.second }))

            val selected = showCheckboxDialog(
                    "Select which items to rename",
                    renames.keys.toTypedArray(),
                    renames.map { kv -> "${kv.key} -> ${kv.value}" }.toTypedArray()
            )

            var text = codeElement.text
            renames.filter { x->selected.contains(x.key) }.forEach { kv -> text = text.replace(Regex("(?<![01-9a-zA-Z_])${kv.key}(?![01-9a-zA-Z_])"), kv.value) }
            replaceString(editor.document, codeElement.startOffset, codeElement.endOffset, text)

        }
    }

    /**
    *
    *  Displays a dialog with a list of checkboxes and an OK button.
    *  When OK is pressed, returns with an array of the selected IDs
    *
    *  @param prompt The prompt to display in the dialog.
    *  @param ids The ids of the checkboxes.
    *  @param descriptions The descriptions of the checkboxes.
    *  @return An array of the ids of the checkboxes that were checked.
    */
    fun showCheckboxDialog(prompt: String, ids: Array<String>, descriptions: Array<String>): Array<String> {
        val formBuilder = FormBuilder.createFormBuilder()
        val checkboxes = HashMap<String,JCheckBox>()
        for (i in ids.indices) {
            val checkbox = JCheckBox(descriptions[i], null as Icon?, true)
            checkboxes.put(ids[i], checkbox)
            formBuilder.addComponent(checkbox)
        }
        val result = showOptionDialog(formBuilder.panel, "OK", title = "Select Names to Replace")
        val selectedIds = ArrayList<String>()
        if (result == 0) {
            for ((id,checkbox) in checkboxes) {
                if (checkbox.isSelected) {
                    selectedIds.add(id)
                }
            }
        }
        return selectedIds.toTypedArray()
    }
}