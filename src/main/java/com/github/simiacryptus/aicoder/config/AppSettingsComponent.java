package com.github.simiacryptus.aicoder.config;

import com.github.simiacryptus.aicoder.StyleUtil;
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
    @Name("API Base")
    public final JBTextField apiBase = new JBTextField();
    @Name("API Key")
    public final JBPasswordField apiKey = new JBPasswordField();
    @Name("Model")
    public final JBTextField model = new JBTextField();
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
    @Name("Developer Tools")
    public final JBCheckBox devActions = new JBCheckBox();
    @Name("API Log Level")
    public final ComboBox apiLogLevel = new ComboBox(Arrays.stream(LogLevel.values()).map(x->x.name()).toArray(String[]::new));

//    @Name("API Envelope")
//    public final ComboBox translationRequestTemplate = new ComboBox(Arrays.stream(TranslationRequestTemplate.values()).map(x->x.name()).toArray(String[]::new));

    public @NotNull JComponent getPreferredFocusedComponent() {
        return apiKey;
    }

    public static String queryAPIKey() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter OpenAI API Key:");
        JPasswordField pass = new JPasswordField(100);
        panel.add(label);
        panel.add(pass);
        String[] options = new String[]{"OK", "Cancel"};
        int option = JOptionPane.showOptionDialog(null, panel, "API Key",
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[1]);
        if (option == 0) {
            char[] password = pass.getPassword();
            return new String(password);
        }
        return null;
    }

}