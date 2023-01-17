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
import com.intellij.openapi.ide.CopyPasteManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.util.Objects;

import static com.github.simiacryptus.aicoder.util.UITools.insertString;
import static com.github.simiacryptus.aicoder.util.UITools.replaceString;

public class PasteAction extends AnAction {

    public PasteAction() {
        super("", "Paste", null);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if(CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor) == null) return false;
        return null != ComputerLanguage.getComputerLanguage(e);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        final @NotNull Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final @NotNull CaretModel caretModel = editor.getCaretModel();
        final @NotNull Caret primaryCaret = caretModel.getPrimaryCaret();
        int selectionStart = primaryCaret.getSelectionStart();
        int selectionEnd = primaryCaret.getSelectionEnd();
        @Nullable String selectedText = primaryCaret.getSelectedText();
        @NotNull String language = Objects.requireNonNull(ComputerLanguage.getComputerLanguage(event)).name();
        @NotNull String text = Objects.requireNonNull(CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor)).toString().trim();
        @NotNull CompletionRequest request = AppSettingsState.getInstance().createTranslationRequest()
                .setInputType("source")
                .setOutputType("translated")
                .setInstruction("Translate this input into " + language)
                .setInputAttribute("language", "autodetect")
                .setOutputAttrute("language", language)
                .setInputText(text)
                .buildCompletionRequest();
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence indent = UITools.getIndent(caret);
        UITools.redoableRequest(request, indent, event, newText -> {
            if(selectedText == null) {
                return insertString(editor.getDocument(), selectionStart, newText);
            } else {
                return replaceString(editor.getDocument(), selectionStart, selectionEnd, newText);
            }
        });
    }
}
