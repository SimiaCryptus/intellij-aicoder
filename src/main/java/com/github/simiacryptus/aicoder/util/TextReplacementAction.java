package com.github.simiacryptus.aicoder.util;

import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.ModerationException;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;

/**
 * TextReplacementAction is an abstract class that extends the AnAction class.
 * It provides a static method create() that takes in a text, description, icon, and an ActionTextEditorFunction.
 * It also provides an actionPerformed() method that is called when the action is performed.
 * This method gets the editor, caret model, and primary caret from the AnActionEvent.
 * It then calls the edit() method, which is implemented by the subclasses, and replaces the selected text with the new text.
 * The ActionTextEditorFunction is a functional interface that takes in an AnActionEvent and a String and returns a String.
 */
public class TextReplacementAction extends AnAction {

    private final ActionTextEditorFunction fn;

    private TextReplacementAction(@Nullable @NlsActions.ActionText CharSequence text, @Nullable @NlsActions.ActionDescription CharSequence description, @Nullable Icon icon, @NotNull ActionTextEditorFunction fn) {
        super(text.toString(), description.toString(), icon);
        this.fn = fn;
    }

    public static @NotNull TextReplacementAction create(@Nullable @NlsActions.ActionText CharSequence text, @Nullable @NlsActions.ActionDescription CharSequence description, @Nullable Icon icon, @NotNull ActionTextEditorFunction fn) {
        return new TextReplacementAction(text, description, icon, fn);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        final Caret primaryCaret = caretModel.getPrimaryCaret();
        try {
            int selectionStart = primaryCaret.getSelectionStart();
            int selectionEnd = primaryCaret.getSelectionEnd();
            String selectedText = primaryCaret.getSelectedText();
            CompletionRequest request = fn.apply(e, selectedText);
            Caret caret = e.getData(CommonDataKeys.CARET);
            CharSequence indent = UITools.getIndent(caret);
            UITools.redoableRequest(request, indent, e, (CharSequence x) -> {
                CharSequence newText = fn.postTransform(e, selectedText, x);
                return replaceString(editor.getDocument(), selectionStart, selectionEnd, newText);
            });
        } catch (ModerationException | IOException ex) {
            UITools.handle(ex);
        }
    }

    public interface ActionTextEditorFunction {
        CompletionRequest apply(AnActionEvent actionEvent, String input) throws IOException, ModerationException;

        /**
         *
         *   Override this method to post-transform the completion string.
         *
         *   @param event The action event
         *   @param prompt The prompt string
         *   @param completion The completion string
         *   @return The transformed string
         */
        default CharSequence postTransform(AnActionEvent event, CharSequence prompt, CharSequence completion) { return completion; }
    }

}
