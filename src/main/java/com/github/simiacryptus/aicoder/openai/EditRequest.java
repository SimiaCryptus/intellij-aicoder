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

    public EditRequest(@NotNull AppSettingsState settingsState) {
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

    public EditRequest(@NotNull EditRequest obj) {
        this.model = obj.model;
        this.top_p = obj.top_p;
        this.input = obj.input;
        this.instruction = obj.instruction;
        this.temperature = obj.temperature;
        this.n = obj.n;
    }

    public @NotNull EditRequest setModel(@NotNull String model) {
        this.model = model;
        return this;
    }

    public @NotNull EditRequest setInput(String input) {
        this.input = input;
        return this;
    }

    public @NotNull EditRequest setInstruction(@NotNull String instruction) {
        this.instruction = instruction;
        return this;
    }

    public @NotNull EditRequest setTemperature(Double temperature) {
        this.top_p = null;
        this.temperature = temperature;
        return this;
    }

    public @NotNull EditRequest setN(Integer n) {
        this.n = n;
        return this;
    }

    public @NotNull EditRequest setTop_p(Double top_p) {
        this.temperature = null;
        this.top_p = top_p;
        return this;
    }

    @Override
    public @NotNull String toString() {
        @NotNull String sb = "EditRequest{" + "model='" + model + '\'' +
                ", input='" + input + '\'' +
                ", instruction='" + instruction + '\'' +
                ", temperature=" + temperature +
                ", n=" + n +
                ", top_p=" + top_p +
                '}';
        return sb;
    }

    public @NotNull EditRequest showModelEditDialog() {
        @NotNull FormBuilder formBuilder = FormBuilder.createFormBuilder();
        @NotNull EditRequest withModel = new EditRequest(this);
        @NotNull InteractiveEditRequest ui = new InteractiveEditRequest(withModel);
        UITools.addFields(ui, formBuilder);
        UITools.writeUI(ui, withModel);
        JPanel mainPanel = formBuilder.addComponentFillVertically(new JPanel(), 0).getPanel();
        Object @NotNull [] options = {"OK"};
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
