package com.github.simiacryptus.aicoder.util.psi;

import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PsiTranslationSkeleton extends PsiVisitorBase {
    public final StringBuffer prefix;
    public final StringBuffer suffix = new StringBuffer();
    public final ArrayList<PsiTranslationSkeleton> children = new ArrayList<>();
    private final ComputerLanguage sourceLanguage;
    private final ComputerLanguage targetLanguage;

    public PsiTranslationSkeleton(String text, PsiTranslationSkeleton parent, @NotNull PsiElement element) {
        this.prefix = new StringBuffer(text);
        this.sourceLanguage = parent.sourceLanguage;
        this.targetLanguage = parent.targetLanguage;
    }

    public PsiTranslationSkeleton(String text, ComputerLanguage sourceLanguage, ComputerLanguage targetLanguage) {
        this.prefix = new StringBuffer(text);
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    @NotNull
    public static PsiTranslationSkeleton getContext(@NotNull PsiFile psiFile, ComputerLanguage sourceLanguage, ComputerLanguage targetLanguage) {
        return new PsiTranslationSkeleton("", sourceLanguage, targetLanguage).init(psiFile);
    }

    public @NotNull PsiTranslationSkeleton init(@NotNull PsiFile psiFile) {
        build(psiFile);
        return this;
    }

    private PsiTranslationSkeleton currentContext = PsiTranslationSkeleton.this;
    private String indent = "";

    @Override
    protected void visit(@NotNull PsiElement element, PsiElementVisitor self) {
        final String text = element.getText();
        if (PsiUtil.matchesType(element, "ImportList")) {
            this.currentContext.children.add(new PsiTranslationSkeleton(text.trim(), this, element));
        } else if (PsiUtil.matchesType(element, "Class")) {
            PsiTranslationSkeleton classDef = newElement(this.indent + text.substring(0, text.indexOf('{')).trim() + " {", element);
            processChildren(element, self, classDef);
            classDef.suffix.append("}");
        } else if (PsiUtil.matchesType(element, "Method", "Field")) {
            PsiTranslationSkeleton memberNode = newElement(this.indent + sourceLanguage.lineComment.fromString(PsiUtil.getDeclaration(element).trim()), element);
            processChildren(element, self, memberNode);
        } else if (PsiUtil.matchesType(element, "Comment", "DocComment", "CodeBlock", "ForStatement", "LocalVariable")) {
            // Skip
        } else {
            element.acceptChildren(self);
        }
    }

    private PsiTranslationSkeleton processChildren(@NotNull PsiElement element, PsiElementVisitor self, PsiTranslationSkeleton newNode) {
        @NotNull PsiTranslationSkeleton prevclassBuffer = this.currentContext;
        currentContext = newNode;
        String prevIndent = this.indent;
        indent += "  ";
        element.acceptChildren(self);
        currentContext = prevclassBuffer;
        indent = prevIndent;
        return newNode;
    }

    @NotNull
    private PsiTranslationSkeleton newElement(String str, @NotNull PsiElement element) {
        @NotNull PsiTranslationSkeleton newNode = new PsiTranslationSkeleton(str, this, element);
        this.currentContext.children.add(newNode);
        return newNode;
    }

    @Override
    public @NotNull String toString() {
        final @NotNull ArrayList<String> sb = new ArrayList<>();
        sb.add(prefix.toString());
        children.stream().map(PsiTranslationSkeleton::toString).forEach(sb::add);
        sb.add(suffix.toString());
        return sb.stream().reduce((l, r) -> l + "\n" + r).get();
    }
}
