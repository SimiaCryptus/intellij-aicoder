package com.github.simiacryptus.aicoder.openai;

public class ApiError {
  public String message;
  public String type;
  public String param;
  public Double code;

  public ApiError() {
  }

  public ApiError(String message, String type, String param, Double code) {
    this.message = message;
    this.type = type;
    this.param = param;
    this.code = code;
  }

}
