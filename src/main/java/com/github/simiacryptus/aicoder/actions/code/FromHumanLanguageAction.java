package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;

/**
 *  The FromHumanLanguageAction class is an action that is used to convert a human language specification into a computer language.
 *  It is triggered when the user selects a text in the editor and selects the action.
 *  The action will then replace the selected text with the OpenAI-translated version.
*/
public class FromHumanLanguageAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if(!UITools.hasSelection(e)) return false;
        if(null == ComputerLanguage.getComputerLanguage(e)) return false;
        return true;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        final @NotNull Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final @NotNull CaretModel caretModel = editor.getCaretModel();
        final @NotNull Caret primaryCaret = caretModel.getPrimaryCaret();
        int selectionStart = primaryCaret.getSelectionStart();
        int selectionEnd = primaryCaret.getSelectionEnd();
        @Nullable String selectedText = primaryCaret.getSelectedText();
        @NotNull CompletionRequest request = AppSettingsState.getInstance().createTranslationRequest()
                .setInputType(AppSettingsState.getInstance().humanLanguage.toLowerCase())
                .setOutputType(requireNonNull(ComputerLanguage.getComputerLanguage(event)).name())
                .setInstruction("Implement this specification")
                .setInputAttribute("type", "input")
                .setOutputAttrute("type", "output")
                .setInputText(selectedText)
                .buildCompletionRequest();
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence indent = UITools.getIndent(caret);
        UITools.redoableRequest(request, indent, event,
                newText -> replaceString(editor.getDocument(), selectionStart, selectionEnd, newText));
    }
}
