package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.util.UITools;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.github.simiacryptus.aicoder.util.StringTools;
import com.github.simiacryptus.aicoder.util.TextBlockFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The RewordCommentAction is an IntelliJ action that allows users to reword comments in their code.
 * It is triggered when the user selects a comment in their code and then clicks the RewordCommentAction button.
 * This will replace the old comment with the new one.
 */
public class RewordCommentAction extends AnAction {

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
        @Nullable ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(event);
        @Nullable RewordCommentParams rewordCommentParams = getRewordCommentParams(event);
        final @NotNull Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        AppSettingsState settings = AppSettingsState.getInstance();
        String text = Objects.requireNonNull(rewordCommentParams).largestIntersectingComment.getText();
        @Nullable TextBlockFactory<?> commentModel = Objects.requireNonNull(computerLanguage).getCommentModel(text);
        @NotNull String commentText = Objects.requireNonNull(commentModel).fromString(text.trim()).stream()
                .map(Object::toString)
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .reduce((a, b) -> a + "\n" + b).get();
        int startOffset = rewordCommentParams.largestIntersectingComment.getTextRange().getStartOffset();
        int endOffset = rewordCommentParams.largestIntersectingComment.getTextRange().getEndOffset();
        CharSequence indent = UITools.INSTANCE.getIndent(rewordCommentParams.caret);
        @NotNull CompletionRequest request = settings.createTranslationRequest()
                .setInstruction(UITools.INSTANCE.getInstruction("Reword"))
                .setInputText(commentText)
                .setInputType(humanLanguage)
                .setOutputAttrute("type", "input")
                .setOutputType(humanLanguage)
                .setOutputAttrute("type", "output")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest();
        @NotNull Document document = editor.getDocument();
        UITools.INSTANCE.redoableRequest(request, "", event,
                newText -> indent.toString() + commentModel.fromString(StringTools.lineWrapping(newText, 120)).withIndent(indent),
                newText -> UITools.INSTANCE.replaceString(document, startOffset, endOffset, newText));
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
