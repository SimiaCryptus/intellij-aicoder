package com.github.simiacryptus.aicoder;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.ModerationException;
import com.github.simiacryptus.aicoder.text.IndentedText;
import com.github.simiacryptus.aicoder.text.PsiClassContext;
import com.github.simiacryptus.aicoder.text.PsiMarkdownContext;
import com.github.simiacryptus.aicoder.text.PsiUtil;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class AICoderMainMenu extends ActionGroup {
    private static final Logger log = Logger.getInstance(AICoderMainMenu.class);

    public static void handle(@NotNull Throwable ex) {
        if (!(ex instanceof ModerationException)) log.error(ex);
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean hasSelection(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        return null != caret && caret.hasSelection();
    }

    /**
     * This method is used to get the children of the action.
     * <p>
     * This Java code is an override of the getChildren() method. It is used to create an array of AnAction objects that will be used to create a context menu for a file.
     * The code first gets the extension of the file and sets the computer language and comment line prefix based on the extension.
     * It then checks if the copy/paste manager has data flavors available and calls the pasteAction() method if it does.
     * It then checks the extension and calls the docAction() method if it is either Java or Scala.
     * It then checks if there is a selection and calls the customTranslation(), autoImplementationAction(), describeAction(), and standardCodeActions() methods if there is.
     * Finally, it returns an array of AnAction objects.
     *
     * @param e AnActionEvent object that contains the necessary data to perform the action.
     * @return An array of AnAction objects that are the children of the action.
     */
    @Override
    public AnAction @NotNull [] getChildren(@NotNull AnActionEvent e) {
        AppSettingsState settings = AppSettingsState.getInstance();
        String inputHumanLanguage = settings.humanLanguage;
        String outputHumanLanguage = settings.humanLanguage;
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return new AnAction[]{};
        String extension = file.getExtension() != null ? file.getExtension().toLowerCase() : "";
        ArrayList<AnAction> children = new ArrayList<>();
        ComputerLanguage language = ComputerLanguage.findByExtension(extension);

        if (language != null) {
            if (settings.devActions) {
                addIfNotNull(children, printTreeAction(e));
            }

            if (CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
                children.add(pasteAction(language.name()));
            }

            if (!language.documentationStyle.isEmpty()) children.add(docAction(extension, language));

            if (hasSelection(e)) {
                children.add(customEdit(language.name()));
                children.add(recentEdits(language.name()));
                switch (language) {
                    case Markdown:
                        addIfNotNull(children, markdownContextAction(e, inputHumanLanguage));
                        break;
                    default:
                        addIfNotNull(children, psiClassContextAction(e, language, inputHumanLanguage));
                        break;
                }
                children.add(describeAction(outputHumanLanguage, language));
                children.add(addCodeCommentsAction(outputHumanLanguage, language));
                children.add(fromHumanLanguageAction(inputHumanLanguage, language));
                children.add(toHumanLanguageAction(outputHumanLanguage, language));
            }
        }

        return children.toArray(AnAction[]::new);
    }

    @NotNull
    private static TextReplacementAction toHumanLanguageAction(String outputHumanLanguage, ComputerLanguage language) {
        String computerLanguage = language.name();
        return TextReplacementAction.create("_To " + outputHumanLanguage, String.format("Describe %s -> %s", outputHumanLanguage, computerLanguage), null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            return settings.createTranslationRequest()
                    .setInstruction(getInstruction(settings.style, "Describe this code"))
                    .setInputText(string)
                    .setInputType(computerLanguage)
                    .setInputAttribute("type", "input")
                    .setOutputType(outputHumanLanguage.toLowerCase())
                    .setOutputAttrute("type", "output")
                    .setOutputAttrute("style", settings.style)
                    .buildCompletionRequest()
                    .complete(getIndent(event.getData(CommonDataKeys.CARET)));
        });
    }

    @NotNull
    private static TextReplacementAction fromHumanLanguageAction(String inputHumanLanguage, ComputerLanguage language) {
        String computerLanguage = language.name();
        return TextReplacementAction.create("_From " + inputHumanLanguage, String.format("Implement %s -> %s", inputHumanLanguage, computerLanguage), null, (event, string) -> {
            return AppSettingsState.getInstance().createTranslationRequest()
                    .setInputType(inputHumanLanguage.toLowerCase())
                    .setOutputType(computerLanguage)
                    .setInstruction("Implement this specification")
                    .setInputAttribute("type", "input")
                    .setOutputAttrute("type", "output")
                    .setInputText(string)
                    .buildCompletionRequest()
                    .complete(getIndent(event.getData(CommonDataKeys.CARET)));
        });
    }

    @NotNull
    private static TextReplacementAction addCodeCommentsAction(String outputHumanLanguage, ComputerLanguage language) {
        String computerLanguage = language.name();
        return TextReplacementAction.create("Add Code _Comments", "Add Code Comments", null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            return settings.createTranslationRequest()
                    .setInputType(computerLanguage)
                    .setOutputType(computerLanguage)
                    .setInstruction(getInstruction(settings.style, "Rewrite to include detailed " + outputHumanLanguage + " code comments for every line"))
                    .setInputAttribute("type", "uncommented")
                    .setOutputAttrute("type", "commented")
                    .setOutputAttrute("style", settings.style)
                    .setInputText(string)
                    .buildCompletionRequest()
                    .complete(getIndent(event.getData(CommonDataKeys.CARET)));
        });
    }

    @NotNull
    private static TextReplacementAction describeAction(String outputHumanLanguage, ComputerLanguage language) {
        return TextReplacementAction.create("_Describe Code and Prepend Comment", "Add JavaDoc Comments", null, (event, inputString) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            String description = settings.createTranslationRequest()
                    .setInputType(language.name())
                    .setOutputType(outputHumanLanguage)
                    .setInstruction(getInstruction(settings.style, "Explain this " + language.name() + " in " + outputHumanLanguage))
                    .setInputAttribute("type", "code")
                    .setOutputAttrute("type", "description")
                    .setOutputAttrute("style", settings.style)
                    .setInputText(IndentedText.fromString(inputString).textBlock.trim())
                    .buildCompletionRequest()
                    .complete(indent);
            String linePrefix = indent + language.singlelineCommentPrefix + " ";
            description = linePrefix + IndentedText.fromString(StringTools.lineWrapping(description.trim())).withIndent(linePrefix);
            return "\n" + description + "\n" + indent + inputString;
        });
    }

    private static String getInstruction(String style, String instruction) {
        if (style.isEmpty()) return instruction;
        return String.format("%s (%s)", instruction, style);
    }

    @NotNull
    private static TextReplacementAction customEdit(String computerLanguage) {
        return TextReplacementAction.create("_Edit...", "Edit...", null, (event, string) -> {
            String instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE);
            AppSettingsState settings = AppSettingsState.getInstance();
            settings.addInstructionToHistory(instruction);
            return settings.createTranslationRequest()
                    .setInputType(computerLanguage)
                    .setOutputType(computerLanguage)
                    .setInstruction(instruction)
                    .setInputAttribute("type", "before")
                    .setOutputAttrute("type", "after")
                    .setInputText(IndentedText.fromString(string).textBlock)
                    .buildCompletionRequest()
                    .complete(getIndent(event.getData(CommonDataKeys.CARET)));
        });
    }

    @NotNull
    private static ActionGroup recentEdits(String computerLanguage) {
        return new ActionGroup("Recent Edits", true) {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                ArrayList<AnAction> children = new ArrayList<>();
                AppSettingsState.getInstance().getEditHistory().forEach(instruction -> children.add(customEdit(computerLanguage, instruction)));
                return children.toArray(AnAction[]::new);
            }
        };
    }

    @NotNull
    private static AnAction docAction(String extension, ComputerLanguage language) {
        return new AnAction("_Add " + language.documentationStyle + " Comments", "Add " + language.documentationStyle + " Comments", null) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent event) {
                try {
                    Caret caret = event.getData(CommonDataKeys.CARET);
                    PsiFile psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE);
                    PsiElement smallestIntersectingMethod = PsiUtil.getSmallestIntersectingEntity(psiFile, caret.getSelectionStart(), caret.getSelectionEnd());
                    if (null == smallestIntersectingMethod) return;
                    AppSettingsState settings = AppSettingsState.getInstance();
                    String code = smallestIntersectingMethod.getText();
                    IndentedText indentedInput = IndentedText.fromString(code);
                    String rawDocString = settings.createTranslationRequest()
                            .setInputType(extension)
                            .setOutputType(extension)
                            .setInstruction(getInstruction(settings.style, "Rewrite to include detailed " + language.documentationStyle))
                            .setInputAttribute("type", "uncommented")
                            .setOutputAttrute("type", "commented")
                            .setOutputAttrute("style", settings.style)
                            .setInputText(indentedInput.textBlock)
                            .buildCompletionRequest()
                            .addStops(new String[]{language.multilineCommentSuffix})
                            .complete("");
                    String replace = language.multilineCommentPrefix + "\n" + StringTools.stripSuffix(StringTools.stripPrefix(rawDocString.trim(), language.multilineCommentPrefix).trim(), language.multilineCommentSuffix).trim() + "\n" + language.multilineCommentSuffix;
                    final String newText = IndentedText.fromString(replace).withIndent(indentedInput.indent) + "\n" + indentedInput.indent + indentedInput.textBlock;
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                        editor.getDocument().replaceString(smallestIntersectingMethod.getTextRange().getStartOffset(), smallestIntersectingMethod.getTextRange().getEndOffset(), newText);
                    });
                } catch (ModerationException | IOException ex) {
                    handle(ex);
                }
            }
        };
    }

    @NotNull
    private static TextReplacementAction pasteAction(@NotNull String language) {
        return TextReplacementAction.create("_Paste", "Paste", null, (event, string) -> {
            String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor).toString().trim();
            return AppSettingsState.getInstance().createTranslationRequest()
                    .setInputType("source")
                    .setOutputType("translated")
                    .setInstruction("Translate this input into " + language)
                    .setInputAttribute("language", "autodetect")
                    .setOutputAttrute("language", language)
                    .setInputText(text)
                    .buildCompletionRequest()
                    .complete(getIndent(event.getData(CommonDataKeys.CARET)));
        });
    }

    private static String getIndent(Caret caret) {
        if (null == caret) return "";
        Document document = caret.getEditor().getDocument();
        return IndentedText.fromString(document.getText().split("\n")[document.getLineNumber(caret.getSelectionStart())]).indent;
    }

    @NotNull
    private static TextReplacementAction customEdit(String computerLanguage, String instruction) {
        return TextReplacementAction.create(instruction, instruction, null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            settings.addInstructionToHistory(instruction);
            return settings.createTranslationRequest()
                    .setInputType(computerLanguage)
                    .setOutputType(computerLanguage)
                    .setInstruction(instruction)
                    .setInputAttribute("type", "before")
                    .setOutputAttrute("type", "after")
                    .setInputText(IndentedText.fromString(string).textBlock)
                    .buildCompletionRequest()
                    .complete(getIndent(event.getData(CommonDataKeys.CARET)));
        });
    }

    @Nullable
    private static TextReplacementAction markdownContextAction(@NotNull AnActionEvent e, String humanLanguage) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null != caret) {
            int selectionStart = caret.getSelectionStart();
            int selectionEnd = caret.getSelectionEnd();
            if (selectionStart < selectionEnd) {
                return TextReplacementAction.create("E_xecute Directive", "Execute Directive", null, (event, humanDescription) -> {
                    AppSettingsState settings = AppSettingsState.getInstance();
                    PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);
                    String context = PsiMarkdownContext.getContext(psiFile, selectionStart, selectionEnd).toString(selectionEnd);
                    context = context + "\n<!-- " + humanDescription + "-->\n";
                    context = context + "\n";
                    return settings.createTranslationRequest()
                            .setOutputType("markdown")
                            .setInstruction(getInstruction(settings.style, String.format("Using Markdown and %s", humanLanguage)))
                            .setInputType("instruction")
                            .setInputText(humanDescription)
                            .setOutputAttrute("type", "document")
                            .setOutputAttrute("style", settings.style)
                            .buildCompletionRequest()
                            //.addStops(new String[]{"#"})
                            .appendPrompt(context)
                            .complete(getIndent(caret));
                });
            }
        }
        return null;
    }

    private static void addIfNotNull(@NotNull ArrayList<AnAction> children, AnAction action) {
        if (null != action) children.add(action);
    }

    @Nullable
    private static AnAction printTreeAction(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        PsiElement psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        PsiElement largestContainedEntity = PsiUtil.getLargestContainedEntity(psiFile, selectionStart, selectionEnd);
        if (largestContainedEntity != null) psiFile = largestContainedEntity;
        PsiElement finalPsiFile = psiFile;
        return new AnAction("Print PSI Tree", "Print PSI Tree", null) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent e1) {
                log.warn(PsiUtil.printTree(finalPsiFile));
            }

        };
    }

    @Nullable
    private static AnAction psiClassContextAction(@NotNull AnActionEvent e, ComputerLanguage computerLanguage, String humanLanguage) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        PsiElement largestIntersectingComment = PsiUtil.getLargestIntersectingComment(psiFile, selectionStart, selectionEnd);
        if (largestIntersectingComment == null) return null;
        return new AnAction("Insert _Implementation", "Insert Implementation", null) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent e1) {
                final Editor editor = e1.getRequiredData(CommonDataKeys.EDITOR);
                final CaretModel caretModel = editor.getCaretModel();
                final Caret primaryCaret = caretModel.getPrimaryCaret();
                @NotNull String selectedText = primaryCaret.getSelectedText();
                AppSettingsState settings = AppSettingsState.getInstance();
                try {
                    String specification = Arrays.stream((selectedText.split(" ").length > 4 ? selectedText : largestIntersectingComment.getText()).trim().split("\n"))
                            .map(x->StringTools.stripPrefix(x, computerLanguage.singlelineCommentPrefix))
                            .map(x->x.trim())
                            .filter(x->!x.isEmpty())
                            .reduce((a,b)->a+" "+b).get();
                    String result = settings.createTranslationRequest()
                            .setInstruction("Implement " + humanLanguage + " as " + computerLanguage.name() + " code")
                            .setInputType(humanLanguage)
                            .setInputAttribute("type", "instruction")
                            .setInputText(specification)
                            .setOutputType(computerLanguage.name())
                            .setOutputAttrute("type", "code")
                            .setOutputAttrute("style", settings.style)
                            .buildCompletionRequest()
                            .appendPrompt(PsiClassContext.getContext(psiFile, selectionStart, selectionEnd) + "\n")
                            .complete(getIndent(caret));
                    WriteCommandAction.runWriteCommandAction(e1.getProject(), () -> {
                        editor.getDocument().insertString(largestIntersectingComment.getTextRange().getEndOffset(), "\n" + result);
                    });
                } catch (ModerationException | IOException ex) {
                    handle(ex);
                }
            }

        };
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
        super.update(e);
    }
}