package com.github.simiacryptus.aicoder.openai;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class LogProbs {
  public String[] tokens;
  public double[] token_logprobs;
  public ObjectNode[] top_logprobs;
  public int[] text_offset;

  public LogProbs() {
  }

  public LogProbs(String[] tokens, double[] token_logprobs, ObjectNode[] top_logprobs, int[] text_offset) {
    this.tokens = tokens;
    this.token_logprobs = token_logprobs;
    this.top_logprobs = top_logprobs;
    this.text_offset = text_offset;
  }

}
