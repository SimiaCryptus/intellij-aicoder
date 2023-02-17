package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.IndentedText;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static java.util.Objects.requireNonNull;


/**
 * The CustomEditAction class is an IntelliJ action that allows users to edit computer language code.
 * When the action is triggered, a dialog box will appear prompting the user to enter an instruction.
 * The instruction will then be used to transform the selected code.
 *
 * To use the CustomEditAction, first select the code that you want to edit.
 * Then, select the action in the context menu.
 * A dialog box will appear, prompting you to enter an instruction.
 * Enter the instruction and press OK.
*/
public class CustomEditAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if(!UITools.INSTANCE.hasSelection(e)) return false;
        if(null == ComputerLanguage.getComputerLanguage(e)) return false;
        return true;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final @NotNull Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final @NotNull CaretModel caretModel = editor.getCaretModel();
        final @NotNull Caret primaryCaret = caretModel.getPrimaryCaret();
        int selectionStart = primaryCaret.getSelectionStart();
        int selectionEnd = primaryCaret.getSelectionEnd();
        @Nullable String selectedText = primaryCaret.getSelectedText();
        @NotNull String computerLanguage = requireNonNull(ComputerLanguage.getComputerLanguage(e)).name();
        String instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE);
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.addInstructionToHistory(instruction);
        @NotNull CompletionRequest request = settings.createTranslationRequest()
                .setInputType(computerLanguage)
                .setOutputType(computerLanguage)
                .setInstruction(instruction)
                .setInputAttribute("type", "before")
                .setOutputAttrute("type", "after")
                .setInputText(IndentedText.fromString(selectedText).getTextBlock())
                .buildCompletionRequest();
        @Nullable Caret caret = e.getData(CommonDataKeys.CARET);
        CharSequence indent = UITools.INSTANCE.getIndent(caret);
        UITools.INSTANCE.redoableRequest(request, indent, e,
                newText -> UITools.INSTANCE.replaceString(editor.getDocument(), selectionStart, selectionEnd, newText));
    }
}
