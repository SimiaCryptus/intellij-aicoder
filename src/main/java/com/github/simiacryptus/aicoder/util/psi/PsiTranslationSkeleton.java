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
            case Kotlin:
            case Scala:
            case Java:
            case JavaScript:
                regex = "(?s)(?<=\n)[^\n]*?\\{[^{}]*?" + stubId + ".*?\\}";
                break;
            case Python:
                /*
                 This Java code creates a regular expression that looks for a line of text that contains the given stubId followed by the
                 word "pass". The regular expression is preceded by a new line character and the line must not contain any other new line
                 characters.
                */
                regex = "(?s)(?<=\n)[^\n]*?:\n[^\n]*?" + stubId + "\\s*?pass\\s*?";
                break;
            default:
                /*
                 This Java code creates a regular expression that looks for a line of text that contains the given stubId and is
                 surrounded by curly braces. The regular expression is set to be case-insensitive and to span multiple lines.
                */
                regex = "(?s)(?<=\n)[^\n]*?\\{[^{}]*?" + stubId + ".*?\\}";
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
            if (PsiUtil.matchesType(element, "Class")) {
                @NotNull PsiTranslationSkeleton newNode = new PsiTranslationSkeleton(getClassDefPrefix(text), element, null);
                currentContext.children.add(newNode);
                processChildren(element, self, newNode);
                newNode.suffix.append("}");
            } else if (PsiUtil.matchesType(element, "Method", "FunctionDefinition", "Function")) {
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
                case Go:
                case Kotlin:
                case Scala:
                case Java:
                case JavaScript:
                    return String.format("%s {\n%s\n}\n", declaration, targetLanguage.lineComment.fromString(stubID));
                case Python:
                    return String.format("%s\n    %s\n    pass", declaration, targetLanguage.lineComment.fromString(stubID));
                default:
                    return String.format("%s {\n%s\n}\n", declaration, targetLanguage.lineComment.fromString(stubID));
            }
        }

        @NotNull
        private String getClassDefPrefix(String text) {
            switch (sourceLanguage) {
                case Go:
                case Kotlin:
                case Scala:
                case Java:
                case JavaScript:
                    return indent + text.substring(0, text.indexOf('{')).trim() + " {";
                case Python:
                    return indent + text.substring(0, text.indexOf(':')).trim() + ":";
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
