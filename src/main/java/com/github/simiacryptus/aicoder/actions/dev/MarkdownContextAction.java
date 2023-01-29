package com.github.simiacryptus.aicoder.actions.dev;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.psi.PsiMarkdownContext;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;


/**
 * The MarkdownContextAction is an action in IntelliJ that allows users to quickly create a Markdown document with the selected text.
 *
 * Work In Progress
*/
public class MarkdownContextAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if(!AppSettingsState.getInstance().devActions) return false;
        ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(e);
        if (null == computerLanguage) return false;
        if (ComputerLanguage.Markdown != computerLanguage) return false;
        return null != getMarkdownContextParams(e, AppSettingsState.getInstance().humanLanguage);
    }

    @Nullable
    public static MarkdownContextParams getMarkdownContextParams(@NotNull AnActionEvent e, CharSequence humanLanguage) {
        @Nullable Caret caret = e.getData(CommonDataKeys.CARET);
        if (null != caret) {
            int selectionStart = caret.getSelectionStart();
            int selectionEnd = caret.getSelectionEnd();
            if (selectionStart < selectionEnd) {
                return new MarkdownContextParams(humanLanguage, selectionStart, selectionEnd);
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        final @NotNull Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final @NotNull CaretModel caretModel = editor.getCaretModel();
        final @NotNull Caret primaryCaret = caretModel.getPrimaryCaret();
        int selectionStart = primaryCaret.getSelectionStart();
        int selectionEnd = primaryCaret.getSelectionEnd();
        @Nullable String selectedText = primaryCaret.getSelectedText();
        @NotNull String humanLanguage = AppSettingsState.getInstance().humanLanguage;
        @Nullable MarkdownContextParams markdownContextParams = getMarkdownContextParams(event, humanLanguage);
        AppSettingsState settings = AppSettingsState.getInstance();
        @NotNull PsiFile psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE);
        @NotNull String context = PsiMarkdownContext.getContext(
                psiFile,
                requireNonNull(markdownContextParams).selectionStart,
                markdownContextParams.selectionEnd
        ).toString(markdownContextParams.selectionEnd);
        context = context + "\n<!-- " + selectedText + "-->\n";
        context = context + "\n";
        @NotNull CompletionRequest request = settings.createTranslationRequest()
                .setOutputType("markdown")
                .setInstruction(UITools.getInstruction(String.format("Using Markdown and %s", markdownContextParams.humanLanguage)))
                .setInputType("instruction")
                .setInputText(selectedText)
                .setOutputAttrute("type", "document")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest()
                .appendPrompt(context);
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence indent = UITools.getIndent(caret);
        UITools.redoableRequest(request, indent, event,
                newText -> replaceString(editor.getDocument(), selectionStart, selectionEnd, newText));
    }

    public static class MarkdownContextParams {
        public final CharSequence humanLanguage;
        public final int selectionStart;
        public final int selectionEnd;

        private MarkdownContextParams(CharSequence humanLanguage, int selectionStart, int selectionEnd) {
            this.humanLanguage = humanLanguage;
            this.selectionStart = selectionStart;
            this.selectionEnd = selectionEnd;
        }

    }
}
