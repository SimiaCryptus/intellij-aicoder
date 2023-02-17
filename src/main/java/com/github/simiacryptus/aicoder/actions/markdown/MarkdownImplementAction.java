package com.github.simiacryptus.aicoder.actions.markdown;

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


/**
 * The MarkdownImplementAction is an IntelliJ action that allows users to quickly insert code snippets into markdown documents.
 * This action is triggered when a user selects a piece of text and then selects the action from the editor context menu.
 * The action will then generate a markdown code block and insert it into the document at the end of the selection.
 *
 * To use the MarkdownImplementAction, first select the text that you want to be included in the markdown code block.
 * Then, select the action in the context menu.
 * The action will generate a markdown code block and insert it into the document at the end of the selection.
*/
public class MarkdownImplementAction extends AnAction {

    private final String language;

    public MarkdownImplementAction(String language) {
        super(language);
        this.language = language;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(e);
        if (null == computerLanguage) return false;
        if (ComputerLanguage.Markdown != computerLanguage) return false;
        if (!UITools.INSTANCE.hasSelection(e)) return false;
        return true;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        AppSettingsState settings = AppSettingsState.getInstance();
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        String selectedText = caret.getSelectedText();
        int endOffset = caret.getSelectionEnd();
        @NotNull CompletionRequest completionRequest = settings.createCompletionRequest()
                .appendPrompt(String.format("%s\n```%s\n", selectedText, language))
                .addStops("```");
        @NotNull Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        UITools.INSTANCE.redoableRequest(completionRequest, "", event,
                docString -> String.format("\n```%s\n%s\n```", language, docString),
                docString -> UITools.INSTANCE.insertString(document, endOffset, docString));
    }

}
