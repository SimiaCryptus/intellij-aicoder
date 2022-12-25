package com.github.simiacryptus.aicoder.openai;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpenAI {

    public static final OpenAI INSTANCE = new OpenAI();

    protected AppSettingsState getSettingsState() {
        return AppSettingsState.getInstance();
    }

    public ObjectNode getEngines() throws IOException {
        return getMapper().readValue(get(getSettingsState().apiBase + "/engines"), ObjectNode.class);
    }

    protected String postRequest(String url, Map<String, Object> map) throws IOException, InterruptedException {
        return post(url, getMapper().writeValueAsString(map));
    }

    protected String post(String url, String body) throws IOException, InterruptedException {
        return post(url, body, 3);
    }

    /**
     * Completes a given text using the specified model.
     *
     * @param completionRequest The completion request containing the text to complete.
     * @param model             The model to use for completion.
     * @return The completion result.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the thread is interrupted.
     */
    public TextCompletion complete(CompletionRequest completionRequest, String model) throws IOException, InterruptedException {
        if(completionRequest.prompt.length() > AppSettingsState.getInstance().maxPrompt) throw new IOException("Prompt too long:" + completionRequest.prompt.length() + " chars");
        moderate(completionRequest.prompt);
        String result = post(getSettingsState().apiBase + "/engines/" + model + "/completions", getMapper().writeValueAsString(completionRequest));
        JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
        if (jsonObject.has("error")) {
            JsonObject errorObject = jsonObject.getAsJsonObject("error");
            throw new IOException(errorObject.get("message").getAsString());
        }
        return getMapper().readValue(result, TextCompletion.class);
    }

    public void moderate(String text) throws IOException, InterruptedException {
        String body = getMapper().writeValueAsString(Map.of("input", text));
        String result = post(getSettingsState().apiBase + "/moderations", body);
        JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
        if (jsonObject.has("error")) {
            JsonObject errorObject = jsonObject.getAsJsonObject("error");
            throw new IOException(errorObject.get("message").getAsString());
        }
        JsonObject moderationResult = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject();
        if(moderationResult.get("flagged").getAsBoolean()) {
            JsonObject categoriesObj = moderationResult.get("categories").getAsJsonObject();
            throw new IOException("Moderation flagged this request due to " + categoriesObj.keySet().stream().filter(c->categoriesObj.get(c).getAsBoolean()).reduce((a, b)->a+", "+b).orElse("???"));
        }
    }


    /**
     * Posts a request to the given URL with the given JSON body and retries if an IOException is thrown.
     *
     * @param url     The URL to post the request to.
     * @param json    The JSON body of the request.
     * @param retries The number of times to retry the request if an IOException is thrown.
     * @return The response from the request.
     * @throws IOException          If an IOException is thrown and the number of retries is exceeded.
     * @throws InterruptedException If the thread is interrupted while sleeping.
     */
    protected String post(String url, String json, int retries) throws IOException, InterruptedException {
        try {
            HttpClientBuilder client = HttpClientBuilder.create();
            HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            authorize(request);
            request.setEntity(new StringEntity(json));
            HttpResponse response = client.build().execute(request);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            if (retries > 0) {
                e.printStackTrace();
                Thread.sleep(15000);
                return post(url, json, retries - 1);
            }
            throw e;
        }
    }

    protected ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(MapperFeature.USE_STD_BEAN_NAMING)
                //.registerModule(DefaultScalaModule)
                .activateDefaultTyping(mapper.getPolymorphicTypeValidator());
        return mapper;
    }

    protected void authorize(HttpRequestBase request) throws IOException {
        request.addHeader("Authorization", "Bearer " + getSettingsState().apiKey);
    }

    /**
     * Gets the response from the given URL.
     *
     * @param url The URL to get the response from.
     * @return The response from the given URL.
     * @throws IOException If an I/O error occurs.
     */
    public String get(String url) throws IOException {
        HttpClientBuilder client = HttpClientBuilder.create();
        HttpGet request = new HttpGet(url);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        authorize(request);
        HttpResponse response = client.build().execute(request);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }


    /**
     * Creates a function that takes a String as input and returns a String as output.
     * The output is the result of a completion request using the given input and output tags, instruction, and attributes.
     *
     * @param inputTag    the tag to wrap the input text in
     * @param outputTag   the tag to wrap the output text in
     * @param instruction the instruction to include in the completion request
     * @param inputAttr   a map of attributes to include in the input tag
     * @param outputAttr  a map of attributes to include in the output tag
     * @param outputPrefix
     * @return a Function that takes a String as input and returns a String as output
     */
    public String xmlFN(String originalText, String inputTag, String outputTag, String instruction, Map<String, String> inputAttr, Map<String, String> outputAttr, String outputPrefix, String... stops) throws IOException {
        CompletionRequest request = xmlFnRequest(inputTag, outputTag, instruction, inputAttr, outputAttr, originalText).addStops(stops).appendPrompt(outputPrefix);
        TextCompletion completion = null;
        try {
            completion = complete(request, getSettingsState().model);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return getNewText(request, completion);
    }

    public static String getNewText(CompletionRequest request, TextCompletion completion) {
        // Get the first choice from the completion
        Optional<String> completionOption = Optional.ofNullable(completion.choices).flatMap(choices -> Arrays.stream(choices).findFirst()).map(choice -> choice.text.trim());
        // If the completion is empty, return the original text
        if (completionOption.isEmpty()) {
            return null;
        } else {
            // Otherwise, strip the prefix from the completion and return it
            return stripPrefix(completionOption.get(), request.prompt);
        }
    }

    @NotNull
    public CompletionRequest xmlFnRequest(String inputTag, String outputTag, String instruction, Map<String, String> inputAttr, Map<String, String> outputAttr, String originalText) {
        // Create a string of input attributes
        String inputAttributes = inputAttr.isEmpty() ? "" : (" " + inputAttr.entrySet().stream().map(t -> String.format("%s=\"%s\"", t.getKey(), t.getValue())).collect(Collectors.joining()));
        // Create a string of output attributes
        String outputAttributes = outputAttr.isEmpty() ? "" : (" " + outputAttr.entrySet().stream().map(t -> String.format("%s=\"%s\"", t.getKey(), t.getValue())).collect(Collectors.joining()));
        // Create a completion request
        CompletionRequest request = new CompletionRequest(
                // Format the request with the given input and output tags, attributes, and instruction
                String.format("<!-- %s -->\n<%s%s>%s</%s>\n<%s%s>", instruction, inputTag, inputAttributes, originalText, inputTag, outputTag, outputAttributes).trim(),
                // Get the temperature from the settings state
                getSettingsState().temperature,
                // Get the max tokens from the settings state
                getSettingsState().maxTokens,
                // Format the end tag
                null, true, String.format("</%s>", outputTag)
                // Set the completion to true
        );
        return request;
    }

    public static String stripPrefix(String text, String prefix) {
        boolean startsWith = text.startsWith(prefix);
        if (startsWith) {
            return text.substring(prefix.length());
        } else {
            return text;
        }
    }

}