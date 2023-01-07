package com.github.simiacryptus.aicoder;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.ModerationException;
import com.github.simiacryptus.aicoder.openai.OpenAI;
import com.github.simiacryptus.aicoder.openai.StringTools;
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


public class AICoderMainMenu extends ActionGroup {
    private static final Logger log = Logger.getInstance(AICoderMainMenu.class);

    public static void handle(@NotNull Throwable ex) {
        if(!(ex instanceof ModerationException)) log.error(ex);
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
        String inputHumanLanguage = AppSettingsState.getInstance().humanLanguage;
        String outputHumanLanguage = AppSettingsState.getInstance().humanLanguage;
        VirtualFile file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
        String extension = file.getExtension().toLowerCase();
        ComputerLanguage language = ComputerLanguage.findByExtension(extension);
        if (language == null) return new AnAction[]{};

        ArrayList<AnAction> children = new ArrayList<>();

        if (CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
            pasteAction(children, extension);
        }
        docAction(extension, language, children);

        if (hasSelection(e)) {
            customTranslation(language, children);
            switch (language) {
                case Markdown:
                    markdownImplementationAction(e, children, inputHumanLanguage);
                    break;
                default:
                    autoImplementationAction(e, children, extension, inputHumanLanguage);
                    break;
            }
            describeAction(outputHumanLanguage, language, children);
            addCodeCommentsAction(outputHumanLanguage, language, children);
            fromHumanLanguageAction(inputHumanLanguage, language, children);
            toHumanLanguageAction(outputHumanLanguage, language, children);
        }

        return children.toArray(AnAction[]::new);
    }

