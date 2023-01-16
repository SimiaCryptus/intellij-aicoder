package com.github.simiacryptus.aicoder.openai;

import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.util.UITools;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The CompletionRequest class is used to create a request for completion of a given prompt.
 */
public class CompletionRequest {

    public CompletionRequest(AppSettingsState config) {
        this("",config.temperature,config.maxTokens,null);
    }

    @NotNull
    public CompletionRequest.CompletionRequestWithModel uiIntercept() {
        CompletionRequestWithModel withModel;
        if (!(this instanceof CompletionRequestWithModel)) {
            if (!AppSettingsState.getInstance().devActions) {
                withModel = new CompletionRequestWithModel(this, AppSettingsState.getInstance().model_completion);
            } else {
                withModel = showModelEditDialog();
            }
        } else {
            withModel = (CompletionRequestWithModel) this;
        }
        return withModel;
    }

    public static class CompletionRequestWithModel extends CompletionRequest {

        public String model;

        public CompletionRequestWithModel(String prompt, double temperature, int max_tokens, Integer logprobs, boolean echo, String model, CharSequence... stop) {
            super(prompt, temperature, max_tokens, logprobs, stop);
            this.model = model;
        }

        public CompletionRequestWithModel(CompletionRequestWithModel other) {
            super(other);
            this.model = other.model;
        }

        public CompletionRequestWithModel(CompletionRequest other, String model) {
            super(other);
            this.model = model;
        }

        public void fixup(AppSettingsState settings) {
            if (null != this.suffix) {
                if (this.suffix.trim().length() == 0) {
                    setSuffix(null);
                } else {
                    this.echo = false;
                }
            }
            if (null != this.stop && this.stop.length == 0) {
                this.stop = null;
            }
            if (this.prompt.length() > settings.maxPrompt)
                throw new IllegalArgumentException("Prompt too long:" + this.prompt.length() + " chars");
        }
    }
    public String prompt;
    public @Nullable String suffix = null;
    @SuppressWarnings("unused")
    public double temperature;
    @SuppressWarnings("unused")
    public int max_tokens;
    public CharSequence @Nullable [] stop;
    @SuppressWarnings("unused")
    public Integer logprobs;
    @SuppressWarnings("unused")
    public boolean echo;

    @SuppressWarnings("unused")
    public CompletionRequest() {
    }

    public CompletionRequest(String prompt, double temperature, int max_tokens, Integer logprobs, CharSequence... stop) {
        this.prompt = prompt;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.stop = stop;
        this.logprobs = logprobs;
        this.echo = false;
    }
    public CompletionRequest(CompletionRequest other) {
        this.prompt = other.prompt;
        this.temperature = other.temperature;
        this.max_tokens = other.max_tokens;
        this.stop = other.stop;
        this.logprobs = other.logprobs;
        this.echo = other.echo;
    }


    public @NotNull CompletionRequest appendPrompt(CharSequence prompt) {
        this.prompt = this.prompt + prompt;
        return this;
    }

    public @NotNull CompletionRequest addStops(@NotNull CharSequence @NotNull ... newStops) {
        ArrayList<CharSequence> stops = new ArrayList<>();
        for (CharSequence x : newStops) {
            if (x.length() > 0) {
                stops.add(x);
            }
        }
        if (!stops.isEmpty()) {
            if (null != this.stop) Arrays.stream(this.stop).forEach(stops::add);
            this.stop = stops.stream().distinct().toArray(CharSequence[]::new);
        }
        return this;
    }

    public @NotNull CompletionRequest setSuffix(@Nullable CharSequence suffix) {
        this.suffix = null==suffix?null:suffix.toString();
        return this;
    }

    public @NotNull CompletionRequestWithModel showModelEditDialog() {
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        CompletionRequestWithModel withModel = new CompletionRequestWithModel(this, AppSettingsState.getInstance().model_completion);
        InteractiveCompletionRequest ui = new InteractiveCompletionRequest(withModel);
        UITools.addFields(ui, formBuilder);
        JPanel mainPanel = formBuilder.addComponentFillVertically(new JPanel(), 0).getPanel();
        UITools.writeUI(ui, withModel);
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
}
