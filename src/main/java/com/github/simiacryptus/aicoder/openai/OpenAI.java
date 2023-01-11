package com.github.simiacryptus.aicoder.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.simiacryptus.aicoder.config.AppSettingsComponent;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.rd.util.LogLevel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static com.github.simiacryptus.aicoder.text.StringTools.stripPrefix;

public final class OpenAI {

    private static final Logger log = Logger.getInstance(OpenAI.class);

    public static final OpenAI INSTANCE = new OpenAI();
    public final ListeningExecutorService pool;
    private transient AppSettingsState settings = null;

    protected AppSettingsState getSettingsState() {
        if (null == this.settings) {
            this.settings = AppSettingsState.getInstance();
        }
        return settings;
    }

    protected OpenAI() {
        this.pool = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }

    @NotNull
    public ListenableFuture<ObjectNode> getEngines() {
        return pool.submit(() -> getMapper().readValue(get(getSettingsState().apiBase + "/engines"), ObjectNode.class));
    }

    protected String post(String url, @NotNull String body) throws IOException, InterruptedException {
        return post(url, body, 3);
    }

    public ListenableFuture<CompletionResponse> complete(@NotNull CompletionRequest completionRequest) {
        AppSettingsState settings = getSettingsState();
        if (completionRequest.prompt.length() > settings.maxPrompt)
            throw new IllegalArgumentException("Prompt too long:" + completionRequest.prompt.length() + " chars");
        return OpenAI.map(moderateAsync(completionRequest.prompt), x->{
            try {
                String request = getMapper().writeValueAsString(completionRequest);
                String result = post(settings.apiBase + "/engines/" + settings.model + "/completions", request);
                JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
                if (jsonObject.has("error")) {
                    JsonObject errorObject = jsonObject.getAsJsonObject("error");
                    String errorMessage = errorObject.get("message").getAsString();
                    log.error(errorMessage);
                    throw new IOException(errorMessage);
                }
                CompletionResponse completionResponse = getMapper().readValue(result, CompletionResponse.class);
                String completionResult = stripPrefix(completionResponse.getFirstChoice().orElse("").trim(), completionRequest.prompt.trim());
                log(settings.apiLogLevel, String.format("Text Completion Request\nPrefix:\n\t%s\n\nCompletion:\n\t%s", completionRequest.prompt.replace("\n", "\n\t"), completionResult.replace("\n", "\n\t")));
                return completionResponse;
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <I extends @Nullable Object, O extends @Nullable Object>
    ListenableFuture<O> map(ListenableFuture<I> moderateAsync, Function<? super I,? extends O> o) {
        return Futures.transform(moderateAsync, o, INSTANCE.pool);
    }
    public static <I extends @Nullable Object> void onSuccess(ListenableFuture<I> moderateAsync, Consumer<? super I> o) {
        Futures.addCallback(moderateAsync, new FutureCallback<I>() {
            @Override
            public void onSuccess(I result) {
                o.accept(result);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error(t);
            }
        }, INSTANCE.pool);
    }

    private void log(LogLevel level, String msg) {
        String message = msg.trim().replace("\n", "\n\t");
        switch (level) {
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

    @NotNull
    private ListenableFuture<?> moderateAsync(@NotNull String text) {
        ListenableFuture<?> future = pool.submit(()->{
            String body = null;
            try {
                body = getMapper().writeValueAsString(Map.of("input", text));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            AppSettingsState settings = getSettingsState();
            String result = null;
            try {
                result = post(settings.apiBase + "/moderations", body);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
            if (jsonObject.has("error")) {
                JsonObject errorObject = jsonObject.getAsJsonObject("error");
                throw new RuntimeException(new IOException(errorObject.get("message").getAsString()));
            }
            JsonObject moderationResult = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject();
            log(LogLevel.Debug, String.format("Moderation Request\nText:\n%s\n\nResult:\n%s", text.replace("\n", "\n\t"), result));
            if (moderationResult.get("flagged").getAsBoolean()) {
                JsonObject categoriesObj = moderationResult.get("categories").getAsJsonObject();
                throw new RuntimeException(new ModerationException("Moderation flagged this request due to " + categoriesObj.keySet().stream().filter(c -> categoriesObj.get(c).getAsBoolean()).reduce((a, b) -> a + ", " + b).orElse("???")));
            }
        });
        return future;
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
     * @param url The URL to GET the response from.
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

}