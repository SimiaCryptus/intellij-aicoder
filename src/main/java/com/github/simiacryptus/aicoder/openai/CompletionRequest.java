package com.github.simiacryptus.aicoder.openai;

import com.github.simiacryptus.aicoder.util.IndentedText;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static com.github.simiacryptus.aicoder.util.StringTools.stripPrefix;
import static com.github.simiacryptus.aicoder.util.StringTools.stripUnbalancedTerminators;

/**
 * The CompletionRequest class is used to create a request for completion of a given prompt.
 */
public class CompletionRequest {
    public String prompt;
    public String suffix = null;
    public double temperature;
    public int max_tokens;
    public CharSequence[] stop;
    public Integer logprobs;
    public boolean echo;

    public CompletionRequest(String prompt, double temperature, int max_tokens, Integer logprobs, boolean echo, CharSequence... stop) {
        this.prompt = prompt;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.stop = stop;
        this.logprobs = logprobs;
        this.echo = echo;
    }

    @NotNull
    public ListenableFuture<CharSequence> complete(@Nullable Project project, CharSequence indent) {
        return OpenAI_API.map(OpenAI_API.INSTANCE.complete(project, this), response -> response
                .getFirstChoice()
                .map(Objects::toString)
                .map(String::trim)
                .map(completion -> stripPrefix(completion, this.prompt.trim()))
                .map(String::trim)
                .map(completion -> stripUnbalancedTerminators(completion))
                .map(IndentedText::fromString)
                .map(indentedText -> indentedText.withIndent(indent))
                .map(IndentedText::toString)
                .map(indentedText -> indent + indentedText)
                .orElse(""));
    }

    public @NotNull CompletionRequest appendPrompt(CharSequence prompt) {
        this.prompt = this.prompt + prompt;
        return this;
    }

    public @NotNull CompletionRequest addStops(@NotNull CharSequence... newStops) {
        ArrayList<CharSequence> stops = new ArrayList<>();
        for (CharSequence x : newStops) {
            if (x != null) {
                if (x.length() > 0) {
                    stops.add(x);
                }
            }
        }
        if (!stops.isEmpty()) {
            if(null != this.stop) Arrays.stream(this.stop).forEach(stops::add);
            this.stop = stops.stream().distinct().toArray(CharSequence[]::new);
        }
        return this;
    }

    public CompletionRequest setSuffix(CharSequence suffix) {
        this.suffix = suffix.toString();
        return this;
    }
}
