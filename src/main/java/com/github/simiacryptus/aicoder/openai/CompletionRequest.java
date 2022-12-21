package com.github.simiacryptus.aicoder.openai;

public class CompletionRequest {
  public String prompt;
  public double temperature;
  public int max_tokens;
  public String stop;
  public Integer logprobs;
  public boolean echo;

  public CompletionRequest() {
  }

  public CompletionRequest(String prompt, double temperature, int max_tokens, String stop, Integer logprobs, boolean echo) {
    this.prompt = prompt;
    this.temperature = temperature;
    this.max_tokens = max_tokens;
    this.stop = stop;
    this.logprobs = logprobs;
    this.echo = echo;
  }

  public CompletionRequest(String prompt, double temperature, int max_tokens, String stop, boolean echo) {
    this(prompt, temperature, max_tokens, stop, null, echo);
  }

}
