package com.github.simiacryptus.aicoder;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

public abstract class TextReplacementAction extends AnAction {

    public TextReplacementAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    public static @NotNull TextReplacementAction create(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon, @NotNull ActionTextEditorFunction fn) {
        return new TextReplacementAction(text, description, icon) {
            @Override
            protected String edit(@NotNull AnActionEvent e, String previousText) throws IOException {
                return fn.apply(e, previousText);
            }
        };
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        final Caret primaryCaret = caretModel.getPrimaryCaret();
        final String newText;
        try {
            newText = edit(e, primaryCaret.getSelectedText());
            WriteCommandAction.runWriteCommandAction(e.getProject(), () -> {
                editor.getDocument().replaceString(primaryCaret.getSelectionStart(), primaryCaret.getSelectionEnd(), newText);
            });
        } catch (IOException ex) {
            AICoderMainMenu.handle(ex);
        }

    }

    protected abstract String edit(@NotNull AnActionEvent e, String previousText) throws IOException;

    public interface ActionTextEditorFunction {
        String apply(AnActionEvent actionEvent, String input) throws IOException;
    }

}
