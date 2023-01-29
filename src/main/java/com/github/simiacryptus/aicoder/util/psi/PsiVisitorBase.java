package com.github.simiacryptus.aicoder.util.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public abstract class PsiVisitorBase {

    public PsiVisitorBase() {
    }

    public void build(@NotNull PsiFile psiFile) {
        @NotNull AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                visit(element, visitor.get());
                super.visitElement(element);
            }
        });
        psiFile.accept(visitor.get());
    }

    protected abstract void visit(@NotNull PsiElement element, PsiElementVisitor self);
}
