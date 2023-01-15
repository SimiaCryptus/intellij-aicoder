package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;

public class ToHumanLanguageAction extends AnAction {

    public ToHumanLanguageAction() {
        super("", "", null);
    }

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
        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        final Caret primaryCaret = caretModel.getPrimaryCaret();
        int selectionStart = primaryCaret.getSelectionStart();
        int selectionEnd = primaryCaret.getSelectionEnd();
        String selectedText = primaryCaret.getSelectedText();
        ComputerLanguage language = ComputerLanguage.getComputerLanguage(event);
        String computerLanguage = requireNonNull(language).name();
        AppSettingsState settings = AppSettingsState.getInstance();
        CompletionRequest request = settings.createTranslationRequest()
                .setInstruction(UITools.getInstruction("Describe this code"))
                .setInputText(selectedText)
                .setInputType(computerLanguage)
                .setInputAttribute("type", "input")
                .setOutputType(AppSettingsState.getInstance().humanLanguage.toLowerCase())
                .setOutputAttrute("type", "output")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest();
        Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence indent = UITools.getIndent(caret);
        Document document = editor.getDocument();
        UITools.redoableRequest(request, indent, event, newText -> replaceString(document, selectionStart, selectionEnd, newText));
    }
}
