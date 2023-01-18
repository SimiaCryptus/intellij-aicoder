package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.util.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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