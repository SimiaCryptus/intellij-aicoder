package com.github.simiacryptus.aicoder;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.ModerationException;
import com.github.simiacryptus.aicoder.openai.OpenAI;
import com.github.simiacryptus.aicoder.psi.PsiClassContext;
import com.github.simiacryptus.aicoder.psi.PsiMarkdownContext;
import com.github.simiacryptus.aicoder.psi.PsiUtil;
import com.github.simiacryptus.aicoder.text.IndentedText;
import com.github.simiacryptus.aicoder.text.StringTools;
import com.github.simiacryptus.aicoder.text.TextBlockFactory;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EditorMenu extends ActionGroup {


    private static final Logger log = Logger.getInstance(EditorMenu.class);

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

            addIfNotNull(children, rewordCommentAction(e, language, inputHumanLanguage));

            if (settings.devActions) {
                addIfNotNull(children, printTreeAction(e));
            }

            if (CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
                children.add(pasteAction(language.name()));
            }

            if (!language.docStyle.isEmpty()) children.add(docAction(extension, language));

            if (language == ComputerLanguage.Markdown) {
                addIfNotNull(children, markdownListAction(e));
                addIfNotNull(children, markdownNewTableRowsAction(e));
                addIfNotNull(children, markdownNewTableColsAction(e));
                addIfNotNull(children, markdownNewTableColsAction2(e));
            }

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
    public static TextReplacementAction toHumanLanguageAction(String outputHumanLanguage, ComputerLanguage language) {
        String computerLanguage = language.name();
        return TextReplacementAction.create("_To " + outputHumanLanguage, String.format("Describe %s -> %s", outputHumanLanguage, computerLanguage), null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            CompletionRequest completionRequest = settings.createTranslationRequest()
                    .setInstruction(getInstruction(settings.style, "Describe this code"))
                    .setInputText(string)
                    .setInputType(computerLanguage)
                    .setInputAttribute("type", "input")
                    .setOutputType(outputHumanLanguage.toLowerCase())
                    .setOutputAttrute("type", "output")
                    .setOutputAttrute("style", settings.style)
                    .buildCompletionRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return completionRequest.complete(indent);
        });
    }

    @NotNull
    public static TextReplacementAction fromHumanLanguageAction(String inputHumanLanguage, ComputerLanguage language) {
        String computerLanguage = language.name();
        return TextReplacementAction.create("_From " + inputHumanLanguage, String.format("Implement %s -> %s", inputHumanLanguage, computerLanguage), null, (event, string) -> {
            CompletionRequest completionRequest = AppSettingsState.getInstance().createTranslationRequest()
                    .setInputType(inputHumanLanguage.toLowerCase())
                    .setOutputType(computerLanguage)
                    .setInstruction("Implement this specification")
                    .setInputAttribute("type", "input")
                    .setOutputAttrute("type", "output")
                    .setInputText(string)
                    .buildCompletionRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return completionRequest.complete(indent);
        });
    }

    @NotNull
    public static TextReplacementAction addCodeCommentsAction(String outputHumanLanguage, ComputerLanguage language) {
        String computerLanguage = language.name();
        return TextReplacementAction.create("Add Code _Comments", "Add Code Comments", null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            CompletionRequest completionRequest = settings.createTranslationRequest()
                    .setInputType(computerLanguage)
                    .setOutputType(computerLanguage)
                    .setInstruction(getInstruction(settings.style, "Rewrite to include detailed " + outputHumanLanguage + " code comments for every line"))
                    .setInputAttribute("type", "uncommented")
                    .setOutputAttrute("type", "commented")
                    .setOutputAttrute("style", settings.style)
                    .setInputText(string)
                    .buildCompletionRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return completionRequest.complete(indent);
        });
    }

    @NotNull
    public static TextReplacementAction describeAction(String outputHumanLanguage, ComputerLanguage language) {
        return TextReplacementAction.create("_Describe Code and Prepend Comment", "Add JavaDoc Comments", null, (event, inputString) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            CompletionRequest completionRequest = settings.createTranslationRequest()
                    .setInputType(language.name())
                    .setOutputType(outputHumanLanguage)
                    .setInstruction(getInstruction(settings.style, "Explain this " + language.name() + " in " + outputHumanLanguage))
                    .setInputAttribute("type", "code")
                    .setOutputAttrute("type", "description")
                    .setOutputAttrute("style", settings.style)
                    .setInputText(IndentedText.fromString(inputString).getTextBlock().trim())
                    .buildCompletionRequest();
            return Futures.transform(
                    completionRequest.complete(indent),
                    description->"\n" + indent + language.blockComment.fromString(StringTools.lineWrapping(description.trim(), 120)).withIndent(indent) + "\n" + indent + inputString,
                    OpenAI.INSTANCE.pool);
        });
    }

    public static String getInstruction(String style, String instruction) {
        if (style.isEmpty()) return instruction;
        return String.format("%s (%s)", instruction, style);
    }

    @NotNull
    public static TextReplacementAction customEdit(String computerLanguage) {
        return TextReplacementAction.create("_Edit...", "Edit...", null, (event, string) -> {
            String instruction = JOptionPane.showInputDialog(null, "Instruction:", "Edit Code", JOptionPane.QUESTION_MESSAGE);
            AppSettingsState settings = AppSettingsState.getInstance();
            settings.addInstructionToHistory(instruction);
            CompletionRequest completionRequest = settings.createTranslationRequest()
                    .setInputType(computerLanguage)
                    .setOutputType(computerLanguage)
                    .setInstruction(instruction)
                    .setInputAttribute("type", "before")
                    .setOutputAttrute("type", "after")
                    .setInputText(IndentedText.fromString(string).getTextBlock())
                    .buildCompletionRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return completionRequest.complete(indent);
        });
    }

    /*
     *
     * Creates a new ActionGroup for recent edits.
     *
     * @param computerLanguage the language of the edit
     * @return a new ActionGroup for recent edits
     */
    @NotNull
    public static ActionGroup recentEdits(String computerLanguage) {
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
    public static AnAction docAction(String extension, ComputerLanguage language) {
        return new AnAction("_Add " + language.docStyle + " Comments", "Add " + language.docStyle + " Comments", null) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent event) {
                Caret caret = event.getData(CommonDataKeys.CARET);
                PsiFile psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE);
                PsiElement smallestIntersectingMethod = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd());
                if (null == smallestIntersectingMethod) return;
                AppSettingsState settings = AppSettingsState.getInstance();
                String code = smallestIntersectingMethod.getText();
                IndentedText indentedInput = IndentedText.fromString(code);
                String indent = indentedInput.getIndent();
                CompletionRequest completionRequest = settings.createTranslationRequest()
                        .setInputType(extension)
                        .setOutputType(extension)
                        .setInstruction(getInstruction(settings.style, "Rewrite to include detailed " + language.docStyle))
                        .setInputAttribute("type", "uncommented")
                        .setOutputAttrute("type", "commented")
                        .setOutputAttrute("style", settings.style)
                        .setInputText(indentedInput.getTextBlock())
                        .buildCompletionRequest()
                        .addStops(language.getMultilineCommentSuffix());
                ListenableFuture<String> future = completionRequest.complete("");
                OpenAI.onSuccess(future, docString->{
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        final String newText = language.docComment.fromString(docString.trim()).withIndent(indent) + "\n" + indent + StringTools.trimPrefix(indentedInput.toString());
                        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                        editor.getDocument().replaceString(smallestIntersectingMethod.getTextRange().getStartOffset(), smallestIntersectingMethod.getTextRange().getEndOffset(), newText);
                    });
                });
            }
        };
    }

    /**
     * Creates a paste action for the given language.
     *
     * @param language the language to paste into
     * @return a {@link TextReplacementAction} that pastes the contents of the clipboard into the given language
     */
    @NotNull
    public static TextReplacementAction pasteAction(@NotNull String language) {
        return TextReplacementAction.create("_Paste", "Paste", null, (event, string) -> {
            String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor).toString().trim();
            CompletionRequest completionRequest = AppSettingsState.getInstance().createTranslationRequest()
                    .setInputType("source")
                    .setOutputType("translated")
                    .setInstruction("Translate this input into " + language)
                    .setInputAttribute("language", "autodetect")
                    .setOutputAttrute("language", language)
                    .setInputText(text)
                    .buildCompletionRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return completionRequest.complete(indent);
        });
    }

    public static String getIndent(Caret caret) {
        if (null == caret) return "";
        Document document = caret.getEditor().getDocument();
        return IndentedText.fromString(document.getText().split("\n")[document.getLineNumber(caret.getSelectionStart())]).getIndent();
    }

    @NotNull
    public static TextReplacementAction customEdit(String computerLanguage, String instruction) {
        return TextReplacementAction.create(instruction, instruction, null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            settings.addInstructionToHistory(instruction);
            CompletionRequest completionRequest = settings.createTranslationRequest()
                    .setInputType(computerLanguage)
                    .setOutputType(computerLanguage)
                    .setInstruction(instruction)
                    .setInputAttribute("type", "before")
                    .setOutputAttrute("type", "after")
                    .setInputText(IndentedText.fromString(string).getTextBlock())
                    .buildCompletionRequest();
            String indent = getIndent(event.getData(CommonDataKeys.CARET));
            return completionRequest.complete(indent);
        });
    }

    @Nullable
    public static AnAction markdownListAction(@NotNull AnActionEvent e) {
        try {
            Caret caret = e.getData(CommonDataKeys.CARET);
            if(null == caret) return null;
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if(null == psiFile) return null;
            PsiElement list = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownListImpl");
            if (null == list) return null;
            return new AnAction("Add _List Items", "Add list items", null) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    AppSettingsState settings = AppSettingsState.getInstance();
                    List<String> items = trim(PsiUtil.getAll(list, "MarkdownListItemImpl").stream().map(item -> PsiUtil.getAll(item, "MarkdownParagraphImpl").get(0).getText()).collect(Collectors.toList()), 10, false);
                    String indent = getIndent(caret);
                    String n = Integer.toString(items.size() * 2);
                    OpenAI.onSuccess(getNewItems(settings, items, n), newItems->{
                        WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                            String strippedList = Arrays.stream(list.getText().split("\n")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.joining("\n"));
                            String bulletString = Stream.of("- [ ] ", "- ", "* ")
                                    .filter(strippedList::startsWith).findFirst().orElse("1. ");
                            String itemText = indent + newItems.stream().map(x -> bulletString + x).collect(Collectors.joining("\n" + indent));
                            final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                            editor.getDocument().insertString(list.getTextRange().getEndOffset(), "\n" + itemText);
                        });
                    });
                }
            };
        } catch (Exception ex) {
            log.error(ex);
            return null;
        }
    }

    @NotNull
    private static List<String> trim(List<String> items, int max, boolean preserveHead) {
        items = new ArrayList<>(items);
        Random random = new Random();
        while (items.size() > max) {
            int index = random.nextInt(items.size());
            if (preserveHead && index == 0) continue;
            items.remove(index);
        }
        return items;
    }

    private static ListenableFuture<List<String>> getNewItems(AppSettingsState settings, List<String> items, String n) {
        String listPrefix = "* ";
        CompletionRequest completionRequest = settings.createTranslationRequest()
                .setInstruction(getInstruction(settings.style, "List " + n + " items"))
                .setInputType("instruction")
                .setInputText("List " + n + " items")
                .setOutputType("list")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest()
                .appendPrompt(items.stream().map(x -> listPrefix + x).collect(Collectors.joining("\n")) + "\n" + listPrefix);

        return OpenAI.map(completionRequest.complete(""), complete->{
            return Arrays.stream(complete.split("\n")).map(String::trim).filter(x -> x != null && !x.isEmpty()).map(x -> StringTools.stripPrefix(x, listPrefix)).collect(Collectors.toList());
        });
    }

    @Nullable
    public static AnAction markdownNewTableColsAction(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        PsiElement table = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownTableImpl");
        if (null == table) return null;
        List<String> rows = Arrays.asList(transposeMarkdownTable(PsiUtil.getAll(table, "MarkdownTableRowImpl").stream().map(PsiElement::getText).collect(Collectors.joining("\n")), false, false).split("\n"));
        String n = Integer.toString(rows.size() * 2);
        return new AnAction("Add _Table Columns", "Add table columns", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                AppSettingsState settings = AppSettingsState.getInstance();
                String indent = getIndent(caret);
                OpenAI.onSuccess(newRows(settings, n, rows, ""), newRows->{
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        String newTableTxt = transposeMarkdownTable(Stream.concat(rows.stream(), newRows.stream()).collect(Collectors.joining("\n")), false, true);
                        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                        editor.getDocument().replaceString(table.getTextRange().getStartOffset(), table.getTextRange().getEndOffset(), newTableTxt.replace("\n", "\n" + indent));
                    });
                });
            }
        };
    }

    @Nullable
    public static AnAction markdownNewTableColsAction2(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        PsiElement table = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownTableImpl");
        if (null == table) return null;
        List<String> rows = Arrays.asList(transposeMarkdownTable(PsiUtil.getAll(table, "MarkdownTableRowImpl").stream().map(PsiElement::getText).collect(Collectors.joining("\n")), false, false).split("\n"));
        String n = Integer.toString(rows.size() * 2);
        return new AnAction("Add Table _Column...", "Add table column...", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                AppSettingsState settings = AppSettingsState.getInstance();
                String indent = getIndent(caret);
                String columnName = JOptionPane.showInputDialog(null, "Column Name:", "Add Column", JOptionPane.QUESTION_MESSAGE);
                OpenAI.onSuccess(newRows(settings, n, rows, "| " + columnName + " | "), newRows->{
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        String newTableTxt = transposeMarkdownTable(Stream.concat(rows.stream(), newRows.stream()).collect(Collectors.joining("\n")), false, true);
                        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                        editor.getDocument().replaceString(table.getTextRange().getStartOffset(), table.getTextRange().getEndOffset(), newTableTxt.replace("\n", "\n" + indent));
                    });
                });
            }
        };
    }

    static String transposeMarkdownTable(String table, boolean inputHeader, boolean outputHeader) {
        String[][] cells = parseMarkdownTable(table, inputHeader);
        StringBuilder transposedTable = new StringBuilder();
        int columns = cells[0].length;
        int rows = cells.length;
        if (outputHeader) columns = columns + 1;
        for (int column = 0; column < columns; column++) {
            transposedTable.append("|");
            for (int row = 0; row < rows; row++) {
                String cellValue;
                String[] rowCells = cells[row];
                if (outputHeader) {
                    if (column < 1) {
                        cellValue = rowCells[column].trim();
                    } else if (column == 1) {
                        cellValue = "---";
                    } else if ((column - 1) >= rowCells.length) {
                        cellValue = "";
                    } else {
                        cellValue = rowCells[column - 1].trim();
                    }
                } else {
                    cellValue = rowCells[column].trim();
                }
                transposedTable.append(" ").append(cellValue).append(" |");
            }
            transposedTable.append("\n");
        }
        return transposedTable.toString();
    }

    private static String[][] parseMarkdownTable(String table, boolean removeHeader) {
        ArrayList<String[]> rows = new ArrayList(Arrays.stream(table.split("\n")).map(x -> Arrays.stream(x.split("\\|")).filter(cell -> !cell.isEmpty()).toArray(String[]::new)).collect(Collectors.toList()));
        if (removeHeader) {
            rows.remove(1);
        }
        return rows.stream()
                //.filter(x -> x.length == rows.get(0).length)
                .toArray(String[][]::new);
    }

    @Nullable
    public static AnAction markdownNewTableRowsAction(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        PsiElement table = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownTableImpl");
        if (null == table) return null;
        if (null != table) {
            List<String> rows = trim(PsiUtil.getAll(table, "MarkdownTableRowImpl").stream().map(PsiElement::getText).collect(Collectors.toList()), 10, true);
            String n = Integer.toString(rows.size() * 2);
            return new AnAction("Add _Table Rows", "Add table rows", null) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    AppSettingsState settings = AppSettingsState.getInstance();
                    String indent = getIndent(caret);
                    OpenAI.onSuccess(newRows(settings, n, rows, ""), newRows->{
                        WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                            String itemText = indent + newRows.stream().collect(Collectors.joining("\n" + indent));
                            final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                            editor.getDocument().insertString(table.getTextRange().getEndOffset(), "\n" + itemText);
                        });
                    });
                }
            };
        }
        return null;
    }

    /**
     * Generates a list of strings based on the given parameters.
     *
     * @param settings  The application settings state.
     * @param n         The number of items to list.
     * @param rows      The existing list of rows.
     * @param rowPrefix The prefix for the new rows.
     * @return A list of strings generated from the given parameters.
     * @throws RuntimeException If an IOException or ModerationException occurs.
     */
    private static ListenableFuture<List<String>> newRows(AppSettingsState settings, String n, List<String> rows, String rowPrefix) {
        CompletionRequest completionRequest = settings.createTranslationRequest()
                .setInstruction(getInstruction(settings.style, "List " + n + " items"))
                .setInputType("instruction")
                .setInputText("List " + n + " items")
                .setOutputType("markdown")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest()
                .appendPrompt("\n" + String.join("\n", rows) + "\n" + rowPrefix);
        return OpenAI.map(completionRequest.complete(""), complete->{
            return Arrays.stream((rowPrefix + complete).split("\n")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toList());
        });
    }

    @Nullable
    public static TextReplacementAction markdownContextAction(@NotNull AnActionEvent e, String humanLanguage) {
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
                    //.addStops(new String[]{"#"})
                    CompletionRequest completionRequest = settings.createTranslationRequest()
                            .setOutputType("markdown")
                            .setInstruction(getInstruction(settings.style, String.format("Using Markdown and %s", humanLanguage)))
                            .setInputType("instruction")
                            .setInputText(humanDescription)
                            .setOutputAttrute("type", "document")
                            .setOutputAttrute("style", settings.style)
                            .buildCompletionRequest()
                            //.addStops(new String[]{"#"})
                            .appendPrompt(context);
                    String indent = getIndent(caret);
                    return completionRequest.complete(indent);
                });
            }
        }
        return null;
    }

    public static void addIfNotNull(@NotNull ArrayList<AnAction> children, AnAction action) {
        if (null != action) children.add(action);
    }

    @Nullable
    public static AnAction printTreeAction(@NotNull AnActionEvent e) {
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
    public static AnAction rewordCommentAction(@NotNull AnActionEvent e, ComputerLanguage computerLanguage, String humanLanguage) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        int selectionStart = caret.getSelectionStart();
        int selectionEnd = caret.getSelectionEnd();
        PsiElement largestIntersectingComment = PsiUtil.getLargestIntersectingComment(psiFile, selectionStart, selectionEnd);
        if (largestIntersectingComment == null) return null;
        return new AnAction("_Reword Comment", "Reword Comment", null) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent e1) {
                final Editor editor = e1.getRequiredData(CommonDataKeys.EDITOR);
                AppSettingsState settings = AppSettingsState.getInstance();
                TextBlockFactory<?> commentModel = computerLanguage.getCommentModel(largestIntersectingComment.getText());
                String commentText = commentModel.fromString(largestIntersectingComment.getText().trim()).stream()
                        .map(String::trim)
                        .filter(x -> !x.isEmpty())
                        .reduce((a, b) -> a + "\n" + b).get();
                CompletionRequest completionRequest = settings.createTranslationRequest()
                        .setInstruction(getInstruction(settings.style, "Reword"))
                        .setInputText(commentText)
                        .setInputType(humanLanguage)
                        .setOutputAttrute("type", "input")
                        .setOutputType(humanLanguage)
                        .setOutputAttrute("type", "output")
                        .setOutputAttrute("style", settings.style)
                        .buildCompletionRequest();
                OpenAI.onSuccess(completionRequest.complete(""), result->{
                    WriteCommandAction.runWriteCommandAction(e1.getProject(), () -> {
                        String indent = getIndent(caret);
                        String finalResult = indent + commentModel.fromString(StringTools.lineWrapping(result, 120)).withIndent(indent);
                        editor.getDocument().replaceString(
                                largestIntersectingComment.getTextRange().getStartOffset(),
                                largestIntersectingComment.getTextRange().getEndOffset(),
                                finalResult);
                    });
                });
            }

        };
    }

    @Nullable
    public static AnAction psiClassContextAction(@NotNull AnActionEvent e, ComputerLanguage computerLanguage, String humanLanguage) {
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

                String instruct = (selectedText.split(" ").length > 4 ? selectedText : largestIntersectingComment.getText()).trim();
                String specification = computerLanguage.getCommentModel(instruct).fromString(instruct).stream()
                        .map(String::trim)
                        .filter(x -> !x.isEmpty())
                        .reduce((a, b) -> a + " " + b).get();
                CompletionRequest completionRequest = settings.createTranslationRequest()
                        .setInstruction("Implement " + humanLanguage + " as " + computerLanguage.name() + " code")
                        .setInputType(humanLanguage)
                        .setInputAttribute("type", "instruction")
                        .setInputText(specification)
                        .setOutputType(computerLanguage.name())
                        .setOutputAttrute("type", "code")
                        .setOutputAttrute("style", settings.style)
                        .buildCompletionRequest()
                        .appendPrompt(PsiClassContext.getContext(psiFile, selectionStart, selectionEnd) + "\n");
                String indent = getIndent(caret);
                OpenAI.onSuccess(completionRequest.complete(indent), result->{
                    WriteCommandAction.runWriteCommandAction(e1.getProject(), () -> {
                        editor.getDocument().insertString(largestIntersectingComment.getTextRange().getEndOffset(), "\n" + result);
                    });
                });
            }

        };
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
        super.update(e);
    }
}