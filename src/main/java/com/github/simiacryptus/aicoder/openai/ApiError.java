package com.github.simiacryptus.aicoder.openai;

public class ApiError {
    @SuppressWarnings("unused")
    public String message;
    @SuppressWarnings("unused")
    public String type;
    @SuppressWarnings("unused")
    public String param;
    @SuppressWarnings("unused")
    public Double code;

    @SuppressWarnings("unused")
    public ApiError() {
    }

    @SuppressWarnings("unused")
    public ApiError(String message, String type, String param, Double code) {
        this.message = message;
        this.type = type;
        this.param = param;
        this.code = code;
    }

}
