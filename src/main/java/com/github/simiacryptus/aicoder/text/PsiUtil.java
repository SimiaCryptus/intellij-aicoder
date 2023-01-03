package com.github.simiacryptus.aicoder.text;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

public class PsiUtil {

    public static PsiElement getLargestIntersectingComment(@NotNull PsiElement element, int selectionStart, int selectionEnd) {
        return getLargestIntersecting(element, selectionStart, selectionEnd, "PsiCommentImpl", "PsiDocCommentImpl");
    }

    /**
     * This method is used to get the largest element that intersects with the given selection range.
     *
     * @param element        The element to search within.
     * @param selectionStart The start of the selection range.
     * @param selectionEnd   The end of the selection range.
     * @param types          The types of elements to search for.
     * @return The largest element that intersects with the given selection range.
     */
    public static PsiElement getLargestIntersecting(@NotNull PsiElement element, int selectionStart, int selectionEnd, String... types) {
        final AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        final AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (null == element) return;
                TextRange textRange = element.getTextRange();
                boolean within = (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() + 1 >= selectionStart && textRange.getStartOffset() <= selectionEnd && textRange.getEndOffset() + 1 >= selectionEnd);
                String simpleName = element.getClass().getSimpleName();
                if (Arrays.asList(types).contains(simpleName)) {
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

    /**
     * This method is used to get the smallest intersecting entity of a given PsiElement.
     *
     * @param element        The PsiElement to search for the smallest intersecting entity.
     * @param selectionStart The start of the selection range.
     * @param selectionEnd   The end of the selection range.
     * @return The smallest intersecting entity of the given PsiElement.
     */
    public static PsiElement getSmallestIntersectingEntity(@NotNull PsiElement element, int selectionStart, int selectionEnd) {
        return getSmallestIntersectingEntity(element, selectionStart, selectionEnd, "PsiMethodImpl", "PsiFieldImpl", "PsiClassImpl");
    }

    /**
     * This method is used to get the smallest intersecting entity from a given PsiElement.
     *
     * @param element        The PsiElement from which the smallest intersecting entity is to be retrieved.
     * @param selectionStart The start of the selection.
     * @param selectionEnd   The end of the selection.
     * @param types          The types of the elements to be retrieved.
     * @return The smallest intersecting entity from the given PsiElement.
     */
    public static PsiElement getSmallestIntersectingEntity(@NotNull PsiElement element, int selectionStart, int selectionEnd, String... types) {
        final AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        final AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (null == element) return;
                TextRange textRange = element.getTextRange();
                boolean within = (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() + 1 >= selectionStart && textRange.getStartOffset() <= selectionEnd && textRange.getEndOffset() + 1 >= selectionEnd);
                String simpleName = element.getClass().getSimpleName();
                if (Arrays.asList(types).contains(simpleName)) {
                    if (within) {
                        largest.updateAndGet(s -> (s == null ? Integer.MAX_VALUE : s.getText().length()) < element.getText().length() ? s : element);
                    }
                }
                //System.out.printf("%s : %s%n", simpleName, element.getText());
                super.visitElement(element);
                element.acceptChildren(visitor.get());
            }
        });
        element.accept(visitor.get());
        return largest.get();
    }

    /**
     * This method is used to get the largest block of a given type from a given PsiElement.
     *
     * @param element   The PsiElement from which the largest block is to be retrieved.
     * @param blockType The type of the block to be retrieved.
     * @return The largest block of the given type from the given PsiElement.
     */
    public static String getLargestBlock(@NotNull PsiElement element, String blockType) {
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

    /**
     * This method returns a {@link HashSet} of {@link String}s containing the simple names of all the {@link PsiElement}s
     * contained within the given {@link PsiElement}.
     *
     * @param element The {@link PsiElement} whose children's simple names are to be retrieved.
     * @return A {@link HashSet} of {@link String}s containing the simple names of all the {@link PsiElement}s contained
     * within the given {@link PsiElement}.
     */
    public static @NotNull HashSet<String> getAllElementNames(@NotNull PsiElement element) {
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
