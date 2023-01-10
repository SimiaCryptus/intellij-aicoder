package com.github.simiacryptus.aicoder.openai;

import com.github.simiacryptus.aicoder.text.IndentedText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static com.github.simiacryptus.aicoder.text.StringTools.stripPrefix;
import static com.github.simiacryptus.aicoder.text.StringTools.stripUnbalancedTerminators;

/**
 * The CompletionRequest class is used to create a request for completion of a given prompt.
 */
public class CompletionRequest {
    public String prompt;
    public double temperature;
    public int max_tokens;
    public String[] stop;
    public Integer logprobs;
    public boolean echo;

    public CompletionRequest() {
    }

    public CompletionRequest(String prompt, double temperature, int max_tokens, Integer logprobs, boolean echo, String... stop) {
        this.prompt = prompt;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.stop = stop;
        this.logprobs = logprobs;
        this.echo = echo;
    }

    @NotNull
    public String complete(String indent) throws IOException, ModerationException {
        CompletionResponse response = OpenAI.INSTANCE.complete(this);
        return response
                .getFirstChoice()
                .map(String::trim)
                .map(completion -> stripPrefix(completion, this.prompt.trim()))
                .map(String::trim)
                .map(completion -> stripUnbalancedTerminators(completion))
                .map(IndentedText::fromString)
                .map(indentedText -> indentedText.withIndent(indent))
                .map(IndentedText::toString)
                .map(indentedText -> indent + indentedText)
                .orElse("");
    }

    public @NotNull CompletionRequest appendPrompt(String prompt) {
        this.prompt = this.prompt + prompt;
        return this;
    }

    public @NotNull CompletionRequest addStops(@NotNull String... newStops) {
        ArrayList<String> stops = new ArrayList<>();
        for (String x : newStops) {
            if (x != null) {
                if (!x.isEmpty()) {
                    stops.add(x);
                }
            }
        }
        if (!stops.isEmpty()) {
            Arrays.stream(this.stop).forEach(stops::add);
            this.stop = stops.stream().distinct().toArray(String[]::new);
        }
        return this;
    }
}
