package com.github.simiacryptus.aicoder.actions.generic;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


/**
 * The GenericAppend IntelliJ action allows users to quickly append a prompt to the end of a selected text.
 * To use, select some text and then select the GenericAppend action from the editor context menu.
 * The action will insert the completion at the end of the selected text.
 */
public class GenericAppend extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    @SuppressWarnings("unused")
    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if (!UITools.hasSelection(e)) return false;
        return true;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        @Nullable CharSequence before = Objects.requireNonNull(caret).getSelectedText();
        AppSettingsState settings = AppSettingsState.getInstance();
        @NotNull CompletionRequest completionRequest = settings.createCompletionRequest().appendPrompt(before);
        @NotNull Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        int selectionEnd = caret.getSelectionEnd();
        UITools.redoableRequest(completionRequest, "", event,
                newText -> UITools.insertString(document, selectionEnd, newText));
    }
}
