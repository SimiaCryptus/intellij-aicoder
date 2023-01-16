package com.github.simiacryptus.aicoder.openai;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * The CompletionRequest class is used to create a request for completion of a given prompt.
 */
public class EditRequest {

    @NotNull
    public String model;
    @Nullable
    public String input = null;
    @NotNull
    public String instruction;
    @SuppressWarnings("unused")
    @Nullable
    public Double temperature;
    @SuppressWarnings("unused")
    @Nullable
    public Integer n = null;
    @Nullable
    public Double top_p = null;

    @SuppressWarnings("unused")
    public EditRequest() {
    }

    public EditRequest(AppSettingsState settingsState) {
        this.setInstruction("");
        this.setModel(settingsState.model_edit);
        this.setTemperature(settingsState.temperature);
    }

    public EditRequest(@NotNull String instruction) {
        this.setInstruction(instruction);
        this.setModel(AppSettingsState.getInstance().model_edit);
        this.setTemperature(AppSettingsState.getInstance().temperature);
    }

    public EditRequest(@NotNull String instruction, @Nullable String input) {
        this.setInput(input);
        this.setInstruction(instruction);
        this.setModel(AppSettingsState.getInstance().model_edit);
        this.setTemperature(AppSettingsState.getInstance().temperature);
    }

    public EditRequest(@NotNull String model, @Nullable String input, @NotNull String instruction, @Nullable Double temperature) {
        this.setModel(model);
        this.setInput(input);
        this.setInstruction(instruction);
        this.setTemperature(temperature);
    }

    public EditRequest(EditRequest obj) {
        this.model = obj.model;
        this.top_p = obj.top_p;
        this.input = obj.input;
        this.instruction = obj.instruction;
        this.temperature = obj.temperature;
        this.n = obj.n;
    }

    public EditRequest setModel(String model) {
        this.model = model;
        return this;
    }

    public EditRequest setInput(String input) {
        this.input = input;
        return this;
    }

    public EditRequest setInstruction(String instruction) {
        this.instruction = instruction;
        return this;
    }

    public EditRequest setTemperature(Double temperature) {
        this.top_p = null;
        this.temperature = temperature;
        return this;
    }

    public EditRequest setN(Integer n) {
        this.n = n;
        return this;
    }

    public EditRequest setTop_p(Double top_p) {
        this.temperature = null;
        this.top_p = top_p;
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EditRequest{");
        sb.append("model='").append(model).append('\'');
        sb.append(", input='").append(input).append('\'');
        sb.append(", instruction='").append(instruction).append('\'');
        sb.append(", temperature=").append(temperature);
        sb.append(", n=").append(n);
        sb.append(", top_p=").append(top_p);
        sb.append('}');
        return sb.toString();
    }

    public @NotNull EditRequest showModelEditDialog() {
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        EditRequest withModel = new EditRequest(this);
        InteractiveEditRequest ui = new InteractiveEditRequest(withModel);
        UITools.addFields(ui, formBuilder);
        UITools.writeUI(ui, withModel);
        JPanel mainPanel = formBuilder.addComponentFillVertically(new JPanel(), 0).getPanel();
        Object[] options = {"OK"};
        if (JOptionPane.showOptionDialog(
                null,
                mainPanel,
                "OpenAI Completion Request",
                JOptionPane.NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]) == JOptionPane.OK_OPTION) {
            UITools.readUI(ui, withModel);
            return withModel;
        } else {
            return withModel;
        }
    }

    @NotNull
    public EditRequest uiIntercept() {
        if (AppSettingsState.getInstance().devActions) {
            return showModelEditDialog();
        } else {
            return this;
        }
    }
}
