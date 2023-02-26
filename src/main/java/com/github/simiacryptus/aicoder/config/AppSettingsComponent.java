package com.github.simiacryptus.aicoder.config;

import com.github.simiacryptus.aicoder.openai.OpenAI_API;
import com.github.simiacryptus.aicoder.util.StyleUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.jetbrains.rd.util.LogLevel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class AppSettingsComponent {
    private static final Logger log = Logger.getInstance(AppSettingsComponent.class);


    @Name("Style")
    public final JBTextField style = new JBTextField();
    @SuppressWarnings("unused")
    public final JButton randomizeStyle = new JButton(new AbstractAction("Randomize Style") {
        @Override
        public void actionPerformed(ActionEvent e) {
            style.setText(StyleUtil.INSTANCE.randomStyle());
        }
    });

    @SuppressWarnings("unused")
    public final JButton testStyle = new JButton(new AbstractAction("Test Style") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StyleUtil.INSTANCE.demoStyle(style.getText());
        }
    });

    @Name("Token Counter")
    public final JBTextField tokenCounter = new JBTextField();
    @SuppressWarnings("unused")
    public final JButton clearCounter = new JButton(new AbstractAction("Clear Token Counter") {
        @Override
        public void actionPerformed(ActionEvent e) {
            tokenCounter.setText("0");
        }
    });

    @SuppressWarnings("unused")
    @Name("Human Language")
    public final JBTextField humanLanguage = new JBTextField();
    @SuppressWarnings("unused")
    @Name("History Limit")
    public final JBTextField historyLimit = new JBTextField();
    @SuppressWarnings("unused")
    @Name("Developer Tools")
    public final JBCheckBox devActions = new JBCheckBox();
    @SuppressWarnings("unused")
    @Name("Suppress Progress (UNSAFE)")
    public final JBCheckBox suppressProgress = new JBCheckBox();
    @SuppressWarnings("unused")
    @Name("API Log Level")
    public final ComboBox<String> apiLogLevel = new ComboBox<>(Arrays.stream(LogLevel.values()).map(Enum::name).toArray(String[]::new));

    @SuppressWarnings("unused")
    @Name("Temperature")
    public final JBTextField temperature = new JBTextField();
    @SuppressWarnings("unused")
    @Name("Max Tokens")
    public final JBTextField maxTokens = new JBTextField();
    @SuppressWarnings("unused")
    @Name("Max Prompt (Characters)")
    public final JBTextField maxPrompt = new JBTextField();
    @SuppressWarnings("unused")
    @Name("Completion Model")
    public final JComponent model_completion = OpenAI_API.INSTANCE.getModelSelector();
    @Name("Edit Model")
    public final JComponent model_edit = OpenAI_API.INSTANCE.getModelSelector();

    @Name("API Key")
    public final JBPasswordField apiKey = new JBPasswordField();
    @SuppressWarnings("unused")
    @Name("API Base")
    public final JBTextField apiBase = new JBTextField();

    public AppSettingsComponent() {
        tokenCounter.setEditable(false);
    }

    public @NotNull JComponent getPreferredFocusedComponent() {
        return apiKey;
    }

}