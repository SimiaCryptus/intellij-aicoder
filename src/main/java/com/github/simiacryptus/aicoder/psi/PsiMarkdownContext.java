package com.github.simiacryptus.aicoder.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class PsiMarkdownContext {
    public final String text;
    public final ArrayList<PsiMarkdownContext> children = new ArrayList<>();
    private final int start;
    private final PsiMarkdownContext parent;

    public PsiMarkdownContext(PsiMarkdownContext parent, String text, int start) {
        this.start = start;
        this.text = text;
        this.parent = parent;
    }

    @NotNull
    public static PsiMarkdownContext getContext(@NotNull PsiFile psiFile, int selectionStart, int selectionEnd) {
        return new PsiMarkdownContext(null, "", 0).init(psiFile, selectionStart, selectionEnd);
    }

    public int getEnd() {
        return Math.max(start + text.length(), children.stream().mapToInt(x -> x.getEnd()).max().orElse(0));
    }

    public int headerLevel() {
        return (int) text.chars().takeWhile(i -> i == (int) '#').count();
    }

    public @NotNull PsiMarkdownContext init(@NotNull PsiFile psiFile, int selectionStart, int selectionEnd) {
        AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            final String indent = "";
            @NotNull PsiMarkdownContext section = PsiMarkdownContext.this;

            @Override
            public void visitElement(@NotNull PsiElement element) {
                String text = element.getText();
                TextRange textRange = element.getTextRange();
                int textRangeEndOffset = textRange.getEndOffset() + 1;
                int textRangeStartOffset = textRange.getStartOffset();
                // Check if the element comes before the selection
                boolean isPrior = textRangeEndOffset < selectionStart;
                // Check if the element overlaps with the selection
                boolean isOverlap = (textRangeStartOffset >= selectionStart && textRangeStartOffset <= selectionEnd) || (textRangeEndOffset >= selectionStart && textRangeEndOffset <= selectionEnd) ||
                        (textRangeStartOffset <= selectionStart && textRangeEndOffset >= selectionStart) || (textRangeStartOffset <= selectionEnd && textRangeEndOffset >= selectionEnd);
                // Check if the element is within the selection
                boolean within = (textRangeStartOffset <= selectionStart && textRangeEndOffset > selectionStart) && (textRangeStartOffset <= selectionEnd && textRangeEndOffset > selectionEnd);
                if (!isPrior && !isOverlap) return;
                String simpleName = element.getClass().getSimpleName();
                if (simpleName.equals("MarkdownHeaderImpl")) {
                    PsiMarkdownContext content = new PsiMarkdownContext(section, text.trim(), element.getTextOffset());
                    while (content.headerLevel() <= section.headerLevel() && section.parent != null) {
                        section = section.parent;
                    }
                    section.children.add(content);
                    section = content;
                } else if (simpleName.equals("MarkdownParagraphImpl") || simpleName.equals("MarkdownTableImpl")) {
                    section.children.add(new PsiMarkdownContext(section, indent + text.trim(), element.getTextOffset()));
                } else if (simpleName.equals("MarkdownListImpl") || simpleName.equals("MarkdownListItemImpl")) {
                    if (isPrior) {
                        section.children.add(new PsiMarkdownContext(section, indent + text.trim(), element.getTextOffset()));
                    } else {
                        element.acceptChildren(visitor.get());
                    }
                } else {
                    element.acceptChildren(visitor.get());
                }
                super.visitElement(element);
            }
        });
        psiFile.accept(visitor.get());
        return this;
    }

    public @NotNull String toString(int toPoint) {
        final ArrayList<String> sb = new ArrayList<>();
        sb.add(text);
        if (getEnd() >= toPoint) {
            children.stream().filter(c -> c.headerLevel() != 0 || (c.getEnd() < toPoint)).map(psiMarkdownContext -> psiMarkdownContext.toString(toPoint)).forEach(sb::add);
        }
        return sb.stream().reduce((l, r) -> l + "\n" + r).get();
    }
}
