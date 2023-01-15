package com.github.simiacryptus.aicoder.openai;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class LogProbs {
    @SuppressWarnings("unused")
    public CharSequence[] tokens;
    @SuppressWarnings("unused")
    public double[] token_logprobs;
    @SuppressWarnings("unused")
    public ObjectNode[] top_logprobs;
    @SuppressWarnings("unused")
    public int[] text_offset;

    @SuppressWarnings("unused")
    public LogProbs() {
    }

    @SuppressWarnings("unused")
    public LogProbs(CharSequence[] tokens, double[] token_logprobs, ObjectNode[] top_logprobs, int[] text_offset) {
        this.tokens = tokens;
        this.token_logprobs = token_logprobs;
        this.top_logprobs = top_logprobs;
        this.text_offset = text_offset;
    }

}
