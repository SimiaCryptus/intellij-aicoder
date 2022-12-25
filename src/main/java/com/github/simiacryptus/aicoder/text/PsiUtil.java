package com.github.simiacryptus.aicoder.text;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

public class PsiUtil {
    /**
     * This method gets the largest comment that intersects with the given selection.
     * <p>
     * It takes in an element, a selection start, and a selection end.
     * <p>
     * It then looks through the element and its children to find a comment that is within the selection.
     * <p>
     * If it finds one, it compares it to the other comments it finds and keeps the one with the longest text.
     * <p>
     * Finally, it returns the largest comment it found.
     */
    public static PsiElement getLargestIntersectingComment(PsiElement element, int selectionStart, int selectionEnd) {
        final AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        final AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (null == element) return;
                TextRange textRange = element.getTextRange();
                boolean within = (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() + 1 >= selectionStart && textRange.getStartOffset() <= selectionEnd && textRange.getEndOffset() + 1 >= selectionEnd);
                String simpleName = element.getClass().getSimpleName();
                if (simpleName.equals("PsiCommentImpl") || simpleName.equals("PsiDocCommentImpl")) {
                    if (within) {
                        largest.updateAndGet(s -> (s == null ? 0 : s.getText().length()) > element.getText().length() ? s : element);
                    }
                }
                super.visitElement(element);
                element.acceptChildren(visitor.get());
            }
        });
        element.accept(visitor.get());
        return largest.get();
    }

    public static PsiElement getSmallestIntersectingEntity(PsiElement element, int selectionStart, int selectionEnd) {
        final AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        final AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (null == element) return;
                TextRange textRange = element.getTextRange();
                boolean within = (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() + 1 >= selectionStart && textRange.getStartOffset() <= selectionEnd && textRange.getEndOffset() + 1 >= selectionEnd);
                String simpleName = element.getClass().getSimpleName();
                if (Arrays.asList("PsiMethodImpl", "PsiFieldImpl", "PsiClassImpl").contains(simpleName)) {
                    if (within) {
                        largest.updateAndGet(s -> (s == null ? Integer.MAX_VALUE : s.getText().length()) < element.getText().length() ? s : element);
                    }
                }
                System.out.println(String.format("%s : %s", simpleName, element.getText()));
                super.visitElement(element);
                element.acceptChildren(visitor.get());
            }
        });
        element.accept(visitor.get());
        return largest.get();
    }

    public static String getLargestBlock(PsiElement element, String blockType) {
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
}
