package com.github.simiacryptus.aicoder;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides controller functionality for application settings.
 */
public class AppSettingsConfigurable implements Configurable {

  AppSettingsComponent settingsComponent;

  public AppSettingsConfigurable() {
    // A default constructor with no arguments is required because this implementation is registered as an applicationConfigurable EP
  }

  @Nls(capitalization = Nls.Capitalization.Title)
  @Override
  public String getDisplayName() {
    return "AICoder Settings";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return settingsComponent.getPreferredFocusedComponent();
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    settingsComponent = new AppSettingsComponent();
    return settingsComponent.getPanel();
  }

  @Override
  public boolean isModified() {
    AppSettingsState buffer = new AppSettingsState();
    this.settingsComponent.getProperties(buffer);
    return !buffer.equals(AppSettingsState.getInstance());
  }

  @Override
  public void apply() {
    this.settingsComponent.getProperties(AppSettingsState.getInstance());
  }

  @Override
  public void reset() {
    settingsComponent.setProperties(AppSettingsState.getInstance());
  }

  @Override
  public void disposeUIResources() {
    settingsComponent = null;
  }

}