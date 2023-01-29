package com.github.simiacryptus.aicoder.actions.code;

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
import org.jetbrains.annotations.Nullable;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;


/**
 * The ToHumanLanguageAction class is an action that is used to translate code written in a computer language into a human language.
 * It is triggered when the user selects a piece of code and then clicks the action.
 * The action will then send a request to the server to translate the code into the human language specified in the AppSettingsState.
 * The translated text will then be inserted into the document at the location of the selected code.
*/
public class ToHumanLanguageAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if(!UITools.hasSelection(e)) return false;
        if(null == ComputerLanguage.getComputerLanguage(e)) return false;
        return true;
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
        @NotNull String computerLanguage = requireNonNull(language).name();
        AppSettingsState settings = AppSettingsState.getInstance();
        @NotNull CompletionRequest request = settings.createTranslationRequest()
                .setInstruction(UITools.getInstruction("Describe this code"))
                .setInputText(selectedText)
                .setInputType(computerLanguage)
                .setInputAttribute("type", "input")
                .setOutputType(AppSettingsState.getInstance().humanLanguage.toLowerCase())
                .setOutputAttrute("type", "output")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest();
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence indent = UITools.getIndent(caret);
        @NotNull Document document = editor.getDocument();
        UITools.redoableRequest(request, indent, event,
                newText -> replaceString(document, selectionStart, selectionEnd, newText));
    }
}
