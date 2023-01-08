package com.github.simiacryptus.aicoder.config;

import com.github.simiacryptus.aicoder.AICoderMainMenu;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.lang.reflect.Field;

public class SimpleSettingsComponent<T> {
    private static final Logger log = Logger.getInstance(SimpleSettingsComponent.class);
    protected final boolean verbose = false;
    private volatile @Nullable JPanel mainPanel = null;

    /**
     * Builds the main panel for the form.
     *
     * @return the main panel for the form
     */
    private JPanel buildMainPanel() {
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        for (Field field : this.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Name nameAnnotation = field.getDeclaredAnnotation(Name.class);
                JComponent component = (JComponent) field.get(this);
                if (null == component) continue;
                if (nameAnnotation != null) {
                    formBuilder.addLabeledComponent(new JBLabel(nameAnnotation.value() + ": "), component, 1, false);
                } else {
                    formBuilder.addComponentToRightColumn(component, 1);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (Throwable e) {
                log.warn("Error processing " + field.getName(), e);
            }
        }
        return formBuilder.addComponentFillVertically(new JPanel(), 0).getPanel();
    }

    public void getProperties(@NotNull T settings) {
        for (Field settingsField : settings.getClass().getDeclaredFields()) {
            settingsField.setAccessible(true);
            String settingsFieldName = settingsField.getName();
            try {
                Object newSettingsValue = null;
                Field uiField = this.getClass().getDeclaredField(settingsFieldName);
                Object uiFieldVal = uiField.get(this);
                switch (settingsField.getType().getName()) {
                    case "java.lang.String":
                        if (uiFieldVal instanceof JTextComponent) {
                            newSettingsValue = ((JTextComponent) uiFieldVal).getText();
                        } else if (uiFieldVal instanceof ComboBox) {
                            newSettingsValue = ((ComboBox<String>) uiFieldVal).getItem();
                        }
                        break;
                    case "int":
                        if (uiFieldVal instanceof JTextComponent) {
                            newSettingsValue = Integer.parseInt(((JTextComponent) uiFieldVal).getText());
                        }
                        break;
                    case "double":
                        if (uiFieldVal instanceof JTextComponent) {
                            newSettingsValue = Double.parseDouble(((JTextComponent) uiFieldVal).getText());
                        }
                        break;
                    case "boolean":
                        if (uiFieldVal instanceof JCheckBox) {
                            newSettingsValue = ((JCheckBox) uiFieldVal).isSelected();
                        } else if (uiFieldVal instanceof JTextComponent) {
                            newSettingsValue = Boolean.parseBoolean(((JTextComponent) uiFieldVal).getText());
                        }
                        break;
                    default:

                        if (java.lang.Enum.class.isAssignableFrom(settingsField.getType())) {
                            if (uiFieldVal instanceof ComboBox) {
                                ComboBox<String> comboBox = (ComboBox<String>) uiFieldVal;
                                String item = comboBox.getItem();
                                newSettingsValue = Enum.valueOf((Class<? extends Enum>) settingsField.getType(), item);
                            }
                        }
                        break;
                }
                settingsField.set(settings, newSettingsValue);
            } catch (Throwable e) {
                if (verbose) new RuntimeException("Error processing " + settingsField, e).printStackTrace();
            }
        }
    }

    public void setProperties(@NotNull T settings) {
        for (Field settingsField : settings.getClass().getDeclaredFields()) {
            settingsField.setAccessible(true);
            String fieldName = settingsField.getName();
            try {
                Field uiField = this.getClass().getDeclaredField(fieldName);
                Object settingsVal = settingsField.get(settings);
                if(null == settingsVal) continue;
                Object uiVal = uiField.get(this);
                switch (settingsField.getType().getName()) {
                    case "java.lang.String":
                        if (uiVal instanceof JTextComponent) {
                            ((JTextComponent) uiVal).setText((String) settingsVal);
                        } else if (uiVal instanceof ComboBox) {
                            ((ComboBox<String>) uiVal).setItem(settingsVal.toString());
                        }
                        break;
                    case "int":
                        if (uiVal instanceof JTextComponent) {
                            ((JTextComponent) uiVal).setText(Integer.toString((Integer) settingsVal));
                        }
                        break;
                    case "double":
                        if (uiVal instanceof JTextComponent) {
                            ((JTextComponent) uiVal).setText(Double.toString(((Double) settingsVal)));
                        }
                        break;
                    case "boolean":
                        if (uiVal instanceof JCheckBox) {
                            ((JCheckBox) uiVal).setSelected(((Boolean) settingsVal));
                        } else if (uiVal instanceof JTextComponent) {
                            ((JTextComponent) uiVal).setText(Boolean.toString((Boolean) settingsVal));
                        }
                        break;
                    default:
                        if (uiVal instanceof ComboBox) {
                            ((ComboBox<String>) uiVal).setItem(settingsVal.toString());
                        }
                        break;
                }
            } catch (Throwable e) {
                if (verbose) new RuntimeException("Error processing " + settingsField, e).printStackTrace();
            }
        }
    }

    public JPanel getPanel() {
        if (null == mainPanel) {
            synchronized (this) {
                if (null == mainPanel) {
                    mainPanel = buildMainPanel();
                }
            }
        }
        return mainPanel;
    }
}
