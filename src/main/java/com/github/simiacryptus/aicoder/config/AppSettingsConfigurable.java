package com.github.simiacryptus.aicoder.config;

import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Provides controller functionality for application settings.
 */
public class AppSettingsConfigurable implements Configurable {

    @Nullable AppSettingsComponent settingsComponent;
    private volatile @Nullable JPanel mainPanel = null;

    public AppSettingsConfigurable() {
        // A default constructor with no arguments is required because this implementation is registered as an applicationConfigurable EP
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public @NotNull String getDisplayName() {
        return "AICoder Settings";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return Objects.requireNonNull(settingsComponent).getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (null == mainPanel) {
            synchronized (this) {
                if (null == mainPanel) {
                    @NotNull FormBuilder formBuilder = FormBuilder.createFormBuilder();
                    settingsComponent = new AppSettingsComponent();
                    UITools.INSTANCE.addFields(settingsComponent, formBuilder);
                    mainPanel = formBuilder.addComponentFillVertically(new JPanel(), 0).getPanel();
                }
            }
        }
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        @NotNull AppSettingsState buffer = new AppSettingsState();
        if (this.settingsComponent != null) {
            UITools.INSTANCE.readUI(this.settingsComponent, buffer);
        }
        return !buffer.equals(AppSettingsState.getInstance());
    }

    @Override
    public void apply() {
        if (this.settingsComponent != null) {
            UITools.INSTANCE.readUI(this.settingsComponent, AppSettingsState.getInstance());
        }
    }

    @Override
    public void reset() {
        if (settingsComponent != null) {
            UITools.INSTANCE.writeUI(settingsComponent, AppSettingsState.getInstance());
        }
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }

}