package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.util.*;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;

public class DescribeAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
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
        @Nullable ComputerLanguage language = ComputerLanguage.getComputerLanguage(event);
        assert language != null;

        if(null == selectedText || selectedText.isEmpty()) {
            @NotNull Document document = editor.getDocument();
            int lineNumber = document.getLineNumber(selectionStart);
            int lineStartOffset = document.getLineStartOffset(lineNumber);
            int lineEndOffset = document.getLineEndOffset(lineNumber);
            @NotNull String currentLine = document.getText().substring(lineStartOffset, lineEndOffset);
            selectionStart = lineStartOffset;
            selectionEnd = lineEndOffset;
            selectedText = currentLine;
        }

        actionPerformed(event, editor, selectionStart, selectionEnd, selectedText, language);
    }

    private static void actionPerformed(@NotNull AnActionEvent event, @NotNull Editor editor, int selectionStart, int selectionEnd, String selectedText, @NotNull ComputerLanguage language) {
        CharSequence indent = UITools.getIndent(event);
        AppSettingsState settings = AppSettingsState.getInstance();
        @NotNull CompletionRequest request = settings.createTranslationRequest()
                .setInputType(Objects.requireNonNull(language).name())
                .setOutputType(settings.humanLanguage)
                .setInstruction(UITools.getInstruction("Explain this " + language.name() + " in " + settings.humanLanguage))
                .setInputAttribute("type", "code")
                .setOutputAttrute("type", "description")
                .setOutputAttrute("style", settings.style)
                .setInputText(IndentedText.fromString(selectedText).getTextBlock().trim())
                .buildCompletionRequest();
        UITools.redoableRequest(request, indent, event, newText -> transformCompletion(selectedText, language, indent, newText), newText -> replaceString(editor.getDocument(), selectionStart, selectionEnd, newText));
    }

    @NotNull
    private static CharSequence transformCompletion(String selectedText, @NotNull ComputerLanguage language, CharSequence indent, @NotNull CharSequence x) {
        @NotNull String wrapping = StringTools.lineWrapping(x.toString().trim(), 120);
        @Nullable TextBlockFactory<?> commentStyle;
        if(wrapping.trim().split("\n").length == 1) {
            commentStyle = language.lineComment;
        } else {
            commentStyle = language.blockComment;
        }
        return "\n" + indent + Objects.requireNonNull(commentStyle).fromString(wrapping).withIndent(indent) + "\n" + indent + selectedText;
    }
}
