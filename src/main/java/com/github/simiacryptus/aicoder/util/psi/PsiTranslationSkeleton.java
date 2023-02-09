package com.github.simiacryptus.aicoder.util.psi;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.OpenAI_API;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PsiTranslationSkeleton {
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

    public CharSequence injectStubTranslation(CharSequence translatedOuter, CharSequence translatedInner, ComputerLanguage targetLanguage) {
        if (stubId == null) return translatedOuter;
        String regex;
        switch (targetLanguage) {
            case Python:
                regex =
                /*
                 This is a Java regular expression that looks for a pattern of a line of text followed by a colon and then another line
                 of text. The (?s) indicates that the expression should span multiple lines, and the (?<=\n|^) indicates that the pattern
                 should start at the beginning of a line or the beginning of the string. The [^\n]*? indicates that any number of
                 characters that are not a new line should be matched, followed by a colon and then another line of characters that are
                 not a new line.
                */
                "(?s)(?<=\n|^)[^\n]*?:\n[^\n]*?" + stubId + "\\s*?pass\\s*?";
                break;
            case Rust:
            case Kotlin:
            case Scala:
            case Java:
            case JavaScript:
                regex = "(?s)(?<=\n|^)[^\n]*?\\{[^{}]*?" + stubId + ".*?\\}";
                break;
            default:
                regex = "(?s)(?<=\n|^)[^\n]*?\\{[^{}]*?" + stubId + ".*?\\}";
                regex = Pattern.compile(regex).matcher(translatedOuter).find() ? regex : stubId;
                break;
        }
        String replaced = translatedOuter.toString().replaceAll(regex, translatedInner.toString());
        return replaced;
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
        if (null != stubId) {
            return element.getText();
        } else {
            return toString();
        }
    }

    public ListenableFuture<?> fullTranslate(Project project, CharSequence indent, ComputerLanguage sourceLanguage, ComputerLanguage targetLanguage) {
        if (!getStubs().isEmpty()) {
            return Futures.allAsList(Streams.concat(
                    Stream.of(translate(project, indent, sourceLanguage, targetLanguage)),
                    getStubs().stream().map(stub -> stub.fullTranslate(project, indent + "  ", sourceLanguage, targetLanguage))
            ).collect(Collectors.toList()));
        } else {
            return translate(project, indent, sourceLanguage, targetLanguage);
        }
    }

    public CharSequence getTranslatedDocument(ComputerLanguage targetLanguage) {
        try {
            CharSequence translated = translateFuture.get();
            for (PsiTranslationSkeleton child : getStubs()) {
                translated = child.injectStubTranslation(translated, child.getTranslatedDocument(targetLanguage), targetLanguage);
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
            if (PsiUtil.matchesType(element, "Class", "ImplItem")) {
                @NotNull PsiTranslationSkeleton newNode = new PsiTranslationSkeleton(getClassDefPrefix(text), element, null);
                currentContext.children.add(newNode);
                processChildren(element, self, newNode);
                newNode.suffix.append("}");
            } else if (PsiUtil.matchesType(element, "Method", "FunctionDefinition", "Function", "StructItem", "Struct")) {
                String declaration = PsiUtil.getDeclaration(element).trim();
                String stubID = "STUB: " + UUID.randomUUID().toString().substring(0, 8);
                // TODO: This needs to support arbitrary languages
                @NotNull PsiTranslationSkeleton newNode = new PsiTranslationSkeleton(stubMethodText(declaration, stubID), element, stubID);
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

        private String stubMethodText(String declaration, String stubID) {
            switch (sourceLanguage) {
                case Python:
                    return String.format("%s\n    %s\n    pass", declaration, targetLanguage.lineComment.fromString(stubID));
                case Go:
                case Kotlin:
                case Scala:
                case Java:
                case JavaScript:
                case Rust:
                default:
                    return String.format("%s {\n%s\n}\n", declaration, targetLanguage.lineComment.fromString(stubID));
            }
        }

        @NotNull
        private String getClassDefPrefix(String text) {
            switch (sourceLanguage) {
                case Python:
                    return indent + text.substring(0, text.indexOf(':')).trim() + ":";
                case Go:
                case Kotlin:
                case Scala:
                case Java:
                case JavaScript:
                case Rust:
                default:
                    return indent + text.substring(0, text.indexOf('{')).trim() + " {";
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
        if (stubId == null) children.stream().map(PsiTranslationSkeleton::toString).forEach(sb::add);
        sb.add(suffix.toString());
        return sb.stream().reduce((l, r) -> l + "\n" + r).get();
    }
}
