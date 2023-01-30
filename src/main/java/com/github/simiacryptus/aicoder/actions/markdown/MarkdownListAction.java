package com.github.simiacryptus.aicoder.actions.markdown;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.github.simiacryptus.aicoder.util.StringTools;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.simiacryptus.aicoder.util.UITools.insertString;


/**
 * The MarkdownListAction class is an action that allows users to quickly expand a list of items in IntelliJ.
 * It is triggered when the user selects a list in the markdown editor and then invokes the action.
 * The action will then use current list items to generate further items via OpenAI's GPT-3 API.
 * These new items will be inserted into the document at the end of the list.
 */
public class MarkdownListAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(e);
        if (null == computerLanguage) return false;
        if (ComputerLanguage.Markdown != computerLanguage) return false;
        return null != getMarkdownListParams(e);
    }

    @Nullable
    public static MarkdownListParams getMarkdownListParams(@NotNull AnActionEvent e) {
        @Nullable Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        @Nullable PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        PsiElement list = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownListImpl");
        if (null == list) return null;
        return new MarkdownListParams(caret, list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        @Nullable MarkdownListParams markdownListParams = getMarkdownListParams(event);
        AppSettingsState settings = AppSettingsState.getInstance();
        @NotNull List<CharSequence> items = StringTools.trim(PsiUtil.getAll(Objects.requireNonNull(markdownListParams).list, "MarkdownListItemImpl")
                .stream().map(item -> PsiUtil.getAll(item, "MarkdownParagraphImpl").get(0).getText()).collect(Collectors.toList()), 10, false);
        CharSequence indent = UITools.getIndent(markdownListParams.caret);
        @NotNull CharSequence n = Integer.toString(items.size() * 2);
        int endOffset = markdownListParams.list.getTextRange().getEndOffset();
        @NotNull String listPrefix = "* ";
        @NotNull CompletionRequest completionRequest = settings.createTranslationRequest()
                .setInstruction(UITools.getInstruction("List " + n + " items"))
                .setInputType("instruction")
                .setInputText("List " + n + " items")
                .setOutputType("list")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest()
                .appendPrompt(items.stream().map(x2 -> listPrefix + x2).collect(Collectors.joining("\n")) + "\n" + listPrefix);
        @NotNull Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        UITools.redoableRequest(completionRequest, "", event,
                newText -> transformCompletion(markdownListParams, indent, listPrefix, newText),
                newText -> insertString(document, endOffset, newText));
    }

    @NotNull
    private static String transformCompletion(@NotNull MarkdownListParams markdownListParams, CharSequence indent, @NotNull String listPrefix, @NotNull CharSequence complete) {
        @NotNull List<CharSequence> newItems = Arrays.stream(complete.toString().split("\n")).map(String::trim)
                .filter(x1 -> x1.length() > 0).map(x1 -> StringTools.stripPrefix(x1, listPrefix)).collect(Collectors.toList());
        String strippedList = Arrays.stream(markdownListParams.list.getText().split("\n"))
                .map(String::trim).filter(x -> x.length() > 0).collect(Collectors.joining("\n"));
        @NotNull String bulletString = Stream.of("- [ ] ", "- ", "* ")
                .filter(strippedList::startsWith).findFirst().orElse("1. ");
        @NotNull CharSequence itemText = indent + newItems.stream().map(x -> bulletString + x)
                .collect(Collectors.joining("\n" + indent));
        return "\n" + itemText;
    }

    public static class MarkdownListParams {
        public final Caret caret;
        public final PsiElement list;

        private MarkdownListParams(Caret caret, PsiElement list) {
            this.caret = caret;
            this.list = list;
        }

    }
}
