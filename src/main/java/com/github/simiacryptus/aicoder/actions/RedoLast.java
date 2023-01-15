package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import org.jetbrains.annotations.NotNull;

public class RedoLast extends AnAction {

    public RedoLast() {
        super("_Redo Last", "Redo last", null);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        return null != UITools.retry.get(e.getRequiredData(CommonDataKeys.EDITOR).getDocument());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        UITools.retry.get(e.getRequiredData(CommonDataKeys.EDITOR).getDocument()).run();
    }
}
