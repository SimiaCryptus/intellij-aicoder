package com.github.simiacryptus.aicoder.config;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.Field;

public class SimpleSettingsComponent<T> {
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
                if (component instanceof JBTextField) {
                    if (nameAnnotation != null) {
                        formBuilder.addLabeledComponent(new JBLabel(nameAnnotation.value() + ": "), component, 1, false);
                    }
                } else {
                    if (nameAnnotation != null) {
                        formBuilder.addLabeledComponent(new JBLabel(nameAnnotation.value() + ": "), component, 1, false);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return formBuilder.addComponentFillVertically(new JPanel(), 0).getPanel();
    }

    public void getProperties(@NotNull T settings) {
        Field[] thoseFields = settings.getClass().getDeclaredFields();
        for (Field thatField : thoseFields) {
            thatField.setAccessible(true);
            String fieldName = thatField.getName();
            try {
                Object thisValue = null;
                Field thisField = this.getClass().getDeclaredField(fieldName);
                Object thisFieldVal = thisField.get(this);
                switch (thatField.getType().getName()) {
                    case "java.lang.String":
                        if (thisFieldVal instanceof JBTextField) {
                            thisValue = ((JBTextField) thisFieldVal).getText();
                        } else if (thisFieldVal instanceof JBTextArea) {
                            thisValue = ((JBTextArea) thisFieldVal).getText();
                        }
                        break;
                    case "int":
                        if (thisFieldVal instanceof JBTextField) {
                            thisValue = Integer.parseInt(((JBTextField) thisFieldVal).getText());
                        } else if (thisFieldVal instanceof JBTextArea) {
                            thisValue = Integer.parseInt(((JBTextArea) thisFieldVal).getText());
                        }
                        break;
                    case "double":
                        if (thisFieldVal instanceof JBTextField) {
                            thisValue = Double.parseDouble(((JBTextField) thisFieldVal).getText());
                        } else if (thisFieldVal instanceof JBTextArea) {
                            thisValue = Double.parseDouble(((JBTextArea) thisFieldVal).getText());
                        }
                        break;
                    case "boolean":
                        if (thisFieldVal instanceof JBCheckBox) {
                            thisValue = ((JBCheckBox) thisFieldVal).isSelected();
                        } else if (thisFieldVal instanceof JBTextField) {
                            thisValue = Boolean.parseBoolean(((JBTextField) thisFieldVal).getText());
                        } else if (thisFieldVal instanceof JBTextArea) {
                            thisValue = Boolean.parseBoolean(((JBTextArea) thisFieldVal).getText());
                        }
                        break;
                }
                thatField.set(settings, thisValue);
            } catch (Throwable e) {
                if (verbose) new RuntimeException("Error processing " + thatField, e).printStackTrace();
            }
        }
    }

    public void setProperties(@NotNull T settings) {
        Field[] thoseFields = settings.getClass().getDeclaredFields();
        for (Field thatField : thoseFields) {
            thatField.setAccessible(true);
            String fieldName = thatField.getName();
            try {
                Field thisField = this.getClass().getDeclaredField(fieldName);
                Object thatFieldVal = thatField.get(settings);
                Object thisFieldVal = thisField.get(this);
                switch (thatField.getType().getName()) {
                    case "java.lang.String":
                        if (thisFieldVal instanceof JBTextField) {
                            ((JBTextField) thisFieldVal).setText((String) thatFieldVal);
                        } else if (thisFieldVal instanceof JBTextArea) {
                            ((JBTextArea) thisFieldVal).setText((String) thatFieldVal);
                        }
                        break;
                    case "int":
                        if (thisFieldVal instanceof JBTextField) {
                            ((JBTextField) thisFieldVal).setText(Integer.toString((Integer) thatFieldVal));
                        } else if (thisFieldVal instanceof JBTextArea) {
                            ((JBTextArea) thisFieldVal).setText(Integer.toString((Integer) thatFieldVal));
                        }
                        break;
                    case "double":
                        if (thisFieldVal instanceof JBTextField) {
                            ((JBTextField) thisFieldVal).setText(Double.toString(((Double) thatFieldVal)));
                        } else if (thisFieldVal instanceof JBTextArea) {
                            ((JBTextArea) thisFieldVal).setText(Double.toString(((Double) thatFieldVal)));
                        }
                        break;
                    case "boolean":
                        if (thisFieldVal instanceof JBCheckBox) {
                            ((JBCheckBox) thisFieldVal).setSelected(((Boolean) thatFieldVal));
                        } else if (thisFieldVal instanceof JBTextField) {
                            ((JBTextField) thisFieldVal).setText(Boolean.toString((Boolean) thatFieldVal));
                        } else if (thisFieldVal instanceof JBTextArea) {
                            ((JBTextArea) thisFieldVal).setText(Boolean.toString((Boolean) thatFieldVal));
                        }
                        break;
                }
            } catch (Throwable e) {
                if (verbose) new RuntimeException("Error processing " + thatField, e).printStackTrace();
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
