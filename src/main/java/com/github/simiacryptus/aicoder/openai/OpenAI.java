package com.github.simiacryptus.aicoder.openai;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.simiacryptus.aicoder.config.AppSettingsComponent;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
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
import java.util.Map;

import static com.github.simiacryptus.aicoder.openai.StringTools.stripPrefix;

public class OpenAI {

    private static final Logger log = Logger.getInstance(OpenAI.class);

    public static final OpenAI INSTANCE = new OpenAI();
    private transient AppSettingsState settings = null;

    protected AppSettingsState getSettingsState() {
        if (null == this.settings) {
            this.settings = AppSettingsState.getInstance();
        }
        return settings;
    }

    public ObjectNode getEngines() throws IOException {
        return getMapper().readValue(request(getSettingsState().apiBase + "/engines"), ObjectNode.class);
    }

    protected String post(String url, @NotNull String body) throws IOException, InterruptedException {
        return post(url, body, 3);
    }

    public TextCompletion request(@NotNull CompletionRequest completionRequest) throws IOException {
        try {
            AppSettingsState settings = getSettingsState();
            if (completionRequest.prompt.length() > settings.maxPrompt)
                throw new IOException("Prompt too long:" + completionRequest.prompt.length() + " chars");
            moderate(completionRequest.prompt);
            String request = getMapper().writeValueAsString(completionRequest);
            String result = post(settings.apiBase + "/engines/" + settings.model + "/completions", request);
            JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
            if (jsonObject.has("error")) {
                JsonObject errorObject = jsonObject.getAsJsonObject("error");
                String errorMessage = errorObject.get("message").getAsString();
                log.error(errorMessage);
                throw new IOException(errorMessage);
            }
            TextCompletion textCompletion = getMapper().readValue(result, TextCompletion.class);
            String completionResult = stripPrefix(textCompletion.getFirstChoice().orElse(""), completionRequest.prompt).replace("\n", "\n\t");
            requestLog(String.format("Text Completion Request\nPrefix:\n%s\n\nCompletion:\n%s", completionRequest.prompt.replace("\n", "\n\t"), completionResult));
            //writeRequestLog(String.format("Request:\n%s\n\nResponse:\n%s", request, result));
            return textCompletion;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void requestLog(String msg) {
        String message = msg.trim().replace("\n", "\n\t");
        switch (getSettingsState().apiLogLevel) {
            case Error:
                log.error(message);
                break;
            case Warn:
                log.warn(message);
                break;
            case Info:
                log.info(message);
                break;
            default:
                log.debug(message);
                break;
        }
    }

    public void moderate(@NotNull String text) throws IOException, InterruptedException {
        String body = getMapper().writeValueAsString(Map.of("input", text));
        String result = post(getSettingsState().apiBase + "/moderations", body);
        JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
        if (jsonObject.has("error")) {
            JsonObject errorObject = jsonObject.getAsJsonObject("error");
            throw new IOException(errorObject.get("message").getAsString());
        }
        JsonObject moderationResult = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject();
        requestLog(String.format("Moderation Request\nText:\n%s\n\nResult:\n%s", text.replace("\n", "\n\t"), result));
        if (moderationResult.get("flagged").getAsBoolean()) {
            JsonObject categoriesObj = moderationResult.get("categories").getAsJsonObject();
            throw new IOException("Moderation flagged this request due to " + categoriesObj.keySet().stream().filter(c -> categoriesObj.get(c).getAsBoolean()).reduce((a, b) -> a + ", " + b).orElse("???"));
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
    protected String post(String url, @NotNull String json, int retries) throws IOException, InterruptedException {
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

    protected @NotNull ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(MapperFeature.USE_STD_BEAN_NAMING)
                //.registerModule(DefaultScalaModule)
                .activateDefaultTyping(mapper.getPolymorphicTypeValidator());
        return mapper;
    }

    protected void authorize(@NotNull HttpRequestBase request) throws IOException {
        AppSettingsState settingsState = getSettingsState();
        String apiKey = settingsState.apiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            synchronized (settingsState) {
                apiKey = settingsState.apiKey;
                if (apiKey == null || apiKey.isEmpty()) {
                    apiKey = AppSettingsComponent.queryAPIKey();
                    settingsState.apiKey = apiKey;
                }
            }
        }
        request.addHeader("Authorization", "Bearer " + apiKey);
    }

    /**
     * Gets the response from the given URL.
     *
     * @param url The URL to get the response from.
     * @return The response from the given URL.
     * @throws IOException If an I/O error occurs.
     */
    public String request(String url) throws IOException {
        HttpClientBuilder client = HttpClientBuilder.create();
        HttpGet request = new HttpGet(url);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        authorize(request);
        HttpResponse response = client.build().execute(request);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }

}