package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.util.StringTools;
import com.github.simiacryptus.aicoder.util.UITools;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.github.simiacryptus.aicoder.util.UITools.insertString;

public class MarkdownNewTableRowsAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        return null != getMarkdownNewTableRowsParams(e);
    }

    @Nullable
    public static MarkdownNewTableRowsParams getMarkdownNewTableRowsParams(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null != caret) {
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if (null != psiFile) {
                PsiElement table = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownTableImpl");
                if (null != table) {
                    return new MarkdownNewTableRowsParams(caret, table);
                }
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        MarkdownNewTableRowsParams markdownNewTableRowsParams = getMarkdownNewTableRowsParams(event);
        List<CharSequence> rows = StringTools.trim(PsiUtil.getAll(Objects.requireNonNull(markdownNewTableRowsParams).table, "MarkdownTableRowImpl")
                .stream().map(PsiElement::getText).collect(Collectors.toList()), 10, true);
        CharSequence n = Integer.toString(rows.size() * 2);
        AppSettingsState settings = AppSettingsState.getInstance();
        int endOffset = markdownNewTableRowsParams.table.getTextRange().getEndOffset();
        Document document = event.getRequiredData(CommonDataKeys.EDITOR).getDocument();
        UITools.redoableRequest(MarkdownNewTableColAction.newRowsRequest(settings, n, rows, ""), "", event,
                newText -> transformCompletion(markdownNewTableRowsParams, newText),
                newText -> insertString(document, endOffset, newText));
    }

    @NotNull
    private static String transformCompletion(MarkdownNewTableRowsParams markdownNewTableRowsParams, CharSequence complete) {
        CharSequence indent = UITools.getIndent(markdownNewTableRowsParams.caret);
        List<CharSequence> newRows = Arrays.stream(("" + complete).split("\n"))
                .map(String::trim).filter(x -> x.length() > 0).collect(Collectors.toList());
        return "\n" + indent + newRows.stream().collect(Collectors.joining("\n" + indent));
    }

    public static class MarkdownNewTableRowsParams {
        public final Caret caret;
        public final PsiElement table;

        private MarkdownNewTableRowsParams(Caret caret, PsiElement table) {
            this.caret = caret;
            this.table = table;
        }

    }
}
