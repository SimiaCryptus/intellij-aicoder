package com.github.simiacryptus.aicoder.actions.dev;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


/**
 * The PrintTreeAction class is an IntelliJ action that enables developers to print the tree structure of a PsiFile.
 * To use this action, first make sure that the "devActions" setting is enabled.
 * Then, open the file you want to print the tree structure of.
 * Finally, select the "PrintTreeAction" action from the editor context menu.
 * This will print the tree structure of the file to the log.
*/
public class PrintTreeAction extends AnAction {
    public static final Logger log = Logger.getInstance(PrintTreeAction.class);

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        if(!AppSettingsState.getInstance().devActions) return false;
        return null != PsiUtil.getPsiFile(e);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e1) {
        log.warn(PsiUtil.printTree(Objects.requireNonNull(PsiUtil.getPsiFile(e1))));
    }

}
