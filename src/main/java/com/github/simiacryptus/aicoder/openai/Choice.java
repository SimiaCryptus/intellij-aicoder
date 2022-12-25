package com.github.simiacryptus.aicoder.openai;

public class Choice {
    public String text;
    public int index;
    public LogProbs logprobs;
    public String finish_reason;

    public Choice() {
    }

    public Choice(String text, int index, LogProbs logprobs, String finish_reason) {
        this.text = text;
        this.index = index;
        this.logprobs = logprobs;
        this.finish_reason = finish_reason;
    }

}
