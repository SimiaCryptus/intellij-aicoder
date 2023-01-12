package com.github.simiacryptus.aicoder.psi;

import com.github.simiacryptus.aicoder.util.StringTools;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
    public static PsiElement getLargestIntersecting(@NotNull PsiElement element, int selectionStart, int selectionEnd, CharSequence... types) {
        final AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        final AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (null == element) return;
                TextRange textRange = element.getTextRange();
                boolean within = (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() + 1 >= selectionStart && textRange.getStartOffset() <= selectionEnd && textRange.getEndOffset() + 1 >= selectionEnd);
                CharSequence simpleName = element.getClass().getSimpleName();
                if (Arrays.asList(expand(types)).contains(simpleName)) {
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
    public static List<PsiElement> getAll(@NotNull PsiElement element, CharSequence... types) {
        final List<PsiElement> elements = new ArrayList<>();
        final AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (null == element) return;
                if (Arrays.asList(expand(types)).contains(element.getClass().getSimpleName())) {
                    elements.add(element);
                } else {
                    element.acceptChildren(visitor.get());
                }
                super.visitElement(element);
            }
        });
        element.accept(visitor.get());
        return elements;
    }

    /**
     * This method is used to get the smallest intersecting entity of a given PsiElement.
     *
     * @param element        The PsiElement to search for the smallest intersecting entity.
     * @param selectionStart The start of the selection range.
     * @param selectionEnd   The end of the selection range.
     * @return The smallest intersecting entity of the given PsiElement.
     */
    public static PsiElement getSmallestIntersecting(@NotNull PsiElement element, int selectionStart, int selectionEnd) {
        return getSmallestIntersecting(element, selectionStart, selectionEnd, "PsiMethodImpl", "PsiFieldImpl", "PsiClassImpl");
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
    public static PsiElement getSmallestIntersecting(@NotNull PsiElement element, int selectionStart, int selectionEnd, CharSequence... types) {
        final AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        final AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (null == element) return;
                TextRange textRange = element.getTextRange();
                boolean within = (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() + 1 >= selectionStart && textRange.getStartOffset() <= selectionEnd && textRange.getEndOffset() + 1 >= selectionEnd);
                CharSequence simpleName = element.getClass().getSimpleName();
                if (Arrays.asList(expand(types)).contains(simpleName)) {
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

    private static CharSequence[] expand(CharSequence[] types) {
        return Arrays.stream(types).flatMap(x-> Stream.of(x, StringTools.stripSuffix(x, "Impl"))).distinct().toArray(CharSequence[]::new);
    }

    public static PsiElement getFirstBlock(@NotNull PsiElement element, CharSequence blockType) {
        PsiElement[] children = element.getChildren();
        if(null == children || 0 == children.length) return null;
        PsiElement first = children[0];
        if(first.getClass().getSimpleName().equals(blockType)) return first;
        return null;
    }

    public static PsiElement getLargestBlock(@NotNull PsiElement element, CharSequence blockType) {
        AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                CharSequence simpleName = element.getClass().getSimpleName();
                if (simpleName.equals(blockType)) {
                    largest.updateAndGet(s -> s != null && s.getText().length() > element.getText().length() ? s : element);
                    super.visitElement(element);
                } else {
                    super.visitElement(element);
                }
                element.acceptChildren(visitor.get());
            }
        });
        element.accept(visitor.get());
        PsiElement psiElement = largest.get();
        return psiElement;
    }

    /**
     * This method returns a {@link HashSet} of {@link String}s containing the simple names of all the {@link PsiElement}s
     * contained within the given {@link PsiElement}.
     *
     * @param element The {@link PsiElement} whose children's simple names are to be retrieved.
     * @return A {@link HashSet} of {@link String}s containing the simple names of all the {@link PsiElement}s contained
     * within the given {@link PsiElement}.
     */
    public static @NotNull HashSet<CharSequence> getAllElementNames(@NotNull PsiElement element) {
        HashSet<CharSequence> set = new HashSet<>();
        AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                CharSequence simpleName = element.getClass().getSimpleName();
                set.add(simpleName);
                element.acceptChildren(visitor.get());
            }
        });
        element.accept(visitor.get());
        return set;
    }

    public static @NotNull String printTree(@NotNull PsiElement element) {
        StringBuilder builder = new StringBuilder();
        printTree(element, builder, 0);
        return builder.toString();
    }

    private static void printTree(@NotNull PsiElement element, @NotNull StringBuilder builder, int level) {
        for (int i = 0; i < level; i++) {
            builder.append("  ");
        }
        builder.append(element.getClass().getSimpleName() + "    " + element.getText().replaceAll("\n", "\\\\n"));
        builder.append("\n");
        for (PsiElement child : element.getChildren()) {
            printTree(child, builder, level + 1);
        }
    }

    public static PsiElement getLargestContainedEntity(PsiElement element, int selectionStart, int selectionEnd) {
        if(null == element) return element;
        TextRange textRange = element.getTextRange();
        if(textRange.getStartOffset() >= selectionStart && textRange.getEndOffset() <= selectionEnd) return element;
        PsiElement largestContainedChild = null;
        for (PsiElement child : element.getChildren()) {
            PsiElement entity = getLargestContainedEntity(child, selectionStart, selectionEnd);
            if(null != entity) {
                if (largestContainedChild == null || largestContainedChild.getTextRange().getLength() < entity.getTextRange().getLength()) {
                    largestContainedChild = entity;
                }
            }
        }
        return largestContainedChild;
    }

}
