package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.github.simiacryptus.aicoder.util.StringTools;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.simiacryptus.aicoder.util.UITools.replaceString;

public class MarkdownNewTableColsAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        return null != getMarkdownNewTableColsParams(e);
    }

    @Nullable
    public static MarkdownNewTableColsParams getMarkdownNewTableColsParams(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null != caret) {
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if (null != psiFile) {
                PsiElement table = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownTableImpl");
                if (null != table) {
                    List<CharSequence> rows = Arrays.asList(StringTools.transposeMarkdownTable(PsiUtil.getAll(table, "MarkdownTableRowImpl")
                            .stream().map(PsiElement::getText).collect(Collectors.joining("\n")), false, false).split("\n"));
                    CharSequence n = Integer.toString(rows.size() * 2);
                    return new MarkdownNewTableColsParams(caret, table, rows, n);
                }
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        MarkdownNewTableColsParams markdownNewTableColsParams = getMarkdownNewTableColsParams(event);
        AppSettingsState settings = AppSettingsState.getInstance();
        CharSequence indent = UITools.getIndent(Objects.requireNonNull(markdownNewTableColsParams).caret);
        CompletionRequest request = MarkdownNewTableColAction.newRowsRequest(settings, markdownNewTableColsParams.n, markdownNewTableColsParams.rows, "");
        UITools.redoableRequest(request, "", event, newText -> transformCompletion(markdownNewTableColsParams, indent, newText), newText -> replaceString(event.getRequiredData(CommonDataKeys.EDITOR).getDocument(),
                markdownNewTableColsParams.table.getTextRange().getStartOffset(),
                markdownNewTableColsParams.table.getTextRange().getEndOffset(), newText));
    }

    @NotNull
    private static String transformCompletion(MarkdownNewTableColsParams markdownNewTableColsParams, CharSequence indent, CharSequence complete) {
        List<CharSequence> newRows = Arrays.stream(("" + complete).split("\n")).map(String::trim)
                .filter(x -> x.length() > 0).collect(Collectors.toList());
        String newTableTxt = StringTools.transposeMarkdownTable(Stream.concat(markdownNewTableColsParams.rows.stream(), newRows.stream())
                .collect(Collectors.joining("\n")), false, true);
        return newTableTxt.replace("\n", "\n" + indent);
    }

    public static class MarkdownNewTableColsParams {
        public final Caret caret;
        public final PsiElement table;
        public final List<CharSequence> rows;
        public final CharSequence n;

        private MarkdownNewTableColsParams(Caret caret, PsiElement table, List<CharSequence> rows, CharSequence n) {
            this.caret = caret;
            this.table = table;
            this.rows = rows;
            this.n = n;
        }

    }
}
