package com.github.simiacryptus.aicoder.openai;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.config.Name;
import com.github.simiacryptus.aicoder.util.UITools;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class InteractiveRequest {
    @SuppressWarnings("unused")
    @Name("Prompt")
    public final JBTextArea prompt = new JBTextArea(10, 40);
    @SuppressWarnings("unused")
    @Name("Suffix")
    public final JBTextArea suffix = new JBTextArea(2, 40);
    @SuppressWarnings("unused")
    @Name("Model")
    public final JComponent model = OpenAI_API.INSTANCE.getModelSelector();
    @SuppressWarnings("unused")
    @Name("Temperature")
    public final JBTextField temperature = new JBTextField(8);
    @SuppressWarnings("unused")
    @Name("Max Tokens")
    public final JBTextField max_tokens = new JBTextField(8);
    public final @NotNull JButton testRequest;

    public InteractiveRequest(@NotNull CompletionRequest parent) {
        testRequest = new JButton(new AbstractAction("Test Request") {
            @Override
            public void actionPerformed(ActionEvent e) {
                CompletionRequest.CompletionRequestWithModel withModel = new CompletionRequest.CompletionRequestWithModel(parent, AppSettingsState.getInstance().model);
                UITools.readUI(InteractiveRequest.this, withModel);
                ListenableFuture<CharSequence> future = OpenAI_API.INSTANCE.complete(null, withModel, "");
                testRequest.setEnabled(false);
                Futures.addCallback(future, new FutureCallback<>() {
                    @Override
                    public void onSuccess(@NotNull CharSequence result) {
                        testRequest.setEnabled(true);
                        String text = result.toString();
                        int rows = Math.min(50, text.split("\n").length);
                        int columns = Math.min(200, Arrays.stream(text.split("\n")).mapToInt(String::length).max().getAsInt());
                        JBTextArea area = new JBTextArea(rows, columns);
                        area.setText(text);
                        area.setEditable(false);
                        JOptionPane.showMessageDialog(null, area, "Test Output", JOptionPane.PLAIN_MESSAGE);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        testRequest.setEnabled(true);
                        UITools.handle(t);
                    }
                }, OpenAI_API.INSTANCE.pool);
            }
        });
    }
}
