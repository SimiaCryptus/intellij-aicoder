package com.github.simiacryptus.aicoder;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.ModerationException;
import com.github.simiacryptus.aicoder.psi.PsiClassContext;
import com.github.simiacryptus.aicoder.psi.PsiMarkdownContext;
import com.github.simiacryptus.aicoder.psi.PsiUtil;
import com.github.simiacryptus.aicoder.util.*;
import com.intellij.core.CoreBundle;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EditorMenu extends ActionGroup {

    private static final Logger log = Logger.getInstance(EditorMenu.class);
    public static final @NotNull
    @Nls String DEFAULT_ACTION_MESSAGE = CoreBundle.message("command.name.undefined");

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

        Caret caret = e.getData(CommonDataKeys.CARET);

        if (null != caret) {
            addIfNotNull(children, redoLast());
            if (!caret.hasSelection()) {
                children.add(genericInsert());
            } else {
                children.add(genericAppend());
            }
        }


        if (language != null) {

            addIfNotNull(children, rewordCommentAction(e, language, inputHumanLanguage));

            if (settings.devActions) {
                addIfNotNull(children, printTreeAction(e));
            }

            if (CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
                children.add(pasteAction(language.name()));
            }

            if (language.docStyle.length() > 0) {
                children.add(docAction(extension, language));
            }

            if (language == ComputerLanguage.Markdown) {
                addIfNotNull(children, markdownListAction(e));
                addIfNotNull(children, markdownNewTableRowsAction(e));
                addIfNotNull(children, markdownNewTableColsAction(e));
                addIfNotNull(children, markdownNewTableColAction(e));
            }

            if (null != caret) {
                if (caret.hasSelection()) {
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
        }

        return children.toArray(AnAction[]::new);
    }

    @NotNull
    protected AnAction toHumanLanguageAction(String outputHumanLanguage, ComputerLanguage language) {
        String computerLanguage = language.name();
        String description = String.format("Describe %s -> %s", outputHumanLanguage, computerLanguage);
        return TextReplacementAction.create("_To " + outputHumanLanguage, description, null,
                (event, string) -> {
                    AppSettingsState settings = AppSettingsState.getInstance();
                    return settings.createTranslationRequest()
                            .setInstruction(UITools.getInstruction("Describe this code"))
                            .setInputText(string)
                            .setInputType(computerLanguage)
                            .setInputAttribute("type", "input")
                            .setOutputType(outputHumanLanguage.toLowerCase())
                            .setOutputAttrute("type", "output")
                            .setOutputAttrute("style", settings.style)
                            .buildCompletionRequest();
                });
    }

    @NotNull
    protected AnAction fromHumanLanguageAction(String inputHumanLanguage, ComputerLanguage language) {
        String computerLanguage = language.name();
        String description = String.format("Implement %s -> %s", inputHumanLanguage, computerLanguage);
        return TextReplacementAction.create("_From " + inputHumanLanguage, description, null, (event, string) ->
                AppSettingsState.getInstance().createTranslationRequest()
                        .setInputType(inputHumanLanguage.toLowerCase())
                        .setOutputType(computerLanguage)
                        .setInstruction("Implement this specification")
                        .setInputAttribute("type", "input")
                        .setOutputAttrute("type", "output")
                        .setInputText(string)
                        .buildCompletionRequest());
    }

    @NotNull
    protected AnAction addCodeCommentsAction(CharSequence outputHumanLanguage, ComputerLanguage language) {
        String computerLanguage = language.name();
        return TextReplacementAction.create("Add Code _Comments", "Add Code Comments", null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            return settings.createTranslationRequest()
                    .setInputType(computerLanguage)
                    .setOutputType(computerLanguage)
                    .setInstruction(UITools.getInstruction("Rewrite to include detailed " + outputHumanLanguage + " code comments for every line"))
                    .setInputAttribute("type", "uncommented")
                    .setOutputAttrute("type", "commented")
                    .setOutputAttrute("style", settings.style)
                    .setInputText(string)
                    .buildCompletionRequest();
        });
    }

    @NotNull
    protected AnAction describeAction(String outputHumanLanguage, ComputerLanguage language) {
        return TextReplacementAction.create("_Describe Code and Prepend Comment", "Add JavaDoc Comments", null, new TextReplacementAction.ActionTextEditorFunction() {
            @Override
            public CompletionRequest apply(AnActionEvent event, String inputString) throws IOException, ModerationException {
                AppSettingsState settings = AppSettingsState.getInstance();
                return settings.createTranslationRequest()
                        .setInputType(language.name())
                        .setOutputType(outputHumanLanguage)
                        .setInstruction(UITools.getInstruction("Explain this " + language.name() + " in " + outputHumanLanguage))
                        .setInputAttribute("type", "code")
                        .setOutputAttrute("type", "description")
                        .setOutputAttrute("style", settings.style)
                        .setInputText(IndentedText.fromString(inputString).getTextBlock().trim())
                        .buildCompletionRequest();
            }

            @Override
            public CharSequence postTransform(AnActionEvent event, CharSequence prompt, CharSequence completion) {
                CharSequence indent = UITools.getIndent(event);
                String wrapping = StringTools.lineWrapping(completion.toString().trim(), 120);
                return "\n" + indent + language.blockComment.fromString(wrapping).withIndent(indent) + "\n" + indent + prompt;
            }
        });
    }

    @NotNull
    protected AnAction genericInsert() {
        return new AnAction("_Insert Text", "Insert Text", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                Caret caret = event.getData(CommonDataKeys.CARET);
                Document document = caret.getEditor().getDocument();
                int caretPosition = caret.getOffset();
                CharSequence before = StringTools.getSuffixForContext(document.getText(new TextRange(0, caretPosition)));
                CharSequence after = StringTools.getPrefixForContext(document.getText(new TextRange(caretPosition, document.getTextLength())));
                AppSettingsState settings = AppSettingsState.getInstance();
                CompletionRequest completionRequest = settings.createCompletionRequest()
                        .appendPrompt(before)
                        .setSuffix(after);
                UITools.redoableRequest(completionRequest, "", event, complete -> {
                    final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                    return UITools.insertString(editor.getDocument(), caretPosition, complete);
                });
            }
        };
    }

    @NotNull
    protected AnAction genericAppend() {
        return new AnAction("_Append Text", "Append Text", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                Caret caret = event.getData(CommonDataKeys.CARET);
                CharSequence before = caret.getSelectedText();
                AppSettingsState settings = AppSettingsState.getInstance();
                CompletionRequest completionRequest = settings.createCompletionRequest()
                        .appendPrompt(before);
                UITools.redoableRequest(completionRequest, "", event, complete -> {
                    final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                    return UITools.insertString(editor.getDocument(), caret.getSelectionEnd(), complete);
                });
            }
        };
    }

    @Nullable
    protected AnAction redoLast() {
        if(UITools.retry.isEmpty()) return null;
        return new AnAction("_Redo Last", "Redo last", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                UITools.retry.pop().run();
            }
        };
    }

    protected AnAction customEdit(String computerLanguage) {
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
                    .setInputText(IndentedText.fromString(string).getTextBlock())
                    .buildCompletionRequest();
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
    protected ActionGroup recentEdits(String computerLanguage) {
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
    protected AnAction docAction(String extension, ComputerLanguage language) {
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
                CharSequence indent = indentedInput.getIndent();
                CompletionRequest completionRequest = settings.createTranslationRequest()
                        .setInputType(extension)
                        .setOutputType(extension)
                        .setInstruction(UITools.getInstruction("Rewrite to include detailed " + language.docStyle))
                        .setInputAttribute("type", "uncommented")
                        .setOutputAttrute("type", "commented")
                        .setOutputAttrute("style", settings.style)
                        .setInputText(indentedInput.getTextBlock())
                        .buildCompletionRequest()
                        .addStops(language.getMultilineCommentSuffix());
                int startOffset = smallestIntersectingMethod.getTextRange().getStartOffset();
                int endOffset = smallestIntersectingMethod.getTextRange().getEndOffset();
                UITools.redoableRequest(completionRequest, "", event, (CharSequence docString) -> {
                    TextBlock reindented = language.docComment.fromString(docString.toString().trim()).withIndent(indent);
                    final CharSequence newText = reindented + "\n" + indent + StringTools.trimPrefix(indentedInput.toString());
                    final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                    return UITools.replaceString(editor.getDocument(), startOffset, endOffset, newText);
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
    protected AnAction pasteAction(@NotNull CharSequence language) {
        return TextReplacementAction.create("_Paste", "Paste", null, (event, string) -> {
            String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor).toString().trim();
            return AppSettingsState.getInstance().createTranslationRequest()
                    .setInputType("source")
                    .setOutputType("translated")
                    .setInstruction("Translate this input into " + language)
                    .setInputAttribute("language", "autodetect")
                    .setOutputAttrute("language", language)
                    .setInputText(text)
                    .buildCompletionRequest();
        });
    }

    @NotNull
    protected AnAction customEdit(CharSequence computerLanguage, CharSequence instruction) {
        return TextReplacementAction.create(instruction, instruction, null, (event, string) -> {
            AppSettingsState settings = AppSettingsState.getInstance();
            settings.addInstructionToHistory(instruction);
            return settings.createTranslationRequest()
                    .setInputType(computerLanguage)
                    .setOutputType(computerLanguage)
                    .setInstruction(instruction)
                    .setInputAttribute("type", "before")
                    .setOutputAttrute("type", "after")
                    .setInputText(IndentedText.fromString(string).getTextBlock())
                    .buildCompletionRequest();
        });
    }

    @Nullable
    protected AnAction markdownListAction(@NotNull AnActionEvent e) {
        try {
            Caret caret = e.getData(CommonDataKeys.CARET);
            if (null == caret) return null;
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if (null == psiFile) return null;
            PsiElement list = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownListImpl");
            if (null == list) return null;
            return new AnAction("Add _List Items", "Add list items", null) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    AppSettingsState settings = AppSettingsState.getInstance();
                    List<CharSequence> items = StringTools.trim(PsiUtil.getAll(list, "MarkdownListItemImpl")
                            .stream().map(item -> PsiUtil.getAll(item, "MarkdownParagraphImpl").get(0).getText()).collect(Collectors.toList()), 10, false);
                    CharSequence indent = UITools.getIndent(caret);
                    CharSequence n = Integer.toString(items.size() * 2);
                    int endOffset = list.getTextRange().getEndOffset();
                    String listPrefix = "* ";
                    CompletionRequest completionRequest = settings.createTranslationRequest()
                            .setInstruction(UITools.getInstruction("List " + n + " items"))
                            .setInputType("instruction")
                            .setInputText("List " + n + " items")
                            .setOutputType("list")
                            .setOutputAttrute("style", settings.style)
                            .buildCompletionRequest()
                            .appendPrompt(items.stream().map(x2 -> listPrefix + x2).collect(Collectors.joining("\n")) + "\n" + listPrefix);
                    UITools.redoableRequest(completionRequest, "", event, complete -> {
                        List<CharSequence> newItems = Arrays.stream(complete.toString().split("\n")).map(String::trim)
                                .filter(x1 -> x1 != null && x1.length() > 0).map(x1 -> StringTools.stripPrefix(x1, listPrefix)).collect(Collectors.toList());
                        String strippedList = Arrays.stream(list.getText().split("\n"))
                                .map(String::trim).filter(x -> x.length() > 0).collect(Collectors.joining("\n"));
                        String bulletString = Stream.of("- [ ] ", "- ", "* ")
                                .filter(strippedList::startsWith).findFirst().orElse("1. ");
                        CharSequence itemText = indent + newItems.stream().map(x -> bulletString + x)
                                .collect(Collectors.joining("\n" + indent));
                        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                        return UITools.insertString(editor.getDocument(), endOffset, "\n" + itemText);
                    });
                }
            };
        } catch (Exception ex) {
            log.error(ex);
            return null;
        }
    }

    /**
     * This method creates an action to add new columns to a Markdown table.
     *
     * @param e The action event
     * @return An action to add new columns to a Markdown table, or null if the action cannot be created
     */
    @Nullable
    protected AnAction markdownNewTableColsAction(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        PsiElement table = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownTableImpl");
        if (null == table) return null;
        List<CharSequence> rows = Arrays.asList(StringTools.transposeMarkdownTable(PsiUtil.getAll(table, "MarkdownTableRowImpl")
                .stream().map(PsiElement::getText).collect(Collectors.joining("\n")), false, false).split("\n"));
        CharSequence n = Integer.toString(rows.size() * 2);
        return new AnAction("Add _Table Columns", "Add table columns", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                CharSequence originalText = table.getText();
                AppSettingsState settings = AppSettingsState.getInstance();
                CharSequence indent = UITools.getIndent(caret);
                UITools.redoableRequest(newRowsRequest(settings, n, rows, ""),
                        "",
                        event,
                        (CharSequence complete) -> {
                            List<CharSequence> newRows = Arrays.stream(("" + complete).split("\n")).map(String::trim)
                                    .filter(x -> x.length() > 0).collect(Collectors.toList());
                            String newTableTxt = StringTools.transposeMarkdownTable(Stream.concat(rows.stream(), newRows.stream())
                                    .collect(Collectors.joining("\n")), false, true);
                            final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                            return UITools.replaceString(
                                    editor.getDocument(),
                                    table.getTextRange().getStartOffset(),
                                    table.getTextRange().getEndOffset(),
                                    newTableTxt.replace("\n", "\n" + indent));
                        });
            }
        };
    }

    /**
     * Creates an action to add a new column to a Markdown table.
     *
     * @param e The action event.
     * @return An action to add a new column to a Markdown table, or null if the action cannot be created.
     */
    @Nullable
    protected AnAction markdownNewTableColAction(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        PsiElement table = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownTableImpl");
        if (null == table) return null;
        List<CharSequence> rows = Arrays.asList(StringTools.transposeMarkdownTable(PsiUtil.getAll(table, "MarkdownTableRowImpl")
                .stream().map(PsiElement::getText).collect(Collectors.joining("\n")), false, false).split("\n"));
        CharSequence n = Integer.toString(rows.size() * 2);
        return new AnAction("Add Table _Column...", "Add table column...", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                AppSettingsState settings = AppSettingsState.getInstance();
                CharSequence indent = UITools.getIndent(caret);
                CharSequence columnName = JOptionPane.showInputDialog(null, "Column Name:", "Add Column", JOptionPane.QUESTION_MESSAGE);
                UITools.redoableRequest(
                        newRowsRequest(settings, n, rows, "| " + columnName + " | "),
                        "",
                        event,
                        (CharSequence complete) -> {
                            List<CharSequence> newRows = Arrays.stream(("" + complete).split("\n"))
                                    .map(String::trim).filter(x -> x.length() > 0).collect(Collectors.toList());
                            String newTableTxt = StringTools.transposeMarkdownTable(Stream.concat(rows.stream(),
                                    newRows.stream()).collect(Collectors.joining("\n")), false, true);
                            final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                            return UITools.replaceString(
                                    editor.getDocument(),
                                    table.getTextRange().getStartOffset(),
                                    table.getTextRange().getEndOffset(),
                                    newTableTxt.replace("\n", "\n" + indent));
                        });
            }
        };
    }

    @Nullable
    protected AnAction markdownNewTableRowsAction(@NotNull AnActionEvent e) {
        Caret caret = e.getData(CommonDataKeys.CARET);
        if (null == caret) return null;
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (null == psiFile) return null;
        PsiElement table = PsiUtil.getSmallestIntersecting(psiFile, caret.getSelectionStart(), caret.getSelectionEnd(), "MarkdownTableImpl");
        if (null == table) return null;
        if (null != table) {
            List<CharSequence> rows = StringTools.trim(PsiUtil.getAll(table, "MarkdownTableRowImpl")
                    .stream().map(PsiElement::getText).collect(Collectors.toList()), 10, true);
            CharSequence n = Integer.toString(rows.size() * 2);
            return new AnAction("Add _Table Rows", "Add table rows", null) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    AppSettingsState settings = AppSettingsState.getInstance();
                    CharSequence indent = UITools.getIndent(caret);
                    UITools.redoableRequest(newRowsRequest(settings, n, rows, ""),
                            "",
                            event,
                            (CharSequence complete) -> {
                                List<CharSequence> newRows = Arrays.stream(("" + complete).split("\n"))
                                        .map(String::trim).filter(x -> x.length() > 0).collect(Collectors.toList());
                                CharSequence itemText = indent + newRows.stream().collect(Collectors.joining("\n" + indent));
                                final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
                                return UITools.insertString(editor.getDocument(), table.getTextRange().getEndOffset(), "\n" + itemText);
                            });
                }
            };
        }
        return null;
    }

    @NotNull
    protected CompletionRequest newRowsRequest(AppSettingsState settings, CharSequence n, List<CharSequence> rows, CharSequence rowPrefix) {
        return settings.createTranslationRequest()
                .setInstruction(UITools.getInstruction("List " + n + " items"))
                .setInputType("instruction")
                .setInputText("List " + n + " items")
                .setOutputType("markdown")
                .setOutputAttrute("style", settings.style)
                .buildCompletionRequest()
                .appendPrompt("\n" + String.join("\n", rows) + "\n" + rowPrefix);
    }

    /**
     * Creates a {@link TextReplacementAction} for the given {@link AnActionEvent} and human language.
     *
     * @param e             the action event
     * @param humanLanguage the human language
     * @return the {@link TextReplacementAction} or {@code null} if no action can be created
     */
    @Nullable
    protected AnAction markdownContextAction(@NotNull AnActionEvent e, CharSequence humanLanguage) {
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
                            .setInstruction(UITools.getInstruction(String.format("Using Markdown and %s", humanLanguage)))
                            .setInputType("instruction")
                            .setInputText(humanDescription)
                            .setOutputAttrute("type", "document")
                            .setOutputAttrute("style", settings.style)
                            .buildCompletionRequest()
                            .appendPrompt(context);
                });
            }
        }
        return null;
    }

    public static void addIfNotNull(@NotNull ArrayList<AnAction> children, AnAction action) {
        if (null != action) children.add(action);
    }

    @Nullable
    protected AnAction printTreeAction(@NotNull AnActionEvent e) {
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
    protected AnAction rewordCommentAction(@NotNull AnActionEvent e, ComputerLanguage computerLanguage, String humanLanguage) {
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
                String text = largestIntersectingComment.getText();
                TextBlockFactory<?> commentModel = computerLanguage.getCommentModel(text);
                String commentText = commentModel.fromString(text.trim()).stream()
                        .map(Object::toString)
                        .map(String::trim)
                        .filter(x -> !x.isEmpty())
                        .reduce((a, b) -> a + "\n" + b).get();
                int startOffset = largestIntersectingComment.getTextRange().getStartOffset();
                int endOffset = largestIntersectingComment.getTextRange().getEndOffset();
                CharSequence indent = UITools.getIndent(caret);
                UITools.redoableRequest(settings.createTranslationRequest()
                        .setInstruction(UITools.getInstruction("Reword"))
                        .setInputText(commentText)
                        .setInputType(humanLanguage)
                        .setOutputAttrute("type", "input")
                        .setOutputType(humanLanguage)
                        .setOutputAttrute("type", "output")
                        .setOutputAttrute("style", settings.style)
                        .buildCompletionRequest(), "", e1, (CharSequence result) -> {
                    String lineWrapping = StringTools.lineWrapping(result, 120);
                    CharSequence finalResult = indent.toString() + commentModel.fromString(lineWrapping).withIndent(indent);
                    return UITools.replaceString(editor.getDocument(), startOffset, endOffset, finalResult);
                });
            }

        };
    }

    @Nullable
    protected AnAction psiClassContextAction(@NotNull AnActionEvent e, ComputerLanguage computerLanguage, String humanLanguage) {
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
                        .map(Object::toString)
                        .map(String::trim)
                        .filter(x -> !x.isEmpty())
                        .reduce((a, b) -> a + " " + b).get();
                int endOffset = largestIntersectingComment.getTextRange().getEndOffset();
                UITools.redoableRequest(settings.createTranslationRequest()
                                .setInstruction("Implement " + humanLanguage + " as " + computerLanguage.name() + " code")
                                .setInputType(humanLanguage)
                                .setInputAttribute("type", "instruction")
                                .setInputText(specification)
                                .setOutputType(computerLanguage.name())
                                .setOutputAttrute("type", "code")
                                .setOutputAttrute("style", settings.style)
                                .buildCompletionRequest()
                                .appendPrompt(PsiClassContext.getContext(psiFile, selectionStart, selectionEnd) + "\n"),
                        UITools.getIndent(caret),
                        e1,
                        (CharSequence result) -> UITools.insertString(editor.getDocument(), endOffset, "\n" + result));
            }
        };
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
        super.update(e);
    }
}