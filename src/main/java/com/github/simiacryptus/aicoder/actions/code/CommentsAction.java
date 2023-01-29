package com.github.simiacryptus.aicoder.actions.code;

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
import org.jetbrains.annotations.Nullable;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;


/**
 * The CommentsAction class is an IntelliJ action that allows users to add detailed comments to their code.
 * To use the CommentsAction, first select a block of code in the editor.
 * Then, select the action in the context menu.
 * The action will then generate a new version of the code with comments added.
*/
public class CommentsAction extends AnAction {

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
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final @NotNull Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final @NotNull CaretModel caretModel = editor.getCaretModel();
        final @NotNull Caret primaryCaret = caretModel.getPrimaryCaret();
        int selectionStart = primaryCaret.getSelectionStart();
        int selectionEnd = primaryCaret.getSelectionEnd();
        @Nullable String selectedText = primaryCaret.getSelectedText();
        @NotNull String outputHumanLanguage = AppSettingsState.getInstance().humanLanguage;
        @Nullable ComputerLanguage language = ComputerLanguage.getComputerLanguage(e);
        AppSettingsState settings = AppSettingsState.getInstance();
        @NotNull CompletionRequest request = settings.createTranslationRequest()
                .setInputType(requireNonNull(language).name())
                .setOutputType(language.name())
                .setInstruction(UITools.getInstruction("Rewrite to include detailed " + outputHumanLanguage + " code comments for every line"))
                .setInputAttribute("type", "commented")
                .setOutputAttrute("type", "uncommented")
                .setOutputAttrute("style", settings.style)
                .setInputText(selectedText)
                .buildCompletionRequest();
        @Nullable Caret caret = e.getData(CommonDataKeys.CARET);
        CharSequence indent = UITools.getIndent(caret);
        UITools.redoableRequest(request, indent, e,
                newText -> replaceString(editor.getDocument(), selectionStart, selectionEnd, newText));
    }
}
