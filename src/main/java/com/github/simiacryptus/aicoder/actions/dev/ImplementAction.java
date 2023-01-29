package com.github.simiacryptus.aicoder.actions.dev;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.*;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.github.simiacryptus.aicoder.util.StringTools.trimPrefix;
import static com.github.simiacryptus.aicoder.util.UITools.replaceString;
import static java.util.Objects.requireNonNull;


/**
 * Work in Progress
 */
public class ImplementAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if(!AppSettingsState.getInstance().devActions) return false;
        @Nullable ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(e);
        if (null == computerLanguage) return false;
        if (ComputerLanguage.Markdown == computerLanguage) return false;
        return null != computerLanguage.docStyle && !computerLanguage.docStyle.isEmpty();
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        @Nullable ComputerLanguage language = ComputerLanguage.getComputerLanguage(event);
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        @NotNull PsiFile psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE);
        PsiElement smallestIntersectingMethod = PsiUtil.getSmallestIntersectingMajorCodeElement(psiFile, requireNonNull(caret));
        if (null == smallestIntersectingMethod) return;
        AppSettingsState settings = AppSettingsState.getInstance();
        String code = smallestIntersectingMethod.getText();
        @NotNull IndentedText indentedInput = IndentedText.fromString(code);
        CharSequence indent = indentedInput.getIndent();
        int startOffset = smallestIntersectingMethod.getTextRange().getStartOffset();
        int endOffset = smallestIntersectingMethod.getTextRange().getEndOffset();
        @NotNull CompletionRequest completionRequest = settings.createTranslationRequest()
                .setInputType(requireNonNull(language).name())
                .setOutputType(language.name())
                .setInstruction(UITools.getInstruction("Rewrite to include detailed " + language.docStyle))
                .setInputAttribute("type", "uncommented")
                .setOutputAttrute("type", "commented")
                .setOutputAttrute("style", settings.style)
                .setInputText(indentedInput.getTextBlock())
                .buildCompletionRequest()
                .addStops(requireNonNull(language.getMultilineCommentSuffix()));
        @NotNull Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        UITools.redoableRequest(completionRequest, "", event,
                docString -> requireNonNull(language.docComment).fromString(docString.toString().trim()).withIndent(indent) + "\n" + indent + trimPrefix(indentedInput.toString()),
                docString -> replaceString(document, startOffset, endOffset, docString));
    }

}
