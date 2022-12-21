package com.github.simiacryptus.aicoder.openai;

public class TextCompletion {
  public String id;
  public String object;
  public int created;
  public String model;
  public Choice[] choices;
  public ApiError error;

  public Usage usage;

  public TextCompletion() {
  }

  public TextCompletion(String id, String object, int created, String model, Choice[] choices, ApiError error) {
    this.id = id;
    this.object = object;
    this.created = created;
    this.model = model;
    this.choices = choices;
    this.error = error;
  }

}
