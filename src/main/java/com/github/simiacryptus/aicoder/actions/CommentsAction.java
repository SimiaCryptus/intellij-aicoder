package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;

public class CommentsAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        final Caret primaryCaret = caretModel.getPrimaryCaret();
        int selectionStart = primaryCaret.getSelectionStart();
        int selectionEnd = primaryCaret.getSelectionEnd();
        if(selectionStart == selectionEnd) return false;
        if (null == ComputerLanguage.getComputerLanguage(e)) return false;
        return true;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        final Caret primaryCaret = caretModel.getPrimaryCaret();
        int selectionStart = primaryCaret.getSelectionStart();
        int selectionEnd = primaryCaret.getSelectionEnd();
        String selectedText = primaryCaret.getSelectedText();
        String outputHumanLanguage = AppSettingsState.getInstance().humanLanguage;
        ComputerLanguage language = ComputerLanguage.getComputerLanguage(e);
        AppSettingsState settings = AppSettingsState.getInstance();
        CompletionRequest request = settings.createTranslationRequest()
                .setInputType(requireNonNull(language).name())
                .setOutputType(language.name())
                .setInstruction(UITools.getInstruction("Rewrite to include detailed " + outputHumanLanguage + " code comments for every line"))
                .setInputAttribute("type", "uncommented")
                .setOutputAttrute("type", "commented")
                .setOutputAttrute("style", settings.style)
                .setInputText(selectedText)
                .buildCompletionRequest();
        Caret caret = e.getData(CommonDataKeys.CARET);
        CharSequence indent = UITools.getIndent(caret);
        UITools.redoableRequest(request, indent, e, newText -> replaceString(editor.getDocument(), selectionStart, selectionEnd, newText));
    }
}
