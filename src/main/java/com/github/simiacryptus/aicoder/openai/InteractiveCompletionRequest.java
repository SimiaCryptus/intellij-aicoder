package com.github.simiacryptus.aicoder.openai;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.config.Name;
import com.github.simiacryptus.aicoder.util.UITools;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class InteractiveCompletionRequest {
    @SuppressWarnings("unused")
    @Name("Prompt")
    public final JBScrollPane prompt;
    @SuppressWarnings("unused")
    @Name("Suffix")
    public final JBTextArea suffix;
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

    public InteractiveCompletionRequest(@NotNull CompletionRequest parent) {
        testRequest = new JButton(new AbstractAction("Test Request") {
            @Override
            public void actionPerformed(ActionEvent e) {
                CompletionRequest.@NotNull CompletionRequestWithModel withModel = new CompletionRequest.CompletionRequestWithModel(parent, AppSettingsState.getInstance().model_completion);
                UITools.INSTANCE.readUI(InteractiveCompletionRequest.this, withModel);
                @NotNull ListenableFuture<CharSequence> future = OpenAI_API.INSTANCE.complete(null, withModel, "");
                testRequest.setEnabled(false);
                Futures.addCallback(future, new FutureCallback<>() {
                    @Override
                    public void onSuccess(@NotNull CharSequence result) {
                        testRequest.setEnabled(true);
                        @NotNull String text = result.toString();
                        int rows = Math.min(50, text.split("\n").length);
                        int columns = Math.min(200, Arrays.stream(text.split("\n")).mapToInt(String::length).max().getAsInt());
                        @NotNull JBTextArea area = new JBTextArea(rows, columns);
                        area.setText(text);
                        area.setEditable(false);
                        JOptionPane.showMessageDialog(null, area, "Test Output", JOptionPane.PLAIN_MESSAGE);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        testRequest.setEnabled(true);
                        UITools.INSTANCE.handle(t);
                    }
                }, OpenAI_API.getPool());
            }
        });
        suffix = UITools.INSTANCE.configureTextArea(new JBTextArea(1, 120));
        prompt = UITools.INSTANCE.wrapScrollPane((new JBTextArea(10, 120)));
    }

}
