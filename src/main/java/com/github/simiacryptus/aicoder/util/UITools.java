package com.github.simiacryptus.aicoder.util;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.ModerationException;
import com.github.simiacryptus.aicoder.openai.OpenAI_API;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

public class UITools {

    private static final Logger log = Logger.getInstance(UITools.class);

    public static final ConcurrentLinkedDeque<Runnable> retry = new ConcurrentLinkedDeque<>();

    /**
     * This method is responsible for making a redoable request.
     *
     * @param request The completion request to be made.
     * @param indent  The indentation to be used.
     * @param event   The project to be used.
     * @param action  The action to be taken when the request is completed.
     * @return A {@link Runnable} that can be used to redo the request.
     */
    public static void redoableRequest(CompletionRequest request, CharSequence indent, @NotNull AnActionEvent event, Function<CharSequence, Runnable> action) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        Document document = editor.getDocument();
        //document.setReadOnly(true);
        ProgressManager progressManager = ProgressManager.getInstance();
        ProgressIndicator progressIndicator = progressManager.getProgressIndicator();
        if(null != progressIndicator) {
            progressIndicator.setIndeterminate(true);
            progressIndicator.setText("Talking to OpenAI...");
        }
        ListenableFuture<CharSequence> resultFuture = request.complete(event.getProject(), indent);
        Futures.addCallback(resultFuture, new FutureCallback<CharSequence>() {
            @Override
            public void onSuccess(CharSequence result) {
                //document.setReadOnly(false);
                if(null != progressIndicator) {
                    progressIndicator.cancel();
                }
                WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                    retry.add(getRetry(request, indent, event, action, action.apply(result.toString())));
                });
            }

            @Override
            public void onFailure(Throwable t) {
                //document.setReadOnly(false);
                if(null != progressIndicator) {
                    progressIndicator.cancel();
                }
                handle(t);
            }
        }, OpenAI_API.INSTANCE.pool);
    }

    /**
     * Get a retry for the given {@link CompletionRequest}.
     *
     * <p>This method will create a {@link Runnable} that will attempt to complete the given {@link CompletionRequest}
     * with the given {@code indent}. If the completion is successful, the given {@code action} will be applied to the
     * result and the given {@code undo} will be run.
     *
     * @param request the {@link CompletionRequest} to complete
     * @param indent  the indent to use for the completion
     * @param event   the {@link Project} to use for the completion
     * @param action  the {@link Function} to apply to the result of the completion
     * @param undo    the {@link Runnable} to run if the completion is successful
     * @return a {@link Runnable} that will attempt to complete the given {@link CompletionRequest}
     */
    @NotNull
    private static Runnable getRetry(CompletionRequest request, CharSequence indent, AnActionEvent event, Function<CharSequence, Runnable> action, Runnable undo) {
        Document document = event.getData(CommonDataKeys.EDITOR).getDocument();
        //document.setReadOnly(true);
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if(null != progressIndicator) {
            progressIndicator.setIndeterminate(true);
        }
        return () -> {
            ListenableFuture<CharSequence> retryFuture = request.complete(event.getProject(), indent);
            Futures.addCallback(retryFuture, new FutureCallback<CharSequence>() {
                @Override
                public void onSuccess(CharSequence result) {
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        //document.setReadOnly(false);
                        if(null != progressIndicator) {
                            progressIndicator.cancel();
                        }
                        if (null != undo) undo.run();
                        retry.add(getRetry(request, indent, event, action, action.apply(result.toString())));
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    //document.setReadOnly(false);
                    if(null != progressIndicator) {
                        progressIndicator.cancel();
                    }
                    handle(t);
                }
            }, OpenAI_API.INSTANCE.pool);
        };
    }

    /**
     * Get an instruction with a style
     *
     * @param instruction The instruction to be returned
     * @return A string containing the instruction and the style
     */
    public static String getInstruction(String instruction) {
        CharSequence style = AppSettingsState.getInstance().style;
        if (style.length() == 0) return instruction;
        return String.format("%s (%s)", instruction, style);
    }

    /**
     * Replaces a string in a document with a new string.
     *
     * @param document    The document to replace the string in.
     * @param startOffset The start offset of the string to be replaced.
     * @param endOffset   The end offset of the string to be replaced.
     * @param newText     The new string to replace the old string.
     * @return A Runnable that can be used to undo the replacement.
     */
    public static Runnable replaceString(Document document, int startOffset, int endOffset, CharSequence newText) {
        CharSequence oldText = document.getText(new TextRange(startOffset, endOffset));
        document.replaceString(startOffset, endOffset, newText);
        return () -> {
            if (!document.getText(new TextRange(startOffset, startOffset + newText.length())).equals(newText))
                throw new AssertionError();
            document.replaceString(startOffset, startOffset + newText.length(), oldText);
        };
    }

    /**
     * Inserts a string into a document at a given offset and returns a Runnable to undo the insertion.
     *
     * @param document    The document to insert the string into.
     * @param startOffset The offset at which to insert the string.
     * @param newText     The string to insert.
     * @return A Runnable that can be used to undo the insertion.
     */
    public static Runnable insertString(Document document, int startOffset, CharSequence newText) {
        document.insertString(startOffset, newText);
        return () -> {
            if (!document.getText(new TextRange(startOffset, startOffset + newText.length())).equals(newText))
                throw new AssertionError();
            document.deleteString(startOffset, startOffset + newText.length());
        };
    }

    public static Runnable deleteString(Document document, int startOffset, int endOffset) {
        CharSequence oldText = document.getText(new TextRange(startOffset, endOffset));
        document.deleteString(startOffset, endOffset);
        return () -> {
            document.insertString(startOffset, oldText);
        };
    }

    public static CharSequence getIndent(Caret caret) {
        if (null == caret) return "";
        Document document = caret.getEditor().getDocument();
        return IndentedText.fromString(document.getText().split("\n")[document.getLineNumber(caret.getSelectionStart())]).getIndent();
    }

    public static boolean hasSelection(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        return null != caret && caret.hasSelection();
    }

    public static void handle(@NotNull Throwable ex) {
        if (!(ex instanceof ModerationException)) log.error(ex);
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static CharSequence getIndent(AnActionEvent event) {
        Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence indent;
        if (null == caret) {
            indent = "";
        } else {
            indent = getIndent(caret);
        }
        return indent;
    }

    public static String queryAPIKey() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter OpenAI API Key:");
        JPasswordField pass = new JPasswordField(100);
        panel.add(label);
        panel.add(pass);
        CharSequence[] options = new CharSequence[]{"OK", "Cancel"};
        int option = JOptionPane.showOptionDialog(null, panel, "API Key",
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);
        if (option == 0) {
            char[] password = pass.getPassword();
            return new String(password);
        }
        return null;
    }
}
