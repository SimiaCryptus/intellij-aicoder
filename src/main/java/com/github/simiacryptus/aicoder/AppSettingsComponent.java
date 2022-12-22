package com.github.simiacryptus.aicoder;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class AppSettingsComponent {

  private final JPanel mainPanel;
  private final JBTextField apiBase = new JBTextField();
  private final JBTextField apiKey = new JBTextField();
  private final JBTextField model = new JBTextField();
  private final JBTextField maxTokens = new JBTextField();
  private final JBTextField temperature = new JBTextField();
  private final JBTextField style = new JBTextField();

  public AppSettingsComponent() {
    mainPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(new JBLabel("API Base: "), apiBase, 1, false)
        .addLabeledComponent(new JBLabel("API Key: "), apiKey, 1, false)
        .addLabeledComponent(new JBLabel("Model: "), model, 1, false)
        .addLabeledComponent(new JBLabel("Max Tokens: "), maxTokens, 1, false)
        .addLabeledComponent(new JBLabel("Temperature: "), temperature, 1, false)
        .addLabeledComponent(new JBLabel("Style: "), style, 1, false)
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();
  }

  void getProperties(AppSettingsState settings) {
    settings.apiBase = apiBase.getText();
    settings.apiKey = apiKey.getText();
    settings.model = model.getText();
    settings.style = style.getText().trim();
    settings.maxTokens = Integer.parseInt(maxTokens.getText());
    settings.temperature = Double.parseDouble(temperature.getText());
  }

  void setProperties(AppSettingsState settings) {
    apiBase.setText(settings.apiBase);
    apiKey.setText(settings.apiKey);
    model.setText(settings.model);
    style.setText(settings.style);
    maxTokens.setText(String.valueOf(settings.maxTokens));
    temperature.setText(String.valueOf(settings.temperature));
  }

  public JPanel getPanel() {
    return mainPanel;
  }

  public JComponent getPreferredFocusedComponent() {
    return apiKey;
  }

}