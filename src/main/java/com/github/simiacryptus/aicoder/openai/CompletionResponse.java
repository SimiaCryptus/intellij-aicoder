package com.github.simiacryptus.aicoder.openai;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public class CompletionResponse {
    public String id;
    public String object;
    public int created;
    public String model;
    public Choice[] choices;
    public ApiError error;

    public Usage usage;

    public CompletionResponse() {
    }

    public CompletionResponse(String id, String object, int created, String model, Choice[] choices, ApiError error) {
        this.id = id;
        this.object = object;
        this.created = created;
        this.model = model;
        this.choices = choices;
        this.error = error;
    }

    public @NotNull Optional<String> getFirstChoice() {
        return Optional.ofNullable(this.choices).flatMap(choices -> Arrays.stream(choices).findFirst()).map(choice -> choice.text.trim());
    }
}
