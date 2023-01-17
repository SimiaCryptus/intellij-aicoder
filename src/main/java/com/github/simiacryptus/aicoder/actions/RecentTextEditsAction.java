package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.EditRequest;
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

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;

public class RecentTextEditsAction extends ActionGroup {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        @Nullable Caret data = e.getData(CommonDataKeys.CARET);
        return data.hasSelection();
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent event) {
        if (event == null) return new AnAction[]{};
        @NotNull ArrayList<AnAction> children = new ArrayList<>();
        for (@NotNull String instruction : AppSettingsState.getInstance().getEditHistory()) {
            int id = children.size() + 1;
            String text;
            if(id<10) {
                text = String.format("_%d: %s", id, instruction);
            } else {
                text = String.format("%d: %s", id, instruction);
            }
            children.add(new AnAction(text, instruction, null) {
                @Override
                public void actionPerformed(@NotNull final AnActionEvent event1) {
                    final @NotNull Editor editor = event1.getRequiredData(CommonDataKeys.EDITOR);
                    final @NotNull CaretModel caretModel = editor.getCaretModel();
                    final @NotNull Caret primaryCaret = caretModel.getPrimaryCaret();
                    int selectionStart = primaryCaret.getSelectionStart();
                    int selectionEnd = primaryCaret.getSelectionEnd();
                    @Nullable String selectedText = primaryCaret.getSelectedText();
                    AppSettingsState settings = AppSettingsState.getInstance();
                    settings.addInstructionToHistory(instruction);
                    @NotNull EditRequest request = settings.createEditRequest()
                            .setInstruction(instruction)
                            .setInput(IndentedText.fromString(selectedText).getTextBlock());
                    @Nullable Caret caret = event1.getData(CommonDataKeys.CARET);
                    CharSequence indent = UITools.getIndent(caret);
                    @NotNull Document document = editor.getDocument();
                    UITools.redoableRequest(request, indent, event1,
                            newText -> replaceString(document, selectionStart, selectionEnd, newText));
                }
            });
        }
        return children.toArray(AnAction[]::new);
    }
}
