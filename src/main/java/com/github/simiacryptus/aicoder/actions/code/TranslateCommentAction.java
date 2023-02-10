package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.StringTools;
import com.github.simiacryptus.aicoder.util.TextBlockFactory;
import com.github.simiacryptus.aicoder.util.UITools;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;


/**
 * The RewordCommentAction is an IntelliJ action that allows users to reword comments in their code.
 * It is triggered when the user selects a comment in their code and then clicks the RewordCommentAction button.
 * This will replace the old comment with the new one.
 */
public class TranslateCommentAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if (null == ComputerLanguage.getComputerLanguage(e)) return false;
        return null != getRewordCommentParams(e);
    }

    @Nullable
    public static RewordCommentParams getRewordCommentParams(@NotNull AnActionEvent e) {
        @Nullable PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        @Nullable Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        PsiElement largestIntersectingComment = PsiUtil.getLargestIntersectingComment(psiFile, selectionStart, selectionEnd);
        if (largestIntersectingComment == null) return null;
        return new RewordCommentParams(caret, largestIntersectingComment);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        @NotNull String humanLanguage = AppSettingsState.getInstance().humanLanguage;
        ComputerLanguage computerLanguage = Objects.requireNonNull(ComputerLanguage.getComputerLanguage(event));
        RewordCommentParams rewordCommentParams = Objects.requireNonNull(getRewordCommentParams(event));
        final @NotNull Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        AppSettingsState settings = AppSettingsState.getInstance();
        PsiElement largestIntersectingComment = rewordCommentParams.largestIntersectingComment;
        CharSequence indent = UITools.getIndent(rewordCommentParams.caret);
        String text = largestIntersectingComment.getText();
        TextRange textRange = largestIntersectingComment.getTextRange();
        @Nullable TextBlockFactory<?> commentModel = Objects.requireNonNull(computerLanguage.getCommentModel(text));
        @NotNull String commentText = commentModel.fromString(text.trim()).stream()
                .map(Object::toString)
                .reduce((a, b) -> a + "\n" + b).get();
        @NotNull CompletionRequest request = settings.createCompletionRequest()
                .appendPrompt(String.format(
                        "TRANSLATE INTO %s\nINPUT:\n\t%s\n%s:\n",
                        humanLanguage.toUpperCase(),
                        commentText.replace("\n", "\n\t"),
                        humanLanguage.toUpperCase()
                ));
        @NotNull Document document = editor.getDocument();
        UITools.redoableRequest(request, "", event,
                newText -> indent.toString() + commentModel.fromString(newText.toString().trim()).withIndent(indent),
                newText -> replaceString(document, textRange.getStartOffset(), textRange.getEndOffset(), newText));
    }

    public static class RewordCommentParams {
        public final Caret caret;
        public final PsiElement largestIntersectingComment;

        private RewordCommentParams(Caret caret, PsiElement largestIntersectingComment) {
            this.caret = caret;
            this.largestIntersectingComment = largestIntersectingComment;
        }

    }
}
