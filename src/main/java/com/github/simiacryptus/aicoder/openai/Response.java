package com.github.simiacryptus.aicoder.openai;

public class Response {
  public String object;
  private Engine[] data;

  public Response() {
  }

  public Response(String object, Engine[] data) {
    this.object = object;
    this.data = data;
  }

}
