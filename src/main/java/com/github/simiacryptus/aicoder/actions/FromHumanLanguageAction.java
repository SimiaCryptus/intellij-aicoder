package com.github.simiacryptus.aicoder.actions;

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

public class FromHumanLanguageAction extends AnAction {

    public FromHumanLanguageAction() {
        super("", "", null);
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
        UITools.redoableRequest(request, indent, event, newText -> replaceString(editor.getDocument(), selectionStart, selectionEnd, newText));
    }
}
