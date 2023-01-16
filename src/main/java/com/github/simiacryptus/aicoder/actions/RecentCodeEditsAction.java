package com.github.simiacryptus.aicoder.actions;

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

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;

public class RecentCodeEditsAction extends ActionGroup {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        Caret data = e.getData(CommonDataKeys.CARET);
        if (!data.hasSelection()) return false;
        return null != ComputerLanguage.getComputerLanguage(e);
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent event) {
        if (event == null) return new AnAction[]{};
        assert event != null;
        String computerLanguage = requireNonNull(ComputerLanguage.getComputerLanguage(event)).name();
        ArrayList<AnAction> children = new ArrayList<>();
        for (String instruction : AppSettingsState.getInstance().getEditHistory()) {
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
                    final Editor editor = event1.getRequiredData(CommonDataKeys.EDITOR);
                    final CaretModel caretModel = editor.getCaretModel();
                    final Caret primaryCaret = caretModel.getPrimaryCaret();
                    int selectionStart = primaryCaret.getSelectionStart();
                    int selectionEnd = primaryCaret.getSelectionEnd();
                    String selectedText = primaryCaret.getSelectedText();
                    AppSettingsState settings = AppSettingsState.getInstance();
                    settings.addInstructionToHistory(instruction);
                    CompletionRequest request = settings.createTranslationRequest()
                            .setInputType(computerLanguage)
                            .setOutputType(computerLanguage)
                            .setInstruction(instruction)
                            .setInputAttribute("type", "before")
                            .setOutputAttrute("type", "after")
                            .setInputText(IndentedText.fromString(selectedText).getTextBlock())
                            .buildCompletionRequest();
                    Caret caret = event1.getData(CommonDataKeys.CARET);
                    CharSequence indent = UITools.getIndent(caret);
                    Document document = editor.getDocument();
                    UITools.redoableRequest(request, indent, event1,
                            newText -> replaceString(document, selectionStart, selectionEnd, newText));
                }
            });
        }
        return children.toArray(AnAction[]::new);
    }
}
