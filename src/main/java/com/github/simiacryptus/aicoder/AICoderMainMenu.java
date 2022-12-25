package com.github.simiacryptus.aicoder;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.OpenAI;
import com.github.simiacryptus.aicoder.text.IndentedText;
import com.github.simiacryptus.aicoder.text.PsiClassContext;
import com.github.simiacryptus.aicoder.text.PsiMarkdownContext;
import com.github.simiacryptus.aicoder.text.PsiUtil;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
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
import java.util.HashMap;
import java.util.Map;


public class AICoderMainMenu extends ActionGroup {

    public static void handle(@NotNull Throwable ex) {
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean hasSelection(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        return null != caret && caret.hasSelection();
    }

    /**
     * This method is used to get the children of the action.
     *
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
        String humanLanguage = AppSettingsState.getInstance().humanLanguage;
        VirtualFile file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
        String extension = file.getExtension().toLowerCase();

        final String computerLanguage;
        switch (extension) {
            case "py":
                computerLanguage = "python";
                break;
            case "sh":
                computerLanguage = "bash";
                break;
            case "gradle":
                computerLanguage = "groovy";
                break;
            case "md":
                computerLanguage = "markdown";
                break;
            default:
                computerLanguage = extension;
                break;
        }

        String commentLinePrefix;
        switch (extension) {
            case "java":
            case "scala":
            case "groovy":
            case "svg":
            case "gradle":
                commentLinePrefix = "// ";
                break;
            case "sql":
            case "py":
            case "sh":
                commentLinePrefix = "# ";
                break;
            case "md":
                commentLinePrefix = "<!-- ";
                break;
            // Default case
            default:
                commentLinePrefix = "?";
                break;
        }

        ArrayList<AnAction> children = new ArrayList<>();
        if (CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
            pasteAction(children, extension);
        }
        switch (extension) {
            case "java":
                docAction(children, extension, "JavaDoc", "*/");
                break;
            case "scala":
                docAction(children, extension, "ScalaDoc", "*/");
                break;
        }
        switch (extension) {
            case "java":
            case "scala":
            case "groovy":
            case "svg":
            case "gradle":
            case "sql":
            case "py":
            case "sh":
                if (hasSelection(e)) {
                    customTranslation(children, extension);
                    autoImplementationAction(e, children, extension, humanLanguage);
                    describeAction(children, extension, humanLanguage, commentLinePrefix);
                    standardCodeActions(children, extension, humanLanguage);
                }
                break;
            case "md":
                if (hasSelection(e)) {
                    customTranslation(children, computerLanguage);
                    markdownImplementationAction(e, children, humanLanguage);
                    describeAction(children, computerLanguage, humanLanguage, commentLinePrefix);
                    standardCodeActions(children, computerLanguage, humanLanguage);
                }
                break;
            // Default case
            default:
                break;
        }
        return children.toArray(AnAction[]::new);
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
            // Set the instruction to "Translate this input into " + extension
            String instruction = "Translate this input into " + extension;
            // Set the input attributes to "language: autodetect"
            Map<String, String> inputAttr = Map.of("language", "autodetect");
            // Set the output attributes to "language: " + extension
            Map<String, String> outputAttr = Map.of("language", extension);
            // Get the contents of the clipboard
            String pasteContents = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor).toString();
            // Return the result of the OpenAIAPI xmlFN function
            return OpenAI.INSTANCE.xmlFN(pasteContents, "source", "translated", instruction, inputAttr, outputAttr, "");
        }));
    }

    /**
     * Private method to add JavaDoc (or similar) comments to a given code block.
     *
     * @param children         The list of actions to add the comment action to.
     * @param computerLanguage The language of the code block.
     * @param docType          The type of documentation to add.
     * @param stop             An array of strings to stop the comment generation at.
     */
    private void docAction(@NotNull ArrayList<AnAction> children, String computerLanguage, String docType, String... stop) {
        children.add(new AnAction("Add " + docType + " Comments", "Add " + docType + " Comments", null) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent event) {
                try {
                    PsiFile psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE);
                    Caret caret = event.getData(CommonDataKeys.CARET);
                    PsiElement smallestIntersectingMethod = PsiUtil.getSmallestIntersectingEntity(psiFile, caret.getSelectionStart(), caret.getSelectionEnd());
                    if (null == smallestIntersectingMethod) return;
                    String code = smallestIntersectingMethod.getText();
                    String instruction = "Rewrite to include detailed " + docType;
                    if (!AppSettingsState.getInstance().style.isEmpty())
                        instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
                    Map<String, String> inputAttr = new HashMap<>(Map.of("type", "uncommented"));
                    Map<String, String> outputAttr = new HashMap<>(Map.of("type", "commented"));
                    if (!AppSettingsState.getInstance().style.isEmpty())
                        outputAttr.put("style", AppSettingsState.getInstance().style);
                    IndentedText indentedInput = IndentedText.fromString(code);
                    String replace = OpenAI.INSTANCE.xmlFN(indentedInput.textBlock, computerLanguage, computerLanguage, instruction, inputAttr, outputAttr, "", stop) + "*/\n";
                    final String newText = new IndentedText(indentedInput.indent, replace + indentedInput.textBlock).toString();
                    final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        editor.getDocument().replaceString(smallestIntersectingMethod.getTextRange().getStartOffset(), smallestIntersectingMethod.getTextRange().getEndOffset(), newText);
                    });
                } catch (IOException ex) {
                    handle(ex);
                }
            }
        });
    }

    /**
     * This method adds two actions to the list of children.
     * The first action is called "Edit..." and it allows you to type in an instruction.
     * The second action is a group of actions that are based on the instructions you have typed in before.
     *
     * @param children         The list of children to add the actions to.
     * @param computerLanguage The language of the computer.
     */
    private void customTranslation(@NotNull ArrayList<AnAction> children, String computerLanguage) {
        children.add(TextReplacementAction.create("Edit...", "Edit...", null, (event, string) -> {
            String instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE);
            AppSettingsState.getInstance().addInstructionToHistory(instruction);
            Map<String, String> inputAttr = new HashMap<>(Map.of("type", "before"));
            Map<String, String> outputAttr = new HashMap<>(Map.of("type", "after"));
            IndentedText indentedInput = IndentedText.fromString(string);
            return new IndentedText(indentedInput.indent, OpenAI.INSTANCE.xmlFN(indentedInput.textBlock, computerLanguage, computerLanguage, instruction, inputAttr, outputAttr, "")).toString();
        }));
        children.add(new ActionGroup("Recent Edits", true) {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                ArrayList<AnAction> children = new ArrayList<>();
                AppSettingsState.getInstance().getInstructionHistory().forEach(instruction -> quickTranslation(children, computerLanguage, instruction));
                return children.toArray(AnAction[]::new);
            }
        });
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
            AppSettingsState.getInstance().addInstructionToHistory(instruction);
            Map<String, String> inputAttr = new HashMap<>(Map.of("type", "before"));
            Map<String, String> outputAttr = new HashMap<>(Map.of("type", "after"));
            IndentedText indentedInput = IndentedText.fromString(string);
            return new IndentedText(indentedInput.indent, OpenAI.INSTANCE.xmlFN(indentedInput.textBlock, computerLanguage, computerLanguage, instruction, inputAttr, outputAttr, "")).toString();
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
            children.add(TextReplacementAction.create("Execute Directive", "Execute Directive", null, (event, directive) -> {
                String instruction = "Implement " + humanLanguage + " as " + computerLanguage + " code";
                if (!AppSettingsState.getInstance().style.isEmpty())
                    instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
                Map<String, String> inputAttr = new HashMap<>(Map.of("type", "instruction"));
                Map<String, String> outputAttr = new HashMap<>(Map.of("type", "document"));
                if (!AppSettingsState.getInstance().style.isEmpty())
                    outputAttr.put("style", AppSettingsState.getInstance().style);
                IndentedText indentedInput = IndentedText.fromString(directive);

                PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);
                PsiMarkdownContext root = PsiMarkdownContext.getContext(psiFile, selectionStart, selectionEnd);
                String contextWithDirective = root.toString(selectionEnd) + "\n<!-- " + directive + "-->\n";

                String implementation = OpenAI.INSTANCE.xmlFN(
                        directive,
                        humanLanguage,
                        computerLanguage,
                        instruction,
                        inputAttr,
                        outputAttr,
                        contextWithDirective + "\n",
                        "#");
                return new IndentedText(indentedInput.indent, implementation).toString();
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
                    if (!AppSettingsState.getInstance().style.isEmpty())
                        instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
                    Map<String, String> inputAttr = new HashMap<>(Map.of("type", "instruction"));
                    Map<String, String> outputAttr = new HashMap<>(Map.of("type", "code"));
                    if (!AppSettingsState.getInstance().style.isEmpty())
                        outputAttr.put("style", AppSettingsState.getInstance().style);
                    IndentedText indentedInput = IndentedText.fromString(string);
                    String implementation = null;
                    try {
                        implementation = OpenAI.INSTANCE.xmlFN(
                                string.split(" ").length > 4 ? string : largestIntersectingComment.getText(),
                                humanLanguage,
                                computerLanguage,
                                instruction,
                                inputAttr,
                                outputAttr,
                                root + "\n");
                    } catch (IOException ex) {
                        handle(ex);
                    }
                    return new IndentedText(indentedInput.indent, implementation).toString();
                }

            });
        }
    }

    /**
     * This code is creating a TextReplacementAction object that will be added to an ArrayList of AnAction objects.
     * This TextReplacementAction object will take a string of code written in a computer language (specified by the computerLanguage parameter) and explain it in a human language (specified by the humanLanguage parameter).
     * The explanation will be preceded by a comment line prefix (specified by the commentLinePrefix parameter).
     * The OpenAIAPI xmlFN function will be used to generate the explanation, and the input and output attributes will be set to "type: code" and "type: description" respectively.
     * The result of the xmlFN function will be returned and appended to the original string of code, preceded by the comment line prefix.
     *
     * @param children
     * @param computerLanguage
     * @param humanLanguage
     * @param commentLinePrefix
     */
    private void describeAction(@NotNull ArrayList<AnAction> children, String computerLanguage, String humanLanguage, String commentLinePrefix) {
        children.add(TextReplacementAction.create("Describe Code and Prepend Comment", "Add JavaDoc Comments", null, (event, string) -> {
            String instruction = "Explain this " + computerLanguage + " in " + humanLanguage;
            if (!AppSettingsState.getInstance().style.isEmpty())
                instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
            // Set the input attributes to "type: uncommented"
            Map<String, String> inputAttr = new HashMap<>(Map.of("type", "code"));
            // Set the output attributes to "type: commented"
            Map<String, String> outputAttr = new HashMap<>(Map.of("type", "description"));
            if (!AppSettingsState.getInstance().style.isEmpty())
                outputAttr.put("style", AppSettingsState.getInstance().style);
            // Return the result of the OpenAIAPI xmlFN function
            IndentedText indentedInput = IndentedText.fromString(string);
            String replace = OpenAI.INSTANCE.xmlFN(indentedInput.textBlock, computerLanguage, humanLanguage, instruction, inputAttr, outputAttr, "").replace("\n", "\n" + commentLinePrefix) + "\n";
            return new IndentedText(indentedInput.indent, replace + indentedInput.textBlock).toString();
        }));
    }

    /**
     * This code is creating three TextReplacementActions and adding them to an ArrayList.
     * The first action is for adding code comments to a line of code written in a computer language.
     * The second action is for implementing a specification written in a human language into a computer language.
     * The third action is for describing code written in a computer language into a human language.
     *
     * @param children
     * @param computerLanguage
     * @param humanLanguage
     */
    private void standardCodeActions(@NotNull ArrayList<AnAction> children, String computerLanguage, @NotNull String humanLanguage) {
        // Add a TextReplacementAction to the ArrayList
        children.add(TextReplacementAction.create("Add Code Comments", "Add Code Comments", null, (event, string) -> {
            // Set the instruction to "Rewrite to include detailed code comments at the end of every line"
            String instruction = "Rewrite to include detailed code comments for every line";
            if (!AppSettingsState.getInstance().style.isEmpty())
                instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
            // Set the input attributes to "type: uncommented"
            Map<String, String> inputAttr = Map.of("type", "uncommented");
            // Set the output attributes to "type: commented"
            Map<String, String> outputAttr = new HashMap<>(Map.of("type", "commented"));
            if (!AppSettingsState.getInstance().style.isEmpty())
                outputAttr.put("style", AppSettingsState.getInstance().style);
            // Return the result of the OpenAIAPI xmlFN function
            return OpenAI.INSTANCE.xmlFN(string, computerLanguage, computerLanguage, instruction, inputAttr, outputAttr, "");
        }));
        // Add a TextReplacementAction to the ArrayList
        children.add(TextReplacementAction.create("From " + humanLanguage, String.format("Implement %s -> %s", humanLanguage, computerLanguage), null, (event, string) -> {
            // Set the instruction to "Implement this specification"
            String instruction = "Implement this specification";
            // Set the input attributes to "type: input"
            Map<String, String> inputAttr = Map.of("type", "input");
            // Set the output attributes to "type: output"
            Map<String, String> outputAttr = Map.of("type", "output");
            // Return the result of the OpenAIAPI xmlFN function
            return OpenAI.INSTANCE.xmlFN(string, humanLanguage.toLowerCase(), computerLanguage, instruction, inputAttr, outputAttr, "");
        }));
        // Add a TextReplacementAction to the ArrayList
        children.add(TextReplacementAction.create("To " + humanLanguage, String.format("Describe %s -> %s", humanLanguage, computerLanguage), null, (event, string) -> {
            // Set the instruction to "Describe this code"
            String instruction = "Describe this code";
            if (!AppSettingsState.getInstance().style.isEmpty())
                instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
            // Set the input attributes to "type: input"
            Map<String, String> inputAttr = Map.of("type", "input");
            // Set the output attributes to "type: output"
            Map<String, String> outputAttr = new HashMap<>(Map.of("type", "output"));
            if (!AppSettingsState.getInstance().style.isEmpty())
                outputAttr.put("style", AppSettingsState.getInstance().style);
            // Return the result of the OpenAIAPI xmlFN function
            return OpenAI.INSTANCE.xmlFN(string, computerLanguage, humanLanguage.toLowerCase(), instruction, inputAttr, outputAttr, "");
        }));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
        super.update(e);
    }
}