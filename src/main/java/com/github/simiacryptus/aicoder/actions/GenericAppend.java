package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class GenericAppend extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    @SuppressWarnings("unused")
    private static boolean isEnabled(@NotNull AnActionEvent e) {
        Caret data = e.getData(CommonDataKeys.CARET);
        if (!data.hasSelection()) return false;
        return true;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence before = Objects.requireNonNull(caret).getSelectedText();
        AppSettingsState settings = AppSettingsState.getInstance();
        CompletionRequest completionRequest = settings.createCompletionRequest().appendPrompt(before);
        Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        int selectionEnd = caret.getSelectionEnd();
        UITools.redoableRequest(completionRequest, "", event, newText -> UITools.insertString(document, selectionEnd, newText));
    }
}
