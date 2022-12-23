package com.github.simiacryptus.aicoder;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

public class ImplementationContext {
    public final boolean isPrior;
    public final boolean isOverlap;
    public final String text;
    public final ArrayList<ImplementationContext> children = new ArrayList<>();

    public ImplementationContext(String text, boolean isPrior, boolean isOverlap) {
        this.isPrior = isPrior;
        this.isOverlap = isOverlap;
        this.text = text;
    }

    @NotNull
    public static ImplementationContext getContext(PsiFile psiFile, int selectionStart, int selectionEnd) {
        ImplementationContext root = new ImplementationContext("", false, true);
        //HashSet<String> elementNames = getAllElementNames(psiFile);
        AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            String indent = "";
            ImplementationContext classBuffer = root;

            @Override
            public void visitElement(@NotNull PsiElement element) {
                String text = element.getText();
                TextRange textRange = element.getTextRange();
                int textRangeEndOffset = textRange.getEndOffset()+1;
                int textRangeStartOffset = textRange.getStartOffset();
                boolean isPrior = textRangeEndOffset < selectionStart;
                boolean isOverlap = (textRangeStartOffset >= selectionStart && textRangeStartOffset <= selectionEnd) || (textRangeEndOffset >= selectionStart && textRangeEndOffset <= selectionEnd) ||
                        (textRangeStartOffset <= selectionStart && textRangeEndOffset >= selectionStart) || (textRangeStartOffset <= selectionEnd && textRangeEndOffset >= selectionEnd);
                boolean within = (textRangeStartOffset <= selectionStart && textRangeEndOffset > selectionStart) && (textRangeStartOffset <= selectionEnd && textRangeEndOffset > selectionEnd);
                String simpleName = element.getClass().getSimpleName();
                String block = getLargestCodeBlock(element, "PsiCodeBlockImpl");
                if (simpleName.equals("PsiImportListImpl")) {
                    classBuffer.children.add(new ImplementationContext(text.trim(), isPrior, isOverlap));
                } else if (simpleName.equals("PsiCommentImpl") || simpleName.equals("PsiDocCommentImpl")) {
                    if (within) {
                        classBuffer.children.add(new ImplementationContext(indent + text.trim(), isPrior, isOverlap));
                    }
                } else if (simpleName.equals("PsiMethodImpl") || simpleName.equals("PsiFieldImpl")) {
                    String docComment = getLargestCodeBlock(element, "PsiDocCommentImpl");
                    String declaration = text;
                    if (declaration.startsWith(docComment))
                        declaration = declaration.substring(docComment.length());
                    if (declaration.endsWith(block))
                        declaration = declaration.substring(0, declaration.length() - block.length());
                    classBuffer.children.add(new ImplementationContext(indent + declaration.trim() + (isOverlap ? " {" : ";"), isPrior, isOverlap));
                    String prevIndent = indent;
                    indent = indent + "  ";
                    element.acceptChildren(visitor.get());
                    indent = prevIndent;
                } else if (simpleName.equals("PsiClassImpl")) {
                    String declarationText = indent + text.substring(0, text.indexOf('{')).trim() + " {";
                    ImplementationContext newNode = new ImplementationContext(declarationText, isPrior, isOverlap);
                    classBuffer.children.add(newNode);
                    ImplementationContext prevclassBuffer = classBuffer;
                    classBuffer = newNode;
                    String prevIndent = indent;
                    indent = indent + "  ";
                    element.acceptChildren(visitor.get());
                    indent = prevIndent;
                    classBuffer = prevclassBuffer;
                    if (!isOverlap) {
                        classBuffer.children.add(new ImplementationContext("}", isPrior, isOverlap));
                    }
                } else {
                    element.acceptChildren(visitor.get());
                }
                super.visitElement(element);
            }
        });
        psiFile.accept(visitor.get());
        return root;
    }

    public static String getLargestCodeBlock(PsiElement element, String blockType) {
        AtomicReference<String> largest = new AtomicReference<>("");
        AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                String simpleName = element.getClass().getSimpleName();
                if (simpleName.equals(blockType)) {
                    String text = element.getText();
                    largest.updateAndGet(s -> s.length() > text.length() ? s : text);
                    super.visitElement(element);
                } else {
                    super.visitElement(element);
                }
                element.acceptChildren(visitor.get());
            }
        });
        element.accept(visitor.get());
        return largest.get();
    }

    public static HashSet<String> getAllElementNames(PsiElement element) {
        HashSet<String> set = new HashSet<>();
        AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                String simpleName = element.getClass().getSimpleName();
                set.add(simpleName);
                element.acceptChildren(visitor.get());
            }
        });
        element.accept(visitor.get());
        return set;
    }

    @Override
    public String toString() {
        final ArrayList<String> sb = new ArrayList<>();
        sb.add(text);
        children.stream().filter(x -> x.isPrior).map(ImplementationContext::toString).forEach(sb::add);
        children.stream().filter(x -> !x.isOverlap && !x.isPrior).map(ImplementationContext::toString).forEach(sb::add);
        children.stream().filter(x -> x.isOverlap).map(ImplementationContext::toString).forEach(sb::add);
        return sb.stream().reduce((l, r) -> l + "\n" + r).get();
    }
}
