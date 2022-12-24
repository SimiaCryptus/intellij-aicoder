package com.github.simiacryptus.aicoder.text;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class PsiClassContext {
    public final boolean isPrior;
    public final boolean isOverlap;
    public final String text;
    public final ArrayList<PsiClassContext> children = new ArrayList<>();
    boolean verbose = false;

    public PsiClassContext(String text, boolean isPrior, boolean isOverlap) {
        this.isPrior = isPrior;
        this.isOverlap = isOverlap;
        this.text = text;
    }

    @NotNull
    public static PsiClassContext getContext(PsiFile psiFile, int selectionStart, int selectionEnd) {
        return new PsiClassContext("", false, true).init(psiFile, selectionStart, selectionEnd);
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
    public PsiClassContext init(PsiFile psiFile, int selectionStart, int selectionEnd) {
        AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            String indent = "";
            PsiClassContext classBuffer = PsiClassContext.this;

            @Override
            public void visitElement(@NotNull PsiElement element) {
                String text = element.getText();
                TextRange textRange = element.getTextRange();
                int textRangeEndOffset = textRange.getEndOffset() + 1;
                int textRangeStartOffset = textRange.getStartOffset();
                boolean isPrior = textRangeEndOffset < selectionStart;
                boolean isOverlap = (textRangeStartOffset >= selectionStart && textRangeStartOffset <= selectionEnd) || (textRangeEndOffset >= selectionStart && textRangeEndOffset <= selectionEnd) ||
                        (textRangeStartOffset <= selectionStart && textRangeEndOffset >= selectionStart) || (textRangeStartOffset <= selectionEnd && textRangeEndOffset >= selectionEnd);
                boolean within = (textRangeStartOffset <= selectionStart && textRangeEndOffset > selectionStart) && (textRangeStartOffset <= selectionEnd && textRangeEndOffset > selectionEnd);
                String simpleName = element.getClass().getSimpleName();
                if (simpleName.equals("PsiImportListImpl")) {
                    classBuffer.children.add(new PsiClassContext(text.trim(), isPrior, isOverlap));
                } else if (simpleName.equals("PsiCommentImpl") || simpleName.equals("PsiDocCommentImpl")) {
                    if (within) {
                        classBuffer.children.add(new PsiClassContext(indent + text.trim(), isPrior, isOverlap));
                    }
                } else if (simpleName.equals("PsiMethodImpl") || simpleName.equals("PsiFieldImpl")) {
                    String docComment = PsiUtil.getLargestBlock(element, "PsiDocCommentImpl");
                    String declaration = text;
                    if (declaration.startsWith(docComment))
                        declaration = declaration.substring(docComment.length());
                    String block = PsiUtil.getLargestBlock(element, "PsiCodeBlockImpl");
                    if (declaration.endsWith(block))
                        declaration = declaration.substring(0, declaration.length() - block.length());
                    classBuffer.children.add(new PsiClassContext(indent + declaration.trim() + (isOverlap ? " {" : ";"), isPrior, isOverlap));
                    String prevIndent = indent;
                    indent = indent + "  ";
                    element.acceptChildren(visitor.get());
                    indent = prevIndent;
                } else if (simpleName.equals("PsiClassImpl")) {
                    String declarationText = indent + text.substring(0, text.indexOf('{')).trim() + " {";
                    PsiClassContext newNode = new PsiClassContext(declarationText, isPrior, isOverlap);
                    classBuffer.children.add(newNode);
                    PsiClassContext prevclassBuffer = classBuffer;
                    classBuffer = newNode;
                    String prevIndent = indent;
                    indent = indent + "  ";
                    element.acceptChildren(visitor.get());
                    indent = prevIndent;
                    classBuffer = prevclassBuffer;
                    if (!isOverlap) {
                        classBuffer.children.add(new PsiClassContext("}", isPrior, isOverlap));
                    }
                } else {
                    if (verbose) System.out.printf("%s -> %s%n", simpleName, text);
                    element.acceptChildren(visitor.get());
                }
                super.visitElement(element);
            }
        });
        psiFile.accept(visitor.get());
        return this;
    }

    @Override
    public String toString() {
        final ArrayList<String> sb = new ArrayList<>();
        sb.add(text);
        children.stream().filter(x -> x.isPrior).map(PsiClassContext::toString).forEach(sb::add);
        children.stream().filter(x -> !x.isOverlap && !x.isPrior).map(PsiClassContext::toString).forEach(sb::add);
        children.stream().filter(x -> x.isOverlap).map(PsiClassContext::toString).forEach(sb::add);
        return sb.stream().reduce((l, r) -> l + "\n" + r).get();
    }
}
