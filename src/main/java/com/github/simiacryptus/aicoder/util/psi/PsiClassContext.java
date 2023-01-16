package com.github.simiacryptus.aicoder.util.psi;

import com.github.simiacryptus.aicoder.util.StringTools;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

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
        @NotNull AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            String indent = "";
            @NotNull PsiClassContext currentContext = PsiClassContext.this;

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
                @NotNull CharSequence simpleName = element.getClass().getSimpleName();
                if (simpleName.equals("PsiImportListImpl")) {
                    currentContext.children.add(new PsiClassContext(text.trim(), isPrior, isOverlap));
                } else if (simpleName.equals("PsiCommentImpl") || simpleName.equals("PsiDocCommentImpl")) {
                    if (within) {
                        currentContext.children.add(new PsiClassContext(indent + text.trim(), false, true));
                    }
                } else if (simpleName.equals("PsiMethodImpl") || simpleName.equals("PsiFieldImpl")) {
                    String declaration = text;
                    @Nullable PsiElement docComment = PsiUtil.getLargestBlock(element, "PsiDocCommentImpl");
                    if(null == docComment)  docComment = PsiUtil.getFirstBlock(element, "PsiCommentImpl");
                    if(null != docComment) declaration = StringTools.stripPrefix(declaration.trim(), docComment.getText().trim());
                    PsiElement codeBlock = PsiUtil.getLargestBlock(element, "PsiCodeBlockImpl");
                    if(null != codeBlock) declaration = StringTools.stripSuffix(declaration.trim(), codeBlock.getText().trim());
                    @NotNull PsiClassContext newNode = new PsiClassContext(indent + declaration.trim() + (isOverlap ? " {" : ";"), isPrior, isOverlap);
                    currentContext.children.add(newNode);
                    String prevIndent = indent;
                    indent = indent + "  ";
                    @NotNull PsiClassContext prevclassBuffer = currentContext;
                    currentContext = newNode;
                    element.acceptChildren(visitor.get());
                    currentContext = prevclassBuffer;
                    indent = prevIndent;
                } else if (simpleName.equals("PsiLocalVariableImpl")) {
                    currentContext.children.add(new PsiClassContext(indent + text.trim() + ";", isPrior, isOverlap));
                } else if (simpleName.equals("PsiClassImpl")) {
                    @NotNull String declarationText = indent + text.substring(0, text.indexOf('{')).trim() + " {";
                    @NotNull PsiClassContext newNode = new PsiClassContext(declarationText, isPrior, isOverlap);
                    currentContext.children.add(newNode);
                    @NotNull PsiClassContext prevclassBuffer = currentContext;
                    currentContext = newNode;
                    String prevIndent = indent;
                    indent = indent + "  ";
                    element.acceptChildren(visitor.get());
                    indent = prevIndent;
                    currentContext = prevclassBuffer;
                    if (!isOverlap) {
                        currentContext.children.add(new PsiClassContext("}", isPrior, false));
                    }
                } else if (!isOverlap && Arrays.asList("PsiCodeBlockImpl", "PsiForStatementImpl").contains(simpleName)) {
                    // Skip
                } else {
                    element.acceptChildren(visitor.get());
                }
                super.visitElement(element);
            }
        });
        psiFile.accept(visitor.get());
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
