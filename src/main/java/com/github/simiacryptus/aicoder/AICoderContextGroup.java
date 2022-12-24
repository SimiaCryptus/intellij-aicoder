package com.github.simiacryptus.aicoder;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


/**
 * This is the AICoderContextGroup class.
 * It is an ActionGroup that helps us do some cool stuff.
 * <p>
 * If the CopyPasteManager has DataFlavors available, it adds a TextReplacementAction to the ArrayList.
 * <p>
 * For Java, Scala, Groovy, SVG, and SQL, it adds standard language actions.
 * For Python and Bash, it adds standard language actions with the language set to Python and Bash respectively.
 * For Gradle, it adds standard language actions with the language set to Groovy.
 * For Markdown, it adds a Markdown implementation action and standard language actions with the language set to Markdown.
 */
public class AICoderContextGroup extends ActionGroup {

    // Set the human language to English

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
            children.add(TextReplacementAction.create("Paste", "Paste", Icons.icon2, (event, string) -> {
                // Set the instruction to "Translate this input into " + extension
                String instruction = "Translate this input into " + extension;
                // Set the input attributes to "language: autodetect"
                Map<String, String> inputAttr = Map.of("language", "autodetect");
                // Set the output attributes to "language: " + extension
                Map<String, String> outputAttr = Map.of("language", extension);
                // Get the contents of the clipboard
                String pasteContents = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor).toString();
                // Return the result of the OpenAIAPI xmlFN function
                return OpenAIAPI.INSTANCE.xmlFN(pasteContents, "source", "translated", instruction, inputAttr, outputAttr, "");
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
                markdownImplementationAction(e, children, humanLanguage);
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
        autoImplementationAction(e, children, bash, humanLanguage);
        standardCodeActions(children, bash, humanLanguage);
        describeAction(children, bash, humanLanguage, commentLinePrefix);
        customTranslation(children, bash);
    }

    private void docAction(ArrayList<AnAction> children, String computerLanguage, String docType, String... stop) {
        children.add(TextReplacementAction.create("Add " + docType + " Comments", "Add " + docType + " Comments", Icons.icon1, (event, string) -> {
            String instruction = "Rewrite to include detailed " + docType;
            if (!AppSettingsState.getInstance().style.isEmpty())
                instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
            Map<String, String> inputAttr = new HashMap<>(Map.of("type", "uncommented"));
            Map<String, String> outputAttr = new HashMap<>(Map.of("type", "commented"));
            if (!AppSettingsState.getInstance().style.isEmpty())
                outputAttr.put("style", AppSettingsState.getInstance().style);
            IndentedText indentedInput = IndentedText.fromString(string);
            String replace = OpenAIAPI.INSTANCE.xmlFN(indentedInput.textBlock, computerLanguage, computerLanguage, instruction, inputAttr, outputAttr, "", stop) + "*/\n";
            return new IndentedText(indentedInput.indent, replace + indentedInput.textBlock).toString();
        }));
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
        children.add(TextReplacementAction.create("Edit...", "Edit...", Icons.icon2, (event, string) -> {
            String instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE);
            AppSettingsState.getInstance().addInstructionToHistory(instruction);
            Map<String, String> inputAttr = new HashMap<>(Map.of("type", "before"));
            Map<String, String> outputAttr = new HashMap<>(Map.of("type", "after"));
            IndentedText indentedInput = IndentedText.fromString(string);
            return new IndentedText(indentedInput.indent, OpenAIAPI.INSTANCE.xmlFN(indentedInput.textBlock, computerLanguage, computerLanguage, instruction, inputAttr, outputAttr, "")).toString();
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
        children.add(TextReplacementAction.create(instruction, instruction, Icons.icon2, (event, string) -> {
            AppSettingsState.getInstance().addInstructionToHistory(instruction);
            Map<String, String> inputAttr = new HashMap<>(Map.of("type", "before"));
            Map<String, String> outputAttr = new HashMap<>(Map.of("type", "after"));
            IndentedText indentedInput = IndentedText.fromString(string);
            return new IndentedText(indentedInput.indent, OpenAIAPI.INSTANCE.xmlFN(indentedInput.textBlock, computerLanguage, computerLanguage, instruction, inputAttr, outputAttr, "")).toString();
        }));
    }

    private void markdownImplementationAction(AnActionEvent e, ArrayList<AnAction> children, String humanLanguage) {
        String computerLanguage = "markdown";
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return;
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        if (selectionStart < selectionEnd) {
            children.add(TextReplacementAction.create("Execute Directive", "Execute Directive", Icons.icon1, (event, directive) -> {
                String instruction = "Implement " + humanLanguage + " as " + computerLanguage + " code";
                if (!AppSettingsState.getInstance().style.isEmpty())
                    instruction = String.format("%s (%s)", instruction, AppSettingsState.getInstance().style);
                Map<String, String> inputAttr = new HashMap<>(Map.of("type", "instruction"));
                Map<String, String> outputAttr = new HashMap<>(Map.of("type", "document"));
                if (!AppSettingsState.getInstance().style.isEmpty())
                    outputAttr.put("style", AppSettingsState.getInstance().style);
                IndentedText indentedInput = IndentedText.fromString(directive);

                PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);
                PsiElement largestIntersectingComment = getLargestIntersectingComment(psiFile, selectionStart, selectionEnd);
                PsiMarkdownContext root = PsiMarkdownContext.getContext(psiFile, selectionStart, selectionEnd);
                String contextWithDirective = root.toString(selectionEnd) + "\n<!-- " + directive + "-->\n";

                String implementation = OpenAIAPI.INSTANCE.xmlFN(
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

    private void autoImplementationAction(AnActionEvent e, ArrayList<AnAction> children, String computerLanguage, String humanLanguage) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return;
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return;
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        PsiElement largestIntersectingComment = getLargestIntersectingComment(psiFile, selectionStart, selectionEnd);
        if (largestIntersectingComment != null) {
            children.add(new AnAction("Insert Implementation", "Insert Implementation", Icons.icon1) {
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
                    String implementation = OpenAIAPI.INSTANCE.xmlFN(
                            string.split(" ").length > 4 ? string : largestIntersectingComment.getText(),
                            humanLanguage,
                            computerLanguage,
                            instruction,
                            inputAttr,
                            outputAttr,
                            root.toString() + "\n");
                    return new IndentedText(indentedInput.indent, implementation).toString();
                }

            });
        }
    }


    /**
     * This method gets the largest comment that intersects with the given selection.
     * <p>
     * It takes in an element, a selection start, and a selection end.
     * <p>
     * It then looks through the element and its children to find a comment that is within the selection.
     * <p>
     * If it finds one, it compares it to the other comments it finds and keeps the one with the longest text.
     * <p>
     * Finally, it returns the largest comment it found.
     */
    private PsiElement getLargestIntersectingComment(PsiElement element, int selectionStart, int selectionEnd) {
        final AtomicReference<PsiElement> largest = new AtomicReference<>(null);
        final AtomicReference<PsiElementVisitor> visitor = new AtomicReference<>();
        visitor.set(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (null == element) return;
                TextRange textRange = element.getTextRange();
                boolean within = (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() + 1 >= selectionStart && textRange.getStartOffset() <= selectionEnd && textRange.getEndOffset() + 1 >= selectionEnd);
                String simpleName = element.getClass().getSimpleName();
                if (simpleName.equals("PsiCommentImpl") || simpleName.equals("PsiDocCommentImpl")) {
                    if (within) {
                        largest.updateAndGet(s -> (s == null ? 0 : s.getText().length()) > element.getText().length() ? s : element);
                    }
                }
                super.visitElement(element);
                element.acceptChildren(visitor.get());
            }
        });
        element.accept(visitor.get());
        return largest.get();
    }

    private void describeAction(ArrayList<AnAction> children, String computerLanguage, String humanLanguage, String commentLinePrefix) {
        children.add(TextReplacementAction.create("Describe Code and Prepend Comment", "Add JavaDoc Comments", Icons.icon1, (event, string) -> {
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
            String replace = OpenAIAPI.INSTANCE.xmlFN(indentedInput.textBlock, computerLanguage, humanLanguage, instruction, inputAttr, outputAttr, "").replace("\n", "\n" + commentLinePrefix) + "\n";
            return new IndentedText(indentedInput.indent, replace + indentedInput.textBlock).toString();
        }));
    }

    private void standardCodeActions(ArrayList<AnAction> children, String computerLanguage, String humanLanguage) {
        // Add a TextReplacementAction to the ArrayList
        children.add(TextReplacementAction.create("Add Code Comments", "Add Code Comments", Icons.icon1, (event, string) -> {
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
            return OpenAIAPI.INSTANCE.xmlFN(string, computerLanguage, computerLanguage, instruction, inputAttr, outputAttr, "");
        }));
        // Add a TextReplacementAction to the ArrayList
        children.add(TextReplacementAction.create("From " + humanLanguage, String.format("Implement %s -> %s", humanLanguage, computerLanguage), Icons.icon1, (event, string) -> {
            // Set the instruction to "Implement this specification"
            String instruction = "Implement this specification";
            // Set the input attributes to "type: input"
            Map<String, String> inputAttr = Map.of("type", "input");
            // Set the output attributes to "type: output"
            Map<String, String> outputAttr = Map.of("type", "output");
            // Return the result of the OpenAIAPI xmlFN function
            return OpenAIAPI.INSTANCE.xmlFN(string, humanLanguage.toLowerCase(), computerLanguage, instruction, inputAttr, outputAttr, "");
        }));
        // Add a TextReplacementAction to the ArrayList
        children.add(TextReplacementAction.create("To " + humanLanguage, String.format("Describe %s -> %s", humanLanguage, computerLanguage), Icons.icon1, (event, string) -> {
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
            return OpenAIAPI.INSTANCE.xmlFN(string, computerLanguage, humanLanguage.toLowerCase(), instruction, inputAttr, outputAttr, "");
        }));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
        super.update(e);
    }
}