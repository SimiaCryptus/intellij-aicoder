package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.IndentedText;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static java.util.Objects.requireNonNull;


/**
 * The RecentCodeEditsAction is an IntelliJ action that allows users to quickly access and apply recent code edits.
 * This action is triggered when a user selects a piece of code and then right-clicks to bring up the context menu.
 * The RecentCodeEditsAction will then display a list of recent code edits that the user can select from and apply to the selected code.
 * When the user selects a code edit, the action will generate a new version of the selected code with the code edit applied.
 * Finally, the new version of the code will be inserted into the document, replacing the original code.
 */
public class RecentCodeEditsAction extends ActionGroup {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if (!UITools.INSTANCE.hasSelection(e)) return false;
        if (null == ComputerLanguage.getComputerLanguage(e)) return false;
        return true;
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        @NotNull ArrayList<AnAction> children = new ArrayList<>();
        for (@NotNull String instruction : AppSettingsState.getInstance().getEditHistory()) {
            int id = children.size() + 1;
            String text;
            if (id < 10) {
                text = String.format("_%d: %s", id, instruction);
            } else {
                text = String.format("%d: %s", id, instruction);
            }
            children.add(new AnAction(text, instruction, null) {
                @Override
                public void actionPerformed(@NotNull final AnActionEvent event) {
                    @NotNull String computerLanguage = requireNonNull(ComputerLanguage.getComputerLanguage(event)).name();
                    final @NotNull Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                    final @NotNull CaretModel caretModel = editor.getCaretModel();
                    final @NotNull Caret primaryCaret = caretModel.getPrimaryCaret();
                    int selectionStart = primaryCaret.getSelectionStart();
                    int selectionEnd = primaryCaret.getSelectionEnd();
                    @Nullable String selectedText = primaryCaret.getSelectedText();
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
                    @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
                    CharSequence indent = UITools.INSTANCE.getIndent(caret);
                    @NotNull Document document = editor.getDocument();
                    UITools.INSTANCE.redoableRequest(request, indent, event,
                            newText -> UITools.INSTANCE.replaceString(document, selectionStart, selectionEnd, newText));
                }
            });
        }
        return children.toArray(AnAction[]::new);
    }
}
