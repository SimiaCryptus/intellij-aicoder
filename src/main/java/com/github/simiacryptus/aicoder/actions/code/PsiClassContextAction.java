package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.util.UITools;
import com.github.simiacryptus.aicoder.util.psi.PsiClassContext;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class PsiClassContextAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if(null == ComputerLanguage.getComputerLanguage(e)) return false;
        return getPsiClassContextActionParams(e).isPresent();
    }

    public static @NotNull Optional<PsiClassContextActionParams> getPsiClassContextActionParams(@NotNull AnActionEvent e) {
        @Nullable PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null != psiFile) {
            @Nullable Caret caret = e.getData(CommonDataKeys.CARET);
            if (null != caret) {
                int selectionStart = caret.getSelectionStart();
                int selectionEnd = caret.getSelectionEnd();
                PsiElement largestIntersectingComment = PsiUtil.getLargestIntersectingComment(psiFile, selectionStart, selectionEnd);
                if (largestIntersectingComment != null) {
                    return Optional.of(new PsiClassContextActionParams(psiFile, caret, selectionStart, selectionEnd, largestIntersectingComment));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        @NotNull String humanLanguage = AppSettingsState.getInstance().humanLanguage;
        @Nullable ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(event);
        @NotNull PsiClassContextActionParams psiClassContextActionParams = getPsiClassContextActionParams(event).get();
        final @NotNull Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final @NotNull CaretModel caretModel = editor.getCaretModel();
        final @NotNull Caret primaryCaret = caretModel.getPrimaryCaret();
        AppSettingsState settings = AppSettingsState.getInstance();

        @NotNull String instruct = psiClassContextActionParams.largestIntersectingComment.getText().trim();
        if (primaryCaret.getSelectionEnd() > primaryCaret.getSelectionStart()) {
            @NotNull String selectedText = Objects.requireNonNull(primaryCaret.getSelectedText());
            if (Objects.requireNonNull(selectedText).split(" ").length > 4) {
                instruct = selectedText.trim();
            }
        }
        assert computerLanguage != null;
        @NotNull String specification = Objects.requireNonNull(computerLanguage.getCommentModel(instruct)).fromString(instruct).stream()
                .map(Object::toString)
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .reduce((a, b) -> a + " " + b).get();
        int endOffset = psiClassContextActionParams.largestIntersectingComment.getTextRange().getEndOffset();
        @NotNull CompletionRequest request = settings.createTranslationRequest()
                .setInstruction("Implement " + humanLanguage + " as " + computerLanguage.name() + " code")
                .setInputType(humanLanguage)
                .setInputAttribute("type", "instruction")
                .setInputText(specification)
                .setOutputType(computerLanguage.name())
                .setOutputAttrute("type", "code")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest()
                .appendPrompt(PsiClassContext.getContext(psiClassContextActionParams.psiFile, psiClassContextActionParams.selectionStart, psiClassContextActionParams.selectionEnd) + "\n");
        UITools.INSTANCE.redoableRequest(request, UITools.INSTANCE.getIndent(psiClassContextActionParams.caret), event,
                newText -> "\n" + newText,
                newText -> UITools.INSTANCE.insertString(editor.getDocument(), endOffset, newText));
    }

    public static class PsiClassContextActionParams {
        public final PsiFile psiFile;
        public final Caret caret;
        public final int selectionStart;
        public final int selectionEnd;
        public final PsiElement largestIntersectingComment;

        private PsiClassContextActionParams(PsiFile psiFile, Caret caret, int selectionStart, int selectionEnd, PsiElement largestIntersectingComment) {
            this.psiFile = psiFile;
            this.caret = caret;
            this.selectionStart = selectionStart;
            this.selectionEnd = selectionEnd;
            this.largestIntersectingComment = largestIntersectingComment;
        }

    }
}
