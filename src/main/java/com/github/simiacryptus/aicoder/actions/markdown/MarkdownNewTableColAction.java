package com.github.simiacryptus.aicoder.actions.markdown;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.StringTools;
import com.github.simiacryptus.aicoder.util.UITools;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;

public class MarkdownNewTableColAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(e);
        if (null == computerLanguage) return false;
        if (ComputerLanguage.Markdown != computerLanguage) return false;
        return null != getMarkdownNewTableColParams(e);
    }

    @Nullable
    public static MarkdownNewTableColParams getMarkdownNewTableColParams(@NotNull AnActionEvent e) {
        @Nullable Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        @Nullable PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        PsiElement table = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownTableImpl");
        if (null == table) return null;
        @NotNull List<CharSequence> rows = Arrays.asList(StringTools.transposeMarkdownTable(PsiUtil.getAll(table, "MarkdownTableRowImpl")
                .stream().map(PsiElement::getText).collect(Collectors.joining("\n")), false, false).split("\n"));
        @NotNull CharSequence n = Integer.toString(rows.size() * 2);
        return new MarkdownNewTableColParams(caret, table, rows, n);
    }

    @NotNull
    public static CompletionRequest newRowsRequest(@NotNull AppSettingsState settings, CharSequence n, @NotNull List<CharSequence> rows, CharSequence rowPrefix) {
        return settings.createTranslationRequest()
                .setInstruction(UITools.getInstruction("List " + n + " items"))
                .setInputType("instruction")
                .setInputText("List " + n + " items")
                .setOutputType("markdown")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest()
                .appendPrompt("\n" + String.join("\n", rows) + "\n" + rowPrefix);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        @Nullable MarkdownNewTableColParams markdownNewTableColParams = getMarkdownNewTableColParams(event);
        AppSettingsState settings = AppSettingsState.getInstance();
        @NotNull CharSequence columnName = JOptionPane.showInputDialog(null, "Column Name:", "Add Column", JOptionPane.QUESTION_MESSAGE).trim();
        @NotNull CompletionRequest request = newRowsRequest(settings, Objects.requireNonNull(markdownNewTableColParams).n, markdownNewTableColParams.rows, "| " + columnName + " | ");
        @NotNull Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        TextRange textRange = markdownNewTableColParams.table.getTextRange();
        int startOffset = textRange.getStartOffset();
        int endOffset = textRange.getEndOffset();
        UITools.redoableRequest(request, "", event,
                newText -> transformCompletion(markdownNewTableColParams, newText, columnName),
                newText -> replaceString(document, startOffset, endOffset, newText));
    }

    @NotNull
    private static String transformCompletion(@NotNull MarkdownNewTableColParams markdownNewTableColParams, CharSequence complete, CharSequence columnName) {
        complete = "| " + columnName + " | " +  complete;
        CharSequence indent = UITools.getIndent(markdownNewTableColParams.caret);
        @NotNull List<CharSequence> newRows = Arrays.stream(("" + complete).split("\n"))
                .map(String::trim).filter(x -> x.length() > 0).collect(Collectors.toList());
        @NotNull String newTableTxt = StringTools.transposeMarkdownTable(Stream.concat(markdownNewTableColParams.rows.stream(),
                newRows.stream()).collect(Collectors.joining("\n")), false, true);
        return newTableTxt.replace("\n", "\n" + indent);
    }

    public static class MarkdownNewTableColParams {
        public final Caret caret;
        public final PsiElement table;
        public final List<CharSequence> rows;
        public final CharSequence n;

        private MarkdownNewTableColParams(Caret caret, PsiElement table, List<CharSequence> rows, CharSequence n) {
            this.caret = caret;
            this.table = table;
            this.rows = rows;
            this.n = n;
        }

    }
}
