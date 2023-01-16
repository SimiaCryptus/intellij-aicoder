package com.github.simiacryptus.aicoder.util;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.config.Name;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.EditRequest;
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
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UITools {

    private static final Logger log = Logger.getInstance(UITools.class);

    public static final WeakHashMap<Document, Runnable> retry = new WeakHashMap<>();

    public static void redoableRequest(@NotNull CompletionRequest request, CharSequence indent, @NotNull AnActionEvent event, @NotNull Function<CharSequence, Runnable> action) {
        redoableRequest(request, indent, event, x->x, action);
    }

    /**
     * This method is responsible for making a redoable request.
     *
     * @param request The completion request to be made.
     * @param indent  The indentation to be used.
     * @param event   The project to be used.
     * @param action  The action to be taken when the request is completed.
     * @return A {@link Runnable} that can be used to redo the request.
     */
    public static void redoableRequest(@NotNull CompletionRequest request, CharSequence indent, @NotNull AnActionEvent event, @NotNull Function<CharSequence, CharSequence> transformCompletion, @NotNull Function<CharSequence, Runnable> action) {
        @Nullable Editor editor = event.getData(CommonDataKeys.EDITOR);
        @NotNull Document document = Objects.requireNonNull(editor).getDocument();
        @NotNull ProgressManager progressManager = ProgressManager.getInstance();
        ProgressIndicator progressIndicator = progressManager.getProgressIndicator();
        if(null != progressIndicator) {
            progressIndicator.setIndeterminate(true);
            progressIndicator.setText("Talking to OpenAI...");
        }
        @NotNull ListenableFuture<CharSequence> resultFuture = OpenAI_API.INSTANCE.complete(event.getProject(), request, indent);
        Futures.addCallback(resultFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@NotNull CharSequence result) {
                if (null != progressIndicator) {
                    progressIndicator.cancel();
                }
                WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                    retry.put(document, getRetry(request, indent, event, action, action.apply(transformCompletion.apply(result.toString()))));
                });
            }

            @Override
            public void onFailure(Throwable t) {
                if (null != progressIndicator) {
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
    private static Runnable getRetry(@NotNull CompletionRequest request, CharSequence indent, @NotNull AnActionEvent event, @NotNull Function<CharSequence, Runnable> action, @Nullable Runnable undo) {
        @NotNull Document document = Objects.requireNonNull(event.getData(CommonDataKeys.EDITOR)).getDocument();
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if(null != progressIndicator) {
            progressIndicator.setIndeterminate(true);
        }
        return () -> {
            @NotNull ListenableFuture<CharSequence> retryFuture = OpenAI_API.INSTANCE.complete(event.getProject(), request, indent);
            Futures.addCallback(retryFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(@NotNull CharSequence result) {
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        if (null != progressIndicator) {
                            progressIndicator.cancel();
                        }
                        if (null != undo) undo.run();
                        retry.put(document, getRetry(request, indent, event, action, action.apply(result.toString())));
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    if (null != progressIndicator) {
                        progressIndicator.cancel();
                    }
                    handle(t);
                }
            }, OpenAI_API.INSTANCE.pool);
        };
    }

    public static void redoableRequest(@NotNull EditRequest request, CharSequence indent, @NotNull AnActionEvent event, @NotNull Function<CharSequence, Runnable> action) {
        redoableRequest(request, indent, event, x->x, action);
    }

    public static void redoableRequest(@NotNull EditRequest request, CharSequence indent, @NotNull AnActionEvent event, @NotNull Function<CharSequence, CharSequence> transformCompletion, @NotNull Function<CharSequence, Runnable> action) {
        @Nullable Editor editor = event.getData(CommonDataKeys.EDITOR);
        @NotNull Document document = Objects.requireNonNull(editor).getDocument();
        @NotNull ProgressManager progressManager = ProgressManager.getInstance();
        ProgressIndicator progressIndicator = progressManager.getProgressIndicator();
        if(null != progressIndicator) {
            progressIndicator.setIndeterminate(true);
            progressIndicator.setText("Talking to OpenAI...");
        }
        @NotNull ListenableFuture<CharSequence> resultFuture = OpenAI_API.INSTANCE.edit(event.getProject(), request.uiIntercept(), indent);
        Futures.addCallback(resultFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@NotNull CharSequence result) {
                if (null != progressIndicator) {
                    progressIndicator.cancel();
                }
                WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                    retry.put(document, getRetry(request, indent, event, action, action.apply(transformCompletion.apply(result.toString()))));
                });
            }

            @Override
            public void onFailure(Throwable t) {
                if (null != progressIndicator) {
                    progressIndicator.cancel();
                }
                handle(t);
            }
        }, OpenAI_API.INSTANCE.pool);
    }

    @NotNull
    private static Runnable getRetry(@NotNull EditRequest request, CharSequence indent, @NotNull AnActionEvent event, @NotNull Function<CharSequence, Runnable> action, @Nullable Runnable undo) {
        @NotNull Document document = Objects.requireNonNull(event.getData(CommonDataKeys.EDITOR)).getDocument();
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if(null != progressIndicator) {
            progressIndicator.setIndeterminate(true);
        }
        return () -> {
            @NotNull ListenableFuture<CharSequence> retryFuture = OpenAI_API.INSTANCE.edit(event.getProject(), request.uiIntercept(), indent);
            Futures.addCallback(retryFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(@NotNull CharSequence result) {
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        if (null != progressIndicator) {
                            progressIndicator.cancel();
                        }
                        if (null != undo) undo.run();
                        retry.put(document, getRetry(request, indent, event, action, action.apply(result.toString())));
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    if (null != progressIndicator) {
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
        @NotNull CharSequence style = AppSettingsState.getInstance().style;
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
    public static @NotNull Runnable replaceString(@NotNull Document document, int startOffset, int endOffset, @NotNull CharSequence newText) {
        @NotNull CharSequence oldText = document.getText(new TextRange(startOffset, endOffset));
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
    public static @NotNull Runnable insertString(@NotNull Document document, int startOffset, @NotNull CharSequence newText) {
        document.insertString(startOffset, newText);
        return () -> {
            if (!document.getText(new TextRange(startOffset, startOffset + newText.length())).equals(newText))
                throw new AssertionError();
            document.deleteString(startOffset, startOffset + newText.length());
        };
    }

    @SuppressWarnings("unused")
    public static @NotNull Runnable deleteString(@NotNull Document document, int startOffset, int endOffset) {
        @NotNull CharSequence oldText = document.getText(new TextRange(startOffset, endOffset));
        document.deleteString(startOffset, endOffset);
        return () -> {
            document.insertString(startOffset, oldText);
        };
    }

    public static CharSequence getIndent(@Nullable Caret caret) {
        if (null == caret) return "";
        @NotNull Document document = caret.getEditor().getDocument();
        @NotNull String documentText = document.getText();
        int lineNumber = document.getLineNumber(caret.getSelectionStart());
        String @NotNull [] lines = documentText.split("\n");
        return IndentedText.fromString(lines[Math.min(Math.max(lineNumber, 0), lines.length-1)]).getIndent();
    }

    @SuppressWarnings("unused")
    public static boolean hasSelection(@NotNull AnActionEvent e) {
        @Nullable Caret caret = e.getData(CommonDataKeys.CARET);
        return null != caret && caret.hasSelection();
    }

    public static void handle(@NotNull Throwable ex) {
        if (!(ex instanceof ModerationException)) log.error(ex);
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static CharSequence getIndent(@NotNull AnActionEvent event) {
        @Nullable Caret caret = event.getData(CommonDataKeys.CARET);
        CharSequence indent;
        if (null == caret) {
            indent = "";
        } else {
            indent = getIndent(caret);
        }
        return indent;
    }

    public static @Nullable String queryAPIKey() {
        @NotNull JPanel panel = new JPanel();
        @NotNull JLabel label = new JLabel("Enter OpenAI API Key:");
        @NotNull JPasswordField pass = new JPasswordField(100);
        panel.add(label);
        panel.add(pass);
        Object @NotNull [] options = {"OK", "Cancel"};
        if (JOptionPane.showOptionDialog(
                null,
                panel,
                "API Key",
                JOptionPane.NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[1]) == JOptionPane.OK_OPTION) {
            char[] password = pass.getPassword();
            return new String(password);
        } else {
            return null;
        }
    }

    public static <T> void readUI(@NotNull Object component, @NotNull T settings) {
        Class<?> componentClass = component.getClass();
        @NotNull Set<String> declaredUIFields = Arrays.stream(componentClass.getFields()).map(Field::getName).collect(Collectors.toSet());
        for (@NotNull Field settingsField : settings.getClass().getFields()) {
            settingsField.setAccessible(true);
            @NotNull String settingsFieldName = settingsField.getName();
            try {
                @Nullable Object newSettingsValue = null;
                if(!declaredUIFields.contains(settingsFieldName)) continue;
                @NotNull Field uiField = componentClass.getDeclaredField(settingsFieldName);
                Object uiFieldVal = uiField.get(component);
                switch (settingsField.getType().getName()) {
                    case "java.lang.String":
                        if (uiFieldVal instanceof JTextComponent) {
                            newSettingsValue = ((JTextComponent) uiFieldVal).getText();
                        } else if (uiFieldVal instanceof ComboBox) {
                            newSettingsValue = ((ComboBox<CharSequence>) uiFieldVal).getItem();
                        }
                        break;
                    case "int":
                        if (uiFieldVal instanceof JTextComponent) {
                            newSettingsValue = Integer.parseInt(((JTextComponent) uiFieldVal).getText());
                        }
                        break;
                    case "long":
                        if (uiFieldVal instanceof JTextComponent) {
                            newSettingsValue = Long.parseLong(((JTextComponent) uiFieldVal).getText());
                        }
                        break;
                    case "double":
                        if (uiFieldVal instanceof JTextComponent) {
                            newSettingsValue = Double.parseDouble(((JTextComponent) uiFieldVal).getText());
                        }
                        break;
                    case "boolean":
                        if (uiFieldVal instanceof JCheckBox) {
                            newSettingsValue = ((JCheckBox) uiFieldVal).isSelected();
                        } else if (uiFieldVal instanceof JTextComponent) {
                            newSettingsValue = Boolean.parseBoolean(((JTextComponent) uiFieldVal).getText());
                        }
                        break;
                    default:

                        if (Enum.class.isAssignableFrom(settingsField.getType())) {
                            if (uiFieldVal instanceof ComboBox) {
                                @NotNull ComboBox<CharSequence> comboBox = (ComboBox<CharSequence>) uiFieldVal;
                                CharSequence item = comboBox.getItem();
                                newSettingsValue = Enum.valueOf((Class<? extends Enum>) settingsField.getType(), item.toString());
                            }
                        }
                        break;
                }
                settingsField.set(settings, newSettingsValue);
            } catch (Throwable e) {
                new RuntimeException("Error processing " + settingsField, e).printStackTrace();
            }
        }
    }

    public static <T> void writeUI(@NotNull Object component, @NotNull T settings) {
        Class<?> componentClass = component.getClass();
        @NotNull Set<String> declaredUIFields = Arrays.stream(componentClass.getFields()).map(Field::getName).collect(Collectors.toSet());
        for (@NotNull Field settingsField : settings.getClass().getFields()) {
            @NotNull String fieldName = settingsField.getName();
            try {
                if(!declaredUIFields.contains(fieldName)) continue;
                @NotNull Field uiField = componentClass.getDeclaredField(fieldName);
                Object settingsVal = settingsField.get(settings);
                if(null == settingsVal) continue;
                Object uiVal = uiField.get(component);
                switch (settingsField.getType().getName()) {
                    case "java.lang.String":
                        if (uiVal instanceof JTextComponent) {
                            ((JTextComponent) uiVal).setText((String) settingsVal);
                        } else if (uiVal instanceof ComboBox) {
                            ((ComboBox<CharSequence>) uiVal).setItem(settingsVal.toString());
                        }
                        break;
                    case "int":
                    case "java.lang.Integer":
                        if (uiVal instanceof JTextComponent) {
                            ((JTextComponent) uiVal).setText(Integer.toString((Integer) settingsVal));
                        }
                        break;
                    case "long":
                        if (uiVal instanceof JTextComponent) {
                            ((JTextComponent) uiVal).setText(Long.toString((Integer) settingsVal));
                        }
                        break;
                    case "boolean":
                        if (uiVal instanceof JCheckBox) {
                            ((JCheckBox) uiVal).setSelected(((Boolean) settingsVal));
                        } else if (uiVal instanceof JTextComponent) {
                            ((JTextComponent) uiVal).setText(Boolean.toString((Boolean) settingsVal));
                        }
                        break;
                    case "double":
                    case "java.lang.Double":
                        if (uiVal instanceof JTextComponent) {
                            ((JTextComponent) uiVal).setText(Double.toString(((Double) settingsVal)));
                        }
                        break;
                    default:
                        if (uiVal instanceof ComboBox) {
                            ((ComboBox<CharSequence>) uiVal).setItem(settingsVal.toString());
                        }
                        break;
                }
            } catch (Throwable e) {
                new RuntimeException("Error processing " + settingsField, e).printStackTrace();
            }
        }
    }

    public static <T> void addFields(@NotNull Object ui, @NotNull FormBuilder formBuilder) {
        for (@NotNull Field field : ui.getClass().getFields()) {
            if(Modifier.isStatic(field.getModifiers())) continue;
            try {
                Name nameAnnotation = field.getDeclaredAnnotation(Name.class);
                JComponent component = (JComponent) field.get(ui);
                if (null == component) continue;
                if (nameAnnotation != null) {
                    formBuilder.addLabeledComponent(new JBLabel(nameAnnotation.value() + ": "), component, 1, false);
                } else {
                    formBuilder.addComponentToRightColumn(component, 1);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (Throwable e) {
                log.warn("Error processing " + field.getName(), e);
            }
        }
    }

}
