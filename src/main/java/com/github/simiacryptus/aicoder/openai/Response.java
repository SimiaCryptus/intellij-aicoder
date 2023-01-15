package com.github.simiacryptus.aicoder.openai;

public class Response {
    @SuppressWarnings("unused")
    public String object;
    @SuppressWarnings("unused")
    private Engine[] data;

    @SuppressWarnings("unused")
    public Response() {
    }

    @SuppressWarnings("unused")
    public Response(String object, Engine[] data) {
        this.object = object;
        this.data = data;
    }

}