    private static void toHumanLanguageAction(String outputHumanLanguage, ComputerLanguage language, ArrayList<AnAction> children) {
        String computerLanguage = language.name();
        children.add(TextReplacementAction.create("To " + outputHumanLanguage, String.format("Describe %s -> %s", outputHumanLanguage, computerLanguage), null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            String instruction = "Describe this code";
            if (!settings.style.isEmpty())
                instruction = String.format("%s (%s)", instruction, settings.style);
            CompletionRequest request = settings.createTranslationRequestTemplate()
                    .setInputTag(computerLanguage)
                    .setOutputTag(outputHumanLanguage.toLowerCase())
                    .setInstruction(instruction)
                    .setInputAttr("type", "input")
                    .setOutputAttr("type", "output")
                    .setOutputAttr("style", settings.style)
                    .setOriginalText(string)
                    .buildRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return request.getCompletionText(OpenAI.INSTANCE.complete(request), indent);
        }));
    }

    private static boolean fromHumanLanguageAction(String inputHumanLanguage, ComputerLanguage language, ArrayList<AnAction> children) {
        String computerLanguage = language.name();
        return children.add(TextReplacementAction.create("From " + inputHumanLanguage, String.format("Implement %s -> %s", inputHumanLanguage, computerLanguage), null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            CompletionRequest request = settings.createTranslationRequestTemplate()
                    .setInputTag(inputHumanLanguage.toLowerCase())
                    .setOutputTag(computerLanguage)
                    .setInstruction("Implement this specification")
                    .setInputAttr("type", "input")
                    .setOutputAttr("type", "output")
                    .setOriginalText(string)
                    .buildRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return request.getCompletionText(OpenAI.INSTANCE.complete(request), indent);
        }));
    }

    private static void addCodeCommentsAction(String outputHumanLanguage, ComputerLanguage language, ArrayList<AnAction> children) {
        String computerLanguage = language.name();
        children.add(TextReplacementAction.create("Add Code Comments", "Add Code Comments", null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            String instruction = "Rewrite to include detailed " + outputHumanLanguage + " code comments for every line";
            if (!settings.style.isEmpty())
                instruction = String.format("%s (%s)", instruction, settings.style);
            CompletionRequest request = settings.createTranslationRequestTemplate()
                    .setInputTag(computerLanguage)
                    .setOutputTag(computerLanguage)
                    .setInstruction(instruction)
                    .setInputAttr("type", "uncommented")
                    .setOutputAttr("type", "commented")
                    .setOutputAttr("style", settings.style)
                    .setOriginalText(string)
                    .buildRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return request.getCompletionText(OpenAI.INSTANCE.complete(request), indent);
        }));
    }

    private void describeAction(String outputHumanLanguage, ComputerLanguage language, ArrayList<AnAction> children) {
        children.add(TextReplacementAction.create("Describe Code and Prepend Comment", "Add JavaDoc Comments", null, (event, inputString) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            String style = settings.style;
            String instruction = "Explain this " + language.name() + " in " + outputHumanLanguage;
            if (!style.isEmpty()) instruction = String.format("%s (%s)", instruction, style);
            CompletionRequest request = settings.createTranslationRequestTemplate()
                    .setInputTag(language.name())
                    .setOutputTag(outputHumanLanguage)
                    .setInstruction(instruction)
                    .setInputAttr("type", "code")
                    .setOutputAttr("type", "description")
                    .setOutputAttr("style", style)
                    .setOriginalText(IndentedText.fromString(inputString).textBlock.trim())
                    .buildRequest();
            //String indent = indentedInput.indent;
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            String description = request.getCompletionText(OpenAI.INSTANCE.complete(request), indent);
            String linePrefix = indent + language.singlelineCommentPrefix + " ";
            description = linePrefix + IndentedText.fromString(StringTools.lineWrapping(description.trim())).withIndent(linePrefix);
            return "\n" + description + "\n" + indent + inputString;
        }));
    }

    private void customTranslation(ComputerLanguage language, ArrayList<AnAction> children) {
        String computerLanguage = language.name();
        children.add(TextReplacementAction.create("Edit...", "Edit...", null, (event, string) -> {
            String instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE);
            AppSettingsState settings = AppSettingsState.getInstance();
            settings.addInstructionToHistory(instruction);
            IndentedText indentedInput = IndentedText.fromString(string);
            CompletionRequest request = settings.createTranslationRequestTemplate()
                    .setInputTag(computerLanguage)
                    .setOutputTag(computerLanguage)
                    .setInstruction(instruction)
                    .setInputAttr("type", "before")
                    .setOutputAttr("type", "after")
                    .setOriginalText(indentedInput.textBlock)
                    .buildRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return request.getCompletionText(OpenAI.INSTANCE.complete(request), indent);
        }));
        children.add(new ActionGroup("Recent Edits", true) {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                ArrayList<AnAction> children11 = new ArrayList<>();
                AppSettingsState.getInstance().getEditHistory().forEach(instruction -> quickTranslation(children11, computerLanguage, instruction));
                return children11.toArray(AnAction[]::new);
            }
        });
    }

    private void docAction(String extension, ComputerLanguage language, ArrayList<AnAction> children) {
        if (language.documentationStyle.isEmpty()) return;
        children.add(new AnAction("Add " + language.documentationStyle + " Comments", "Add " + language.documentationStyle + " Comments", null) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent event) {
                try {
                    Caret caret = event.getData(CommonDataKeys.CARET);
                    PsiFile psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE);
                    PsiElement smallestIntersectingMethod = PsiUtil.getSmallestIntersectingEntity(psiFile, caret.getSelectionStart(), caret.getSelectionEnd());
                    if (null == smallestIntersectingMethod) return;
                    AppSettingsState settings = AppSettingsState.getInstance();
                    String instruction = "Rewrite to include detailed " + language.documentationStyle;
                    if (!settings.style.isEmpty())
                        instruction = String.format("%s (%s)", instruction, settings.style);
                    String code = smallestIntersectingMethod.getText();
                    IndentedText indentedInput = IndentedText.fromString(code);
                    CompletionRequest request = settings.createTranslationRequestTemplate()
                            .setInputTag(extension)
                            .setOutputTag(extension)
                            .setInstruction(instruction)
                            .setInputAttr("type", "uncommented")
                            .setOutputAttr("type", "commented")
                            .setOutputAttr("style", settings.style)
                            .setOriginalText(indentedInput.textBlock)
                            .buildRequest()
                            .addStops(new String[]{language.multilineCommentSuffix});
                    String replace = request.getCompletionText(OpenAI.INSTANCE.complete(request), "").toString().replaceAll("\n", "\n*") + "\n*/\n";
                    final String newText = IndentedText.fromString(replace).withIndent(indentedInput.indent) + indentedInput.textBlock;
                    final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        editor.getDocument().replaceString(smallestIntersectingMethod.getTextRange().getStartOffset(), smallestIntersectingMethod.getTextRange().getEndOffset(), newText);
                    });
                } catch (ModerationException | IOException ex) {
                    handle(ex);
                }
            }
        });
    }

    /**
     * Adds a TextReplacementAction that auto-translates content from the clipboard
     *
     * @param children  The ArrayList to add the TextReplacementAction to.
     * @param extension The extension to translate the input into.
     */
    private void pasteAction(@NotNull ArrayList<AnAction> children, @NotNull String extension) {
        // Add a TextReplacementAction to the ArrayList
        children.add(TextReplacementAction.create("Paste", "Paste", null, (event, string) -> {
            String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor).toString().trim();
            CompletionRequest request = AppSettingsState.getInstance().createTranslationRequestTemplate()
                    .setInputTag("source")
                    .setOutputTag("translated")
                    .setInstruction("Translate this input into " + extension)
                    .setInputAttr("language", "autodetect")
                    .setOutputAttr("language", extension)
                    .setOriginalText(text)
                    .buildRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return request.getCompletionText(OpenAI.INSTANCE.complete(request), indent);
        }));
    }

    private static String getIndent(Caret caret) {
        if (null == caret) return "";
        Document document = caret.getEditor().getDocument();
        return IndentedText.fromString(document.getText().split("\n")[document.getLineNumber(caret.getSelectionStart())]).indent;
    }

    /**
     * This method adds a TextReplacementAction to the given ArrayList of AnActions.
     * The TextReplacementAction is created with the given instruction and a lambda expression.
     * The lambda expression is used to translate the given string from one computer language to another.
     *
     * @param children         The ArrayList of AnActions to which the TextReplacementAction is added.
     * @param computerLanguage The language to which the given string is translated.
     * @param instruction      The instruction used to create the TextReplacementAction.
     */
    private void quickTranslation(@NotNull ArrayList<AnAction> children, String computerLanguage, String instruction) {
        children.add(TextReplacementAction.create(instruction, instruction, null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            settings.addInstructionToHistory(instruction);
            IndentedText indentedInput = IndentedText.fromString(string);
            CompletionRequest request = settings.createTranslationRequestTemplate()
                    .setInputTag(computerLanguage)
                    .setOutputTag(computerLanguage)
                    .setInstruction(instruction)
                    .setInputAttr("type", "before")
                    .setOutputAttr("type", "after")
                    .setOriginalText(indentedInput.textBlock)
                    .buildRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return request.getCompletionText(OpenAI.INSTANCE.complete(request), indent);
        }));
    }

    /**
     * This method is used to add an action to the list of available actions for the user to execute.
     * The action is used to implement a given human language action as markdown documentation.
     *
     * @param e             AnActionEvent object containing the context of the action.
     * @param children      ArrayList of AnAction objects containing the list of available actions.
     * @param humanLanguage The human language to be implemented as markdown documentation.
     * @return void
     */
    private void markdownImplementationAction(@NotNull AnActionEvent e, @NotNull ArrayList<AnAction> children, String humanLanguage) {
        String computerLanguage = "markdown";
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return;
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        if (selectionStart < selectionEnd) {
            children.add(TextReplacementAction.create("Execute Directive", "Execute Directive", null, (event, humanDescription) -> {
                String instruction = "Implement " + humanLanguage + " as " + computerLanguage + " code";
                AppSettingsState settings = AppSettingsState.getInstance();
                if (!settings.style.isEmpty())
                    instruction = String.format("%s (%s)", instruction, settings.style);
                PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);
                PsiMarkdownContext root = PsiMarkdownContext.getContext(psiFile, selectionStart, selectionEnd);
                String contextWithDirective = root.toString(selectionEnd) + "\n<!-- " + humanDescription + "-->\n";
                CompletionRequest request = settings.createTranslationRequestTemplate()
                        .setInputTag(humanLanguage)
                        .setOutputTag(computerLanguage)
                        .setInstruction(instruction)
                        .setInputAttr("type", "instruction")
                        .setOutputAttr("type", "document")
                        .setOutputAttr("style", settings.style)
                        .setOriginalText(humanDescription)
                        .buildRequest()
                        .addStops(new String[]{"#"})
                        .appendPrompt(contextWithDirective + "\n");
                String indent = getIndent(caret);
                return request.getCompletionText(OpenAI.INSTANCE.complete(request), indent);
            }));
        }
    }

    /**
     * Automatically implements a given action.
     *
     * @param e                the action event
     * @param children         the list of actions
     * @param computerLanguage the computer language to implement
     * @param humanLanguage    the human language to implement
     */
    private void autoImplementationAction(@NotNull AnActionEvent e, @NotNull ArrayList<AnAction> children, String computerLanguage, String humanLanguage) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return;
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return;
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        PsiElement largestIntersectingComment = PsiUtil.getLargestIntersectingComment(psiFile, selectionStart, selectionEnd);
        if (largestIntersectingComment != null) {
            children.add(new AnAction("Insert Implementation", "Insert Implementation", null) {
                @Override
                public void actionPerformed(@NotNull final AnActionEvent e) {
                    final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
                    final CaretModel caretModel = editor.getCaretModel();
                    final Caret primaryCaret = caretModel.getPrimaryCaret();
                    final String newText = implement(e, primaryCaret.getSelectedText());
                    WriteCommandAction.runWriteCommandAction(e.getProject(), () -> {
                        editor.getDocument().insertString(largestIntersectingComment.getTextRange().getEndOffset(), "\n" + newText);
                    });
                }

                private String implement(@NotNull AnActionEvent event, @NotNull String string) {
                    PsiClassContext root = PsiClassContext.getContext(psiFile, selectionStart, selectionEnd);
                    String instruction = "Implement " + humanLanguage + " as " + computerLanguage + " code";
                    AppSettingsState settings = AppSettingsState.getInstance();
                    if (!settings.style.isEmpty())
                        instruction = String.format("%s (%s)", instruction, settings.style);
                    String implementation = null;
                    try {
                        CompletionRequest request = settings.createTranslationRequestTemplate()
                                .setInputTag(humanLanguage)
                                .setOutputTag(computerLanguage)
                                .setInstruction(instruction)
                                .setInputAttr("type", "instruction")
                                .setOutputAttr("type", "code")
                                .setOutputAttr("style", settings.style)
                                .setOriginalText(string.split(" ").length > 4 ? string : largestIntersectingComment.getText())
                                .buildRequest()
                                .appendPrompt(root + "\n");
                        String indent = getIndent(caret);
                        implementation = request.getCompletionText(OpenAI.INSTANCE.complete(request), indent);
                    } catch (ModerationException | IOException ex) {
                        handle(ex);
                    }
                    return implementation;
                }

            });
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
        super.update(e);
    }
}