package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.EditRequest;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

public class GenericEdit extends AnAction {

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
        AppSettingsState settings = AppSettingsState.getInstance();
        String instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Text", JOptionPane.QUESTION_MESSAGE);
        settings.addInstructionToHistory(instruction);

        Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence selectedText = Objects.requireNonNull(caret).getSelectedText();
        EditRequest editRequest = settings.createEditRequest()
                .setInput(selectedText.toString())
                .setInstruction(instruction);
        Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        int selectionEnd = caret.getSelectionEnd();
        int selectionStart = caret.getSelectionStart();
        UITools.redoableRequest(editRequest, "", event,
                newText -> UITools.replaceString(document, selectionStart, selectionEnd, newText));
    }
}
