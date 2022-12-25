package com.github.simiacryptus.aicoder.openai;

import java.util.ArrayList;
import java.util.Arrays;

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

    public CompletionRequest appendPrompt(String prompt) {
        this.prompt = this.prompt + prompt;
        return this;
    }

    public CompletionRequest addStops(String[] stop) {
        ArrayList<String> stops = new ArrayList<>();
        Arrays.stream(this.stop).forEach(stops::add);
        Arrays.stream(stop).forEach(stops::add);
        this.stop = stops.stream().distinct().toArray(String[]::new);
        return this;
    }
}
