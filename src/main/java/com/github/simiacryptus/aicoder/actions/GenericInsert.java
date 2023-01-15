package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.StringTools;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class GenericInsert extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    @SuppressWarnings("unused")
    private static boolean isEnabled(@NotNull AnActionEvent e) {
        Caret data = e.getData(CommonDataKeys.CARET);
        if(data.hasSelection()) return false;
        return true;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Caret caret = event.getData(CommonDataKeys.CARET);
        Document document = Objects.requireNonNull(caret).getEditor().getDocument();
        int caretPosition = caret.getOffset();
        CharSequence before = StringTools.getSuffixForContext(document.getText(new TextRange(0, caretPosition)));
        CharSequence after = StringTools.getPrefixForContext(document.getText(new TextRange(caretPosition, document.getTextLength())));
        AppSettingsState settings = AppSettingsState.getInstance();
        CompletionRequest completionRequest = settings.createCompletionRequest()
                .appendPrompt(before)
                .setSuffix(after);
        UITools.redoableRequest(completionRequest, "", event, newText -> UITools.insertString(document, caretPosition, newText));
    }
}
