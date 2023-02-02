package com.github.simiacryptus.aicoder.util.psi;

import com.github.simiacryptus.aicoder.util.StringTools;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PsiUtil {

    public static final CharSequence[] ELEMENTS_CODE = {
            "Method",
            "Field",
            "Class",
            "Function",
            "CssBlock"
    };
    public static final CharSequence[] ELEMENTS_COMMENTS = {
            "Comment"
    };

    public static PsiElement getLargestIntersectingComment(@NotNull PsiElement element, int selectionStart, int selectionEnd) {
        return getLargestIntersecting(element, selectionStart, selectionEnd, ELEMENTS_COMMENTS);
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
    public static PsiElement getLargestIntersecting(@NotNull PsiElement element, int selectionStart, int selectionEnd, CharSequence @NotNull ... types) {
        final @NotNull AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        final @NotNull AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                TextRange textRange = element.getTextRange();
                boolean within = (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() + 1 >= selectionStart && textRange.getStartOffset() <= selectionEnd && textRange.getEndOffset() + 1 >= selectionEnd);
                if (matchesType(element, types)) {
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

    public static @NotNull List<PsiElement> getAll(@NotNull PsiElement element, CharSequence @NotNull ... types) {
        final @NotNull List<PsiElement> elements = new ArrayList<>();
        final @NotNull AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (matchesType(element, types)) {
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
     * This method is used to get the smallest intersecting entity from a given PsiElement.
     *
     * @param element        The PsiElement from which the smallest intersecting entity is to be retrieved.
     * @param selectionStart The start of the selection.
     * @param selectionEnd   The end of the selection.
     * @param types          The types of the elements to be retrieved.
     * @return The smallest intersecting entity from the given PsiElement.
     */
    public static PsiElement getSmallestIntersecting(@NotNull PsiElement element, int selectionStart, int selectionEnd, CharSequence @NotNull ... types) {
        final @NotNull AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        final @NotNull AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                TextRange textRange = element.getTextRange();
                boolean within = (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() + 1 >= selectionStart && textRange.getStartOffset() <= selectionEnd && textRange.getEndOffset() + 1 >= selectionEnd);
                if (matchesType(element, types)) {
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

    public static boolean matchesType(@NotNull PsiElement element, CharSequence @NotNull ... types) {
        @NotNull CharSequence simpleName = element.getClass().getSimpleName();
        simpleName = StringTools.stripSuffix(simpleName, "Impl");
        simpleName = StringTools.stripPrefix(simpleName, "Psi");
        @NotNull String str = simpleName.toString();
        return Stream.of(types)
                .map(s->StringTools.stripSuffix(s, "Impl"))
                .map(s->StringTools.stripPrefix(s, "Psi"))
                .anyMatch(t -> str.endsWith(t.toString()));
    }

    public static @Nullable PsiElement getFirstBlock(@NotNull PsiElement element, CharSequence... blockType) {
        PsiElement @NotNull [] children = element.getChildren();
        if (0 == children.length) return null;
        PsiElement first = children[0];
        if (matchesType(first, blockType)) return first;
        return null;
    }

    public static PsiElement getLargestBlock(@NotNull PsiElement element, CharSequence... blockType) {
        @NotNull AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        @NotNull AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (matchesType(element, blockType)) {
                    largest.updateAndGet(s -> s != null && s.getText().length() > element.getText().length() ? s : element);
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

    public static @NotNull String printTree(@NotNull PsiElement element) {
        @NotNull StringBuilder builder = new StringBuilder();
        printTree(element, builder, 0);
        return builder.toString();
    }

    private static void printTree(@NotNull PsiElement element, @NotNull StringBuilder builder, int level) {
        builder.append("  ".repeat(Math.max(0, level)));
        Class<? extends @NotNull PsiElement> elementClass = element.getClass();
        @NotNull String simpleName = getName(elementClass);
        builder.append(simpleName).append("    ").append(element.getText().replaceAll("\n", "\\\\n"));
        builder.append("\n");
        for (@NotNull PsiElement child : element.getChildren()) {
            printTree(child, builder, level + 1);
        }
    }

    @NotNull
    private static String getName(@NotNull Class<?> elementClass) {
        @NotNull StringBuilder stringBuilder = new StringBuilder();
        @NotNull Set<String> interfaces = getInterfaces(elementClass);
        while (elementClass != Object.class) {
            if (stringBuilder.length() > 0) stringBuilder.append("/");
            stringBuilder.append(elementClass.getSimpleName());
            elementClass = elementClass.getSuperclass();
        }
        stringBuilder.append("[ ");
        stringBuilder.append(interfaces.stream().sorted().collect(Collectors.joining(",")));
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    @NotNull
    private static Set<String> getInterfaces(@NotNull Class<?> elementClass) {
        @NotNull HashSet<String> strings = Arrays.stream(elementClass.getInterfaces()).map(Class::getSimpleName).collect(Collectors.toCollection(HashSet::new));
        if (elementClass.getSuperclass() != Object.class) strings.addAll(getInterfaces(elementClass.getSuperclass()));
        return strings;
    }

    public static PsiElement getLargestContainedEntity(@Nullable PsiElement element, int selectionStart, int selectionEnd) {
        if (null == element) return null;
        TextRange textRange = element.getTextRange();
        if (textRange.getStartOffset() >= selectionStart && textRange.getEndOffset() <= selectionEnd) return element;
        @Nullable PsiElement largestContainedChild = null;
        for (PsiElement child : element.getChildren()) {
            PsiElement entity = getLargestContainedEntity(child, selectionStart, selectionEnd);
            if (null != entity) {
                if (largestContainedChild == null || largestContainedChild.getTextRange().getLength() < entity.getTextRange().getLength()) {
                    largestContainedChild = entity;
                }
            }
        }
        return largestContainedChild;
    }

    @Nullable
    public static PsiElement getPsiFile(@NotNull AnActionEvent e) {
        @Nullable Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        @Nullable PsiElement psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        PsiElement largestContainedEntity = getLargestContainedEntity(psiFile, selectionStart, selectionEnd);
        if (largestContainedEntity != null) psiFile = largestContainedEntity;
        return psiFile;
    }

    public static PsiElement getSmallestIntersectingMajorCodeElement(@NotNull PsiFile psiFile, @NotNull Caret caret) {
        return getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), ELEMENTS_CODE);
    }

    public static String getDeclaration(@NotNull PsiElement element) {
        String declaration = element.getText();
        @Nullable PsiElement docComment = getLargestBlock(element, "DocComment");
        if (null == docComment) docComment = getFirstBlock(element, "Comment");
        if (null != docComment)
            declaration = StringTools.stripPrefix(declaration.trim(), docComment.getText().trim());
        PsiElement codeBlock = getLargestBlock(element, "CodeBlock", "BlockExpr", "BlockExpression", "StatementList");
        if (null != codeBlock)
            declaration = StringTools.stripSuffix(declaration.trim(), codeBlock.getText().trim());
        return declaration.trim();
    }

    public static PsiFile parseFile(Project project, Language language, CharSequence text) {
        final AtomicReference<PsiFile> fileFromText = new AtomicReference<>();
        WriteCommandAction.runWriteCommandAction(project, () -> fileFromText.set(PsiFileFactory.getInstance(project).createFileFromText(language, text)));
        return fileFromText.get();
    }
}
