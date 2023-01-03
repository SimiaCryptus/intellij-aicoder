package com.github.simiacryptus.aicoder.config;

import com.github.simiacryptus.aicoder.AICoderMainMenu;
import com.intellij.openapi.diagnostic.Logger;
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
                        if (thisFieldVal instanceof JTextComponent) {
                            thisValue = ((JTextComponent) thisFieldVal).getText();
                        }
                        break;
                    case "int":
                        if (thisFieldVal instanceof JTextComponent) {
                            thisValue = Integer.parseInt(((JTextComponent) thisFieldVal).getText());
                        }
                        break;
                    case "double":
                        if (thisFieldVal instanceof JTextComponent) {
                            thisValue = Double.parseDouble(((JTextComponent) thisFieldVal).getText());
                        }
                        break;
                    case "boolean":
                        if (thisFieldVal instanceof JBCheckBox) {
                            thisValue = ((JBCheckBox) thisFieldVal).isSelected();
                        } else if (thisFieldVal instanceof JTextComponent) {
                            thisValue = Boolean.parseBoolean(((JTextComponent) thisFieldVal).getText());
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
                if(null == thatFieldVal) continue;
                Object thisFieldVal = thisField.get(this);
                switch (thatField.getType().getName()) {
                    case "java.lang.String":
                        if (thisFieldVal instanceof JTextComponent) {
                            ((JTextComponent) thisFieldVal).setText((String) thatFieldVal);
                        }
                        break;
                    case "int":
                        if (thisFieldVal instanceof JTextComponent) {
                            ((JTextComponent) thisFieldVal).setText(Integer.toString((Integer) thatFieldVal));
                        }
                        break;
                    case "double":
                        if (thisFieldVal instanceof JTextComponent) {
                            ((JTextComponent) thisFieldVal).setText(Double.toString(((Double) thatFieldVal)));
                        }
                        break;
                    case "boolean":
                        if (thisFieldVal instanceof JBCheckBox) {
                            ((JBCheckBox) thisFieldVal).setSelected(((Boolean) thatFieldVal));
                        } else if (thisFieldVal instanceof JTextComponent) {
                            ((JTextComponent) thisFieldVal).setText(Boolean.toString((Boolean) thatFieldVal));
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
