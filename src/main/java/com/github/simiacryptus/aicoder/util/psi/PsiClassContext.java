package com.github.simiacryptus.aicoder.util.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PsiClassContext {
    public final boolean isPrior;
    public final boolean isOverlap;
    public final String text;
    public final ArrayList<PsiClassContext> children = new ArrayList<>();

    public PsiClassContext(String text, boolean isPrior, boolean isOverlap) {
        this.isPrior = isPrior;
        this.isOverlap = isOverlap;
        this.text = text;
    }

    @NotNull
    public static PsiClassContext getContext(@NotNull PsiFile psiFile, int selectionStart, int selectionEnd) {
        return new PsiClassContext("", false, true).init(psiFile, selectionStart, selectionEnd);
    }
    public static PsiClassContext getContext(@NotNull PsiFile psiFile) {
        return getContext(psiFile, 0, psiFile.getTextLength());
    }

    /**
     * This java code is initializing a PsiClassContext object. It is doing this by creating a PsiElementVisitor and using it to traverse the PsiFile.
     * It is checking the text range of each element and whether it is prior to, overlapping, or within the selectionStart and selectionEnd parameters.
     * Depending on the element, it is adding the text of the element to the PsiClassContext object, or recursively visiting its children.
     *
     * @param psiFile
     * @param selectionStart
     * @param selectionEnd
     * @return
     */
    public @NotNull PsiClassContext init(@NotNull PsiFile psiFile, int selectionStart, int selectionEnd) {
        new PsiVisitorBase() {
            PsiClassContext currentContext = PsiClassContext.this;
            String indent = "";
            @Override
            protected void visit(@NotNull PsiElement element, PsiElementVisitor self) {
                final String text = element.getText();
                final TextRange textRange = element.getTextRange();
                final int textRangeEndOffset = textRange.getEndOffset() + 1;
                final int textRangeStartOffset = textRange.getStartOffset();
                // Check if the element comes before the selection
                final boolean isPrior = textRangeEndOffset < selectionStart;
                // Check if the element overlaps with the selection
                final boolean isOverlap = textRangeStartOffset >= selectionStart && textRangeStartOffset <= selectionEnd || textRangeEndOffset >= selectionStart && textRangeEndOffset <= selectionEnd ||
                        textRangeStartOffset <= selectionStart && textRangeEndOffset >= selectionStart || textRangeStartOffset <= selectionEnd && textRangeEndOffset >= selectionEnd;
                // Check if the element is within the selection
                final boolean within = textRangeStartOffset <= selectionStart && textRangeEndOffset > selectionStart && textRangeStartOffset <= selectionEnd && textRangeEndOffset > selectionEnd;
                if (PsiUtil.matchesType(element, "ImportList")) {
                    this.currentContext.children.add(new PsiClassContext(text.trim(), isPrior, isOverlap));
                } else if (PsiUtil.matchesType(element, "Comment", "DocComment")) {
                    if (within) {
                        this.currentContext.children.add(new PsiClassContext(this.indent + text.trim(), false, true));
                    }
                } else if (PsiUtil.matchesType(element, "Method", "Field")) {
                    processChildren(element, self, isPrior, isOverlap, this.indent + PsiUtil.getDeclaration(element).trim() + (isOverlap ? " {" : ";"));
                } else if (PsiUtil.matchesType(element, "LocalVariable")) {
                    this.currentContext.children.add(new PsiClassContext(this.indent + text.trim() + ";", isPrior, isOverlap));
                } else if (PsiUtil.matchesType(element, "Class")) {
                    processChildren(element, self, isPrior, isOverlap, this.indent + text.substring(0, text.indexOf('{')).trim() + " {");
                    if (!isOverlap) {
                        this.currentContext.children.add(new PsiClassContext("}", isPrior, false));
                    }
                } else if (!isOverlap && PsiUtil.matchesType(element, "CodeBlock", "ForStatement")) {
                    // Skip
                } else {
                    element.acceptChildren(self);
                }
            }

            private @NotNull PsiClassContext processChildren(@NotNull PsiElement element, PsiElementVisitor self, boolean isPrior, boolean isOverlap, @NotNull String declarationText) {
                @NotNull PsiClassContext newNode = new PsiClassContext(declarationText, isPrior, isOverlap);
                this.currentContext.children.add(newNode);
                @NotNull PsiClassContext prevclassBuffer = this.currentContext;
                currentContext = newNode;
                String prevIndent = this.indent;
                indent += "  ";
                element.acceptChildren(self);
                indent = prevIndent;
                currentContext = prevclassBuffer;
                return newNode;
            }
        }.build(psiFile);
        return this;
    }

    @Override
    public @NotNull String toString() {
        final @NotNull ArrayList<String> sb = new ArrayList<>();
        sb.add(text);
        children.stream().filter(x -> x.isPrior).map(PsiClassContext::toString).forEach(sb::add);
        children.stream().filter(x -> !x.isOverlap && !x.isPrior).map(PsiClassContext::toString).forEach(sb::add);
        children.stream().filter(x -> x.isOverlap).map(PsiClassContext::toString).forEach(sb::add);
        return sb.stream().reduce((l, r) -> l + "\n" + r).get();
    }
}
