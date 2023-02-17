package com.github.simiacryptus.aicoder.actions.markdown;

import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * The ConvertFileTo ActionGroup provides a way to quickly insert code snippets into markdown documents in various languages.
 */
public class MarkdownImplementActionGroup extends ActionGroup {
    private static final Logger log = Logger.getInstance(MarkdownImplementActionGroup.class);

    List<String> markdownLanguages = Arrays.asList(
            "sql",
            "java",
            "asp",
            "c",
            "clojure",
            "coffee",
            "cpp",
            "csharp",
            "css",
            "bash",
            "go",
            "java",
            "javascript",
            "less",
            "make",
            "matlab",
            "objectivec",
            "pascal",
            "PHP",
            "Perl",
            "python",
            "rust",
            "scss",
            "sql",
            "svg",
            "swift",
            "ruby",
            "smalltalk",
            "vhdl"
    );

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(e);
        if (null == computerLanguage) return false;
        if (ComputerLanguage.Markdown != computerLanguage) return false;
        if (!UITools.INSTANCE.hasSelection(e)) return false;
        return true;
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        ComputerLanguage computerLanguage = ComputerLanguage.getComputerLanguage(e);
        ArrayList<AnAction> actions = new ArrayList<>();
        for (String language : markdownLanguages) {
            if (computerLanguage.equals(language)) continue;
            actions.add(new MarkdownImplementAction(language));
        }
        return actions.toArray(new AnAction[]{});
    }
}
