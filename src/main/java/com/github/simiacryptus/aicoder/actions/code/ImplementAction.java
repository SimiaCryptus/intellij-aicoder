package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.*;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.github.simiacryptus.aicoder.util.StringTools.stripPrefix;
import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;

public class ImplementAction extends AnAction {
    private static final List<ComputerLanguage> SUPPORTED_LANGUAGES = Arrays.asList(
            ComputerLanguage.Java,
            ComputerLanguage.Scala
    );

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent event) {
        if (!AppSettingsState.getInstance().devActions) return false;
        @Nullable ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(event);
        if (null == computerLanguage) return false;
        //if (!SUPPORTED_LANGUAGES.contains(computerLanguage)) return false;
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        @NotNull PsiFile psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE);
        PsiElement smallestIntersectingMethod = PsiUtil.getSmallestIntersectingMajorCodeElement(psiFile, requireNonNull(caret));
        if (!isStub(smallestIntersectingMethod)) return false;
        return true;
    }

    private static boolean isStub(@NotNull PsiElement element) {
        String compact = compacted(element).trim();
        String declaration = PsiUtil.getDeclaration(element);
        String sansComments = stripPrefix(compact, declaration).trim();
        if (sansComments.isBlank()) return true;
        if (Pattern.compile("(?s)\\{\\s*}").matcher(sansComments).matches()) return true;
        return false;
    }

    private static String compacted(PsiElement element) {
        StringBuffer sb = new StringBuffer();
        element.accept(new PsiElementVisitor() {
            public void visitElement(PsiElement element) {
                if (PsiUtil.matchesType(element, "Comment", "DocComment")) {
                    // Ignore
                } else if (PsiUtil.matchesType(element, "Method", "CodeBlock")) {
                    element.acceptChildren(this);
                } else {
                    sb.append(element.getText());
                }
                super.visitElement(element);
            }
        });
        return sb.toString();
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        @NotNull PsiFile psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE);
        PsiElement smallestIntersectingMethod = PsiUtil.getSmallestIntersectingMajorCodeElement(psiFile, requireNonNull(caret));
        if (null == smallestIntersectingMethod) return;
        AppSettingsState settings = AppSettingsState.getInstance();
        String code = smallestIntersectingMethod.getText();
        @NotNull IndentedText indentedInput = IndentedText.fromString(code);
        String declaration = PsiUtil.getDeclaration(smallestIntersectingMethod);
        @NotNull CompletionRequest completionRequest = settings.createCompletionRequest()
                //.appendPrompt("<code>").addStops("</code>")
                .appendPrompt(declaration);
        @NotNull Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        TextRange textRange = smallestIntersectingMethod.getTextRange();
        UITools.redoableRequest(completionRequest, "", event,
                string -> IndentedText.fromString(declaration + string.toString().trim()).withIndent(indentedInput.getIndent()).toString(),
                docString -> replaceString(document, textRange.getStartOffset(), textRange.getEndOffset(), docString));
    }

}
