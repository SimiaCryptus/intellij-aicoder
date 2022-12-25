package com.github.simiacryptus.aicoder;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.OpenAI;
import com.github.simiacryptus.aicoder.text.IndentedText;
import com.github.simiacryptus.aicoder.text.PsiClassContext;
import com.github.simiacryptus.aicoder.text.PsiMarkdownContext;
import com.github.simiacryptus.aicoder.text.PsiUtil;
import com.intellij.openapi.actionSystem.*;
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


/**
 * This is the AICoderContextGroup class.
 * It is an ActionGroup that helps us do some cool stuff.
 *
 * If the CopyPasteManager has DataFlavors available, it adds a TextReplacementAction to the ArrayList.
 * For Java, Scala, Groovy, SVG, and SQL, it adds standard language actions.
 * For Python and Bash, it adds standard language actions with the language set to Python and Bash respectively.
 * For Gradle, it adds standard language actions with the language set to Groovy.
 * For Markdown, it adds a Markdown implementation action and standard language actions with the language set to Markdown.
 */
public class AICoderMainMenu extends ActionGroup {

    public static void handle(Throwable ex) {
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean hasSelection(AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        return null != caret && caret.hasSelection();
    }

    @Override
    public AnAction @NotNull [] getChildren(AnActionEvent e) {
        String humanLanguage = AppSettingsState.getInstance().humanLanguage;
        // Get the VirtualFile associated with the current action event
        VirtualFile file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
        // Create an ArrayList to store the generated AnAction objects
        ArrayList<AnAction> children = new ArrayList<>();
        // Get the file extension of the VirtualFile
        String extension = file.getExtension().toLowerCase();

        // If the CopyPasteManager has DataFlavors available
        if (CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
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

        // Switch on the file extension
        switch (extension) {
            case "java":
                docAction(children, extension, "JavaDoc", "*/");
                standardLanguageActions(e, children, humanLanguage, extension, "// ");
                break;
            case "scala":
                docAction(children, extension, "ScalaDoc", "*/");
            case "groovy":
            case "svg":
                standardLanguageActions(e, children, humanLanguage, extension, "// ");
                break;
            case "sql":
                standardLanguageActions(e, children, humanLanguage, extension, "# ");
                break;
            case "py":
                standardLanguageActions(e, children, humanLanguage, "python", "# ");
                break;
            case "sh":
                standardLanguageActions(e, children, humanLanguage, "bash", "# ");
                break;
            case "gradle":
                standardLanguageActions(e, children, humanLanguage, "groovy", "// ");
                break;
            case "md":
                if (hasSelection(e)) markdownImplementationAction(e, children, humanLanguage);
                standardLanguageActions(e, children, humanLanguage, "markdown", "<!-- ");
                break;
            // Default case
            default:
                break;
        }

        // Return the ArrayList as an array of AnAction objects
        return children.toArray(AnAction[]::new);
    }

    private void standardLanguageActions(AnActionEvent e, ArrayList<AnAction> children, String humanLanguage, String bash, String commentLinePrefix) {
        boolean hasSelection = hasSelection(e);
        if (hasSelection) customTranslation(children, bash);
        if (hasSelection) autoImplementationAction(e, children, bash, humanLanguage);
        if (hasSelection) describeAction(children, bash, humanLanguage, commentLinePrefix);
        if (hasSelection) standardCodeActions(children, bash, humanLanguage);
    }

    private void docAction(ArrayList<AnAction> children, String computerLanguage, String docType, String... stop) {
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
    private void customTranslation(ArrayList<AnAction> children, String computerLanguage) {
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

    private void quickTranslation(ArrayList<AnAction> children, String computerLanguage, String instruction) {
        children.add(TextReplacementAction.create(instruction, instruction, null, (event, string) -> {
            AppSettingsState.getInstance().addInstructionToHistory(instruction);
            Map<String, String> inputAttr = new HashMap<>(Map.of("type", "before"));
            Map<String, String> outputAttr = new HashMap<>(Map.of("type", "after"));
            IndentedText indentedInput = IndentedText.fromString(string);
            return new IndentedText(indentedInput.indent, OpenAI.INSTANCE.xmlFN(indentedInput.textBlock, computerLanguage, computerLanguage, instruction, inputAttr, outputAttr, "")).toString();
        }));
    }

    private void markdownImplementationAction(AnActionEvent e, ArrayList<AnAction> children, String humanLanguage) {
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
        ;
    }

    /**
     * Automatically implements a given action.
     *
     * @param e                the action event
     * @param children         the list of actions
     * @param computerLanguage the computer language to implement
     * @param humanLanguage    the human language to implement
     */
    private void autoImplementationAction(AnActionEvent e, ArrayList<AnAction> children, String computerLanguage, String humanLanguage) {
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

                protected String implement(@NotNull AnActionEvent event, String string) {
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
                                root.toString() + "\n");
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
    private void describeAction(ArrayList<AnAction> children, String computerLanguage, String humanLanguage, String commentLinePrefix) {
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
    private void standardCodeActions(ArrayList<AnAction> children, String computerLanguage, String humanLanguage) {
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