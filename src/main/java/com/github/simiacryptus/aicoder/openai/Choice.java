package com.github.simiacryptus.aicoder.openai;

public class Choice {
    public String text;
    @SuppressWarnings("unused")
    public int index;
    @SuppressWarnings("unused")
    public LogProbs logprobs;
    @SuppressWarnings("unused")
    public String finish_reason;

    @SuppressWarnings("unused")
    public Choice() {
    }

    @SuppressWarnings("unused")
    public Choice(String text, int index, LogProbs logprobs, String finish_reason) {
        this.text = text;
        this.index = index;
        this.logprobs = logprobs;
        this.finish_reason = finish_reason;
    }

}
