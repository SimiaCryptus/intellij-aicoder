package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.OpenAI_API;
import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.UITools;
import com.github.simiacryptus.aicoder.util.psi.PsiTranslationSkeleton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class ConvertFileToLanguage extends AnAction {
    private static final Logger log = Logger.getInstance(ConvertFileToLanguage.class);

    private final ComputerLanguage targetLanguage;

    public ConvertFileToLanguage(ComputerLanguage language) {
        super(language.name());
        this.targetLanguage = language;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ComputerLanguage sourceLanguage = ComputerLanguage.getComputerLanguage(event);
        CharSequence indent = UITools.INSTANCE.getIndent(event.getData(CommonDataKeys.CARET));
        PsiTranslationSkeleton skeleton = PsiTranslationSkeleton.parseFile(event.getRequiredData(CommonDataKeys.PSI_FILE), sourceLanguage, targetLanguage);
        translate(event, sourceLanguage, indent, skeleton);
    }

    private void translate(@NotNull AnActionEvent event, ComputerLanguage sourceLanguage, CharSequence indent, PsiTranslationSkeleton root) {
        ListenableFuture<?> future;
        if(AppSettingsState.getInstance().apiThreads > 1) {
            future = root.parallelTranslate(event.getProject(), indent, sourceLanguage, targetLanguage);
        } else {
            future = root.sequentialTranslate(event.getProject(), indent, sourceLanguage, targetLanguage);
        }
        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object newText) {
                try {
                    future.get(1, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
                String content = root.getTranslatedDocument(targetLanguage).toString();
                write(event.getProject(), getNewFile(event.getProject(), event.getRequiredData(CommonDataKeys.VIRTUAL_FILE), targetLanguage), content);
            }
            @Override public void onFailure(Throwable e) {
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
