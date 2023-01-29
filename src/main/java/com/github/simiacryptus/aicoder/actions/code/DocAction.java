package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.github.simiacryptus.aicoder.util.*;
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
 *  The DocAction is an IntelliJ action that enables users to add detailed documentation to their code.
 *  It works by taking the current code element and translating it into a documentation comment.
 *  The style of the comments is determined by the user's settings.
 *
 *  To use DocAction, place the cursor on the code member they wish to document.
 *  Then, select the action in the context menu.
 *  DocAction will then take the selected code, translate it into a comment, and prepend the original code with the new doc comment.
 *
 *  DocAction is a useful tool for quickly adding detailed documentation to code.
 *  It can save time and effort, and make code easier to read and understand.
*/
public class DocAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent event) {
        @Nullable ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(event);
        if (null == computerLanguage) return false;
        if (null == computerLanguage.docStyle) return false;
        if (computerLanguage.docStyle.isEmpty()) return false;
        @NotNull PsiFile psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE);
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        PsiElement smallestIntersectingMethod = PsiUtil.getSmallestIntersectingMajorCodeElement(psiFile, requireNonNull(caret));
        if (null == smallestIntersectingMethod) return false;
        return true;
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
