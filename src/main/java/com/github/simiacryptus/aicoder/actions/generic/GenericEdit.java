package com.github.simiacryptus.aicoder.actions.generic;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.EditRequest;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;


/**
 * The GenericEdit action is an IntelliJ action that allows users to edit text in their code.
 * When the action is triggered, a dialog box will appear prompting the user to enter an instruction.
 * The instruction will be added to the history and used to edit the selected text.
 * The action will then replace the selected text with the edited version.
 *
 * To use the GenericEdit action, first select the text you want to edit.
 * Then, select the action in the context menu.
 * A dialog box will appear prompting you to enter an instruction.
 * Enter the instruction and press OK.
 * The selected text will then be replaced with the edited version.
*/
public class GenericEdit extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    @SuppressWarnings("unused")
    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if(!UITools.INSTANCE.hasSelection(e)) return false;
        return true;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        AppSettingsState settings = AppSettingsState.getInstance();
        String instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Text", JOptionPane.QUESTION_MESSAGE);
        settings.addInstructionToHistory(instruction);

        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        @Nullable CharSequence selectedText = Objects.requireNonNull(caret).getSelectedText();
        @NotNull EditRequest editRequest = settings.createEditRequest()
                .setInput(selectedText.toString())
                .setInstruction(instruction);
        @NotNull Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        int selectionEnd = caret.getSelectionEnd();
        int selectionStart = caret.getSelectionStart();
        UITools.INSTANCE.redoableRequest(editRequest, "", event,
                newText -> UITools.INSTANCE.replaceString(document, selectionStart, selectionEnd, newText));
    }
}
