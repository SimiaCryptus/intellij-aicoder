package com.github.simiacryptus.aicoder.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.simiacryptus.aicoder.util.StyleUtil;
import com.github.simiacryptus.aicoder.openai.OpenAI_API;
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

public class AppSettingsComponent extends SimpleSettingsComponent<AppSettingsState> {
    private static final Logger log = Logger.getInstance(AppSettingsComponent.class);

    @Name("API Base")
    public final JBTextField apiBase = new JBTextField();
    @Name("API Key")
    public final JBPasswordField apiKey = new JBPasswordField();
    @Name("Model")
    public final JComponent model = getModelSelector();

    @NotNull
    private static JComponent getModelSelector() {
        AppSettingsState settings = AppSettingsState.getInstance();
        CharSequence apiKey = settings.apiKey;
        if (null != apiKey && apiKey.toString().trim().length() > 0) {
            try {
                ComboBox<CharSequence> comboBox = new ComboBox<>(new CharSequence[]{settings.model});
                OpenAI_API.onSuccess(OpenAI_API.INSTANCE.getEngines(), engines -> {
                    JsonNode data = engines.get("data");
                    CharSequence[] items = new CharSequence[data.size()];
                    for (int i = 0; i < data.size(); i++) {
                        items[i] = data.get(i).get("id").asText();
                    }
                    Arrays.sort(items);
                    Arrays.stream(items).forEach(comboBox::addItem);
                });
                return comboBox;
            } catch (Throwable e) {
                log.warn(e);
            }
        }
        return new JBTextField();
    }

    @Name("Style")
    public final JBTextField style = new JBTextField();
    @Name("Human Language")
    public final JBTextField humanLanguage = new JBTextField();
    @Name("Max Prompt (Characters)")
    public final JBTextField maxPrompt = new JBTextField();
    @Name("Max Tokens")
    public final JBTextField maxTokens = new JBTextField();
    @Name("History Limit")
    public final JBTextField historyLimit = new JBTextField();
    @Name("Temperature")
    public final JBTextField temperature = new JBTextField();
    public final JButton randomizeStyle = new JButton(new AbstractAction("Randomize Style") {
        @Override
        public void actionPerformed(ActionEvent e) {
            style.setText(StyleUtil.randomStyle());
        }
    });
    public final JButton testStyle = new JButton(new AbstractAction("Test Style") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StyleUtil.demoStyle(style.getText());
        }
    });
    @Name("Token Counter")
    public final JBTextField tokenCounter = new JBTextField();
    public final JButton clearCounter = new JButton(new AbstractAction("Clear Token Counter") {
        @Override
        public void actionPerformed(ActionEvent e) {
            tokenCounter.setText("0");
        }
    });
    @Name("Developer Tools")
    public final JBCheckBox devActions = new JBCheckBox();
    @Name("API Log Level")
    public final ComboBox apiLogLevel = new ComboBox(Arrays.stream(LogLevel.values()).map(x -> x.name()).toArray(CharSequence[]::new));

//    @Name("API Envelope")
//    public final ComboBox translationRequestTemplate = new ComboBox(Arrays.stream(TranslationRequestTemplate.values()).map(x->x.name()).toArray(String[]::new));

    public AppSettingsComponent() {
        tokenCounter.setEditable(false);
    }

    public @NotNull JComponent getPreferredFocusedComponent() {
        return apiKey;
    }

}