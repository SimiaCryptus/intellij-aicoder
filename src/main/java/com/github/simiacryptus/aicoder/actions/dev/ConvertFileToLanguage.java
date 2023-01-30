package com.github.simiacryptus.aicoder.actions.dev;

import com.github.simiacryptus.aicoder.openai.OpenAI_API;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.UITools;
import com.github.simiacryptus.aicoder.util.psi.PsiTranslationSkeleton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ConvertFileToLanguage extends AnAction {
    private static final Logger log = Logger.getInstance(ConvertFileToLanguage.class);

    private final ComputerLanguage targetLanguage;

    public ConvertFileToLanguage(ComputerLanguage language) {
        super(language.name());
        this.targetLanguage = language;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        log.warn(Language.getRegisteredLanguages().stream().map(language -> Arrays.asList(
                language.getID(),
                language.getDisplayName(),
                Optional.ofNullable(language.getAssociatedFileType()).map(x->x.getDefaultExtension()).orElse(""),
                Optional.ofNullable(language.getBaseLanguage()).map(x->x.getID()).orElse(""),
                Optional.ofNullable(language.getDialects()).map(x->x.stream().map(y->y.getID()).collect(Collectors.joining(","))).orElse("")
        ).stream().collect(Collectors.joining(","))).collect(Collectors.joining("\n")));
        ComputerLanguage sourceLanguage = ComputerLanguage.getComputerLanguage(event);
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence indent = UITools.getIndent(caret);
        PsiTranslationSkeleton skeleton = PsiTranslationSkeleton.parseFile(event.getRequiredData(CommonDataKeys.PSI_FILE), sourceLanguage, targetLanguage);
        translate(event, sourceLanguage, indent, skeleton);
    }

    private void translate(@NotNull AnActionEvent event, ComputerLanguage sourceLanguage, CharSequence indent, PsiTranslationSkeleton root) {
        ProgressIndicator progressIndicator = UITools.startProgress();
        Futures.addCallback(root.fullTranslate(event.getProject(), indent, sourceLanguage, targetLanguage), new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object newText) {
                String content = root.getTranslatedDocument().toString();
                if (null != progressIndicator) {
                    progressIndicator.cancel();
                }
                write(event.getProject(), getNewFile(event.getProject(), event.getRequiredData(CommonDataKeys.VIRTUAL_FILE), targetLanguage), content);
            }
            @Override public void onFailure(Throwable e) {
                if (null != progressIndicator) {
                    progressIndicator.cancel();
                }
                log.error("Error translating file", e);
            };
        }, OpenAI_API.INSTANCE.pool);
    }

    public static VirtualFile getNewFile(Project project, VirtualFile file, ComputerLanguage language) {
        AtomicReference<VirtualFile> newFileRef = new AtomicReference<>();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                newFileRef.set(file.getParent().createChildData(file, file.getNameWithoutExtension() + "." + language.extensions.get(0)));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        return newFileRef.get();
    }

    public static void write(Project project, VirtualFile newFile, String content) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                newFile.setBinaryContent(content.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
