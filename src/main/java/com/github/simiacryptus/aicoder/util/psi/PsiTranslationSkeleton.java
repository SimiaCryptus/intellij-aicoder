package com.github.simiacryptus.aicoder.util.psi;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.OpenAI_API;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.lang.Language;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PsiTranslationSkeleton  {
    public final StringBuffer prefix;
    public final StringBuffer suffix = new StringBuffer();
    public final ArrayList<PsiTranslationSkeleton> children = new ArrayList<>();

    public ListenableFuture<CharSequence> translateFuture = null;
    private final PsiElement element;
    private final String stubId;

    public PsiTranslationSkeleton(String text, @NotNull PsiElement element, @Nullable String stubId) {
        prefix = new StringBuffer(text);
        this.element = element;
        this.stubId = stubId;
    }

    public CharSequence injectStubTranslation(CharSequence translatedOuter, CharSequence translatedInner) {
        if(stubId==null) return translatedOuter;
        // TODO: Correctly implement this and support multiple languages
        return translatedOuter.toString().replace(stubId, translatedInner);
    }

    private static PsiFile parseFile(Project project, CharSequence text, Language language) {
        final AtomicReference<PsiFile> fileFromText = new AtomicReference<>();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);
            PsiFile file = psiFileFactory.createFileFromText(language, text);
            PsiUtil.printTree(file);
            fileFromText.set(file);
        });
        return fileFromText.get();
    }

    @NotNull
    public static PsiTranslationSkeleton parseFile(@NotNull PsiFile psiFile, ComputerLanguage sourceLanguage, ComputerLanguage targetLanguage) {
        PsiTranslationSkeleton psiTranslationSkeleton = new PsiTranslationSkeleton("", psiFile, null);
        new Parser(sourceLanguage, targetLanguage, psiTranslationSkeleton).build(psiFile);
        return psiTranslationSkeleton;
    }

    public ListenableFuture<CharSequence> translate(Project project, CharSequence indent, ComputerLanguage sourceLanguage, ComputerLanguage targetLanguage) {
        if (null == translateFuture) {
            this.translateFuture = OpenAI_API.INSTANCE.complete(project, AppSettingsState.getInstance()
                    .createTranslationRequest()
                    .setInstruction(String.format("Translate %s into %s", sourceLanguage.name(), targetLanguage.name()))
                    .setInputType("source")
                    .setInputText(translationText())
                    .setInputAttribute("language", sourceLanguage.name())
                    .setOutputType("translated")
                    .setOutputAttrute("language", targetLanguage.name())
                    .buildCompletionRequest(), indent);
        }
        return this.translateFuture;
    }

    @NotNull
    private String translationText() {
        if(null != stubId) {
            return element.getText();
        } else {
            return toString();
        }
    }

    public ListenableFuture<?> fullTranslate(Project project, CharSequence indent, ComputerLanguage sourceLanguage, ComputerLanguage targetLanguage) {
        if (!getStubs().isEmpty()) {
            return Futures.allAsList(Streams.concat(
                    Stream.of(translate(project, indent, sourceLanguage, targetLanguage)),
                    getStubs().stream().map(stub -> stub.fullTranslate(project, indent, sourceLanguage, targetLanguage))
            ).collect(Collectors.toList()));
        } else {
            return translate(project, indent, sourceLanguage, targetLanguage);
        }
    }

    public CharSequence getTranslatedDocument() {
        try {
            CharSequence translated = translateFuture.get();
            for (PsiTranslationSkeleton child : getStubs()) {
                translated = child.injectStubTranslation(translated, child.getTranslatedDocument());
            }
            return translated;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Parser extends PsiVisitorBase {

        private final ComputerLanguage sourceLanguage;
        private final ComputerLanguage targetLanguage;
        private PsiTranslationSkeleton currentContext;
        private String indent = "";

        private Parser(ComputerLanguage sourceLanguage, ComputerLanguage targetLanguage, PsiTranslationSkeleton currentContext) {
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.currentContext = currentContext;
        }
        @Override
        protected void visit(@NotNull PsiElement element, PsiElementVisitor self) {
            final String text = element.getText();
            if (PsiUtil.matchesType(element, "Class")) {
                @NotNull PsiTranslationSkeleton newNode = new PsiTranslationSkeleton(indent + text.substring(0, text.indexOf('{')).trim() + " {", element, null);
                currentContext.children.add(newNode);
                processChildren(element, self, newNode);
                newNode.suffix.append("}");
            } else if (PsiUtil.matchesType(element, "Method")) {
                String declaration = PsiUtil.getDeclaration(element).trim();
                String asComment = indent + sourceLanguage.lineComment.fromString(declaration);
                String stubID = "STUB: " + UUID.randomUUID().toString().substring(0, 8);
                String stubbed = String.format("%s {\n%s\n}\n", declaration, targetLanguage.lineComment.fromString(stubID));
                @NotNull PsiTranslationSkeleton newNode = new PsiTranslationSkeleton(stubbed, element, stubID);
                currentContext.children.add(newNode);
                processChildren(element, self, newNode);
            } else if (PsiUtil.matchesType(element, "ImportList", "Field")) {
                @NotNull PsiTranslationSkeleton newNode = new PsiTranslationSkeleton(element.getText().trim(), element, null);
                currentContext.children.add(newNode);
                processChildren(element, self, newNode);
            } else {
                element.acceptChildren(self);
            }
        }

        private PsiTranslationSkeleton processChildren(@NotNull PsiElement element, PsiElementVisitor self, PsiTranslationSkeleton newNode) {
            @NotNull PsiTranslationSkeleton prevclassBuffer = currentContext;
            currentContext = newNode;
            String prevIndent = indent;
            indent += "  ";
            element.acceptChildren(self);
            currentContext = prevclassBuffer;
            indent = prevIndent;
            return newNode;
        }

    }

    public List<PsiTranslationSkeleton> getStubs() {
        return Stream.concat(
                children.stream().filter(x -> x.stubId != null),
                children.stream().filter(x -> x.stubId == null).flatMap(x -> x.getStubs().stream())
        ).collect(Collectors.toList());
    }

    @Override
    public @NotNull String toString() {
        final @NotNull ArrayList<String> sb = new ArrayList<>();
        sb.add(prefix.toString());
        if(stubId==null) children.stream().map(PsiTranslationSkeleton::toString).forEach(sb::add);
        sb.add(suffix.toString());
        return sb.stream().reduce((l, r) -> l + "\n" + r).get();
    }
}
