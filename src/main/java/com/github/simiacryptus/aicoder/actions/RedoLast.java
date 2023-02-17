package com.github.simiacryptus.aicoder.actions;

import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import org.jetbrains.annotations.NotNull;


/**
 * The RedoLast action is an IntelliJ action that allows users to redo the last AI Coder action they performed in the editor.
 * To use this action, open the editor and select the RedoLast action from the editor context menu.
 * This will redo the last action that was performed in the editor.
 */
public class RedoLast extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
        super.update(e);
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        return null != UITools.INSTANCE.getRetry().get(e.getRequiredData(CommonDataKeys.EDITOR).getDocument());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        UITools.INSTANCE.getRetry().get(e.getRequiredData(CommonDataKeys.EDITOR).getDocument()).run();
    }
}
