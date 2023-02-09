package com.github.simiacryptus.aicoder.actions.code;

import com.github.simiacryptus.aicoder.util.ComputerLanguage;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.simiacryptus.aicoder.util.ComputerLanguage.*;
import static com.github.simiacryptus.aicoder.util.ComputerLanguage.Java;


/**
 * The ConvertFileTo ActionGroup provides a way to quickly convert a file from one language to another.
 * It is enabled when the current file is in one of the supported languages,
 * and provides a list of available languages to convert to.
*/
public class ConvertFileTo extends ActionGroup {
    private static final Logger log = Logger.getInstance(ConvertFileTo.class);

    List<ComputerLanguage> supportedLanguages = Arrays.asList(
            Java, JavaScript, Scala, Kotlin, Go, Rust, Python
    );

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ComputerLanguage computerLanguage = getComputerLanguage(e);
        if(null == computerLanguage) return false;
        if(!supportedLanguages.contains(computerLanguage)) return false;
        return true;
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        ComputerLanguage computerLanguage = getComputerLanguage(e);
        ArrayList<AnAction> actions = new ArrayList<>();
        for (ComputerLanguage language : supportedLanguages) {
            if(computerLanguage.equals(language)) continue;
            actions.add(new ConvertFileToLanguage(language));
        }
        return actions.toArray(new AnAction[]{});
    }
}
