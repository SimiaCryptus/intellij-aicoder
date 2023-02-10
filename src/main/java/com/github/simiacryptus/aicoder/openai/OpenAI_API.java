package com.github.simiacryptus.aicoder.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.simiacryptus.aicoder.config.AppSettingsState;
import com.github.simiacryptus.aicoder.util.IndentedText;
import com.github.simiacryptus.aicoder.util.StringTools;
import com.github.simiacryptus.aicoder.util.UITools;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.jetbrains.rd.util.AtomicReference;
import com.jetbrains.rd.util.LogLevel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.simiacryptus.aicoder.util.StringTools.stripPrefix;

public final class OpenAI_API {
    private static final Logger log = Logger.getInstance(OpenAI_API.class);
    public static final OpenAI_API INSTANCE = new OpenAI_API();
    public final @NotNull ListeningExecutorService pool;
    private final @NotNull ListeningScheduledExecutorService scheduledPool;
    private transient @Nullable AppSettingsState settings = null;
    private transient @Nullable ComboBox<CharSequence> comboBox = null;
    private static final WeakHashMap<ComboBox<CharSequence>, Object> activeModelUI = new WeakHashMap<>();

    @NotNull
    public JComponent getModelSelector() {
        if (null != comboBox) {
            @NotNull ComboBox<CharSequence> element = new ComboBox<>(IntStream.range(0, comboBox.getItemCount()).mapToObj(comboBox::getItemAt).toArray(CharSequence[]::new));
            activeModelUI.put(element, new Object());
            return element;
        }
        AppSettingsState settings = AppSettingsState.getInstance();
        @NotNull CharSequence apiKey = settings.apiKey;
        if (apiKey.toString().trim().length() > 0) {
            try {
                comboBox = new ComboBox<>(new CharSequence[]{settings.model_completion, settings.model_edit});
                activeModelUI.put(comboBox, new Object());
                onSuccess(INSTANCE.getEngines(), engines -> {
                    JsonNode data = engines.get("data");
                    CharSequence @NotNull [] items = new CharSequence[data.size()];
                    for (int i = 0; i < data.size(); i++) {
                        items[i] = data.get(i).get("id").asText();
                    }
                    Arrays.sort(items);
                    activeModelUI.keySet().forEach(ui -> Arrays.stream(items).forEach(ui::addItem));
                });
                return comboBox;
            } catch (Throwable e) {
                log.warn(e);
            }
        }
        return new JBTextField();
    }

    @NotNull
    public ListenableFuture<CharSequence> complete(@Nullable Project project, @NotNull CompletionRequest request, CharSequence indent) {
        return complete(project, request, filterCodeInsert(indent));
    }

    @NotNull
    public ListenableFuture<CharSequence> complete(@Nullable Project project, @NotNull CompletionRequest request, java.util.function.@NotNull Function<Optional<CharSequence>, Optional<CharSequence>> filter) {
        return map(complete(project, request), response -> filter.apply(response.getFirstChoice()).orElse(""));
    }

    @NotNull
    public ListenableFuture<CharSequence> edit(@Nullable Project project, @NotNull EditRequest request, CharSequence indent) {
        return edit(project, request, filterCodeInsert(indent));
    }

    @NotNull
    public ListenableFuture<CharSequence> edit(@Nullable Project project, @NotNull EditRequest request, java.util.function.@NotNull Function<Optional<CharSequence>, Optional<CharSequence>> filter) {
        return map(edit(project, request), response -> filter.apply(response.getFirstChoice()).orElse(""));
    }

    @NotNull
    public static java.util.function.Function<Optional<CharSequence>, Optional<CharSequence>> filterCodeInsert(CharSequence indent) {
        return response -> response
                .map(Objects::toString)
                .map(String::trim)
                .map(StringTools::stripUnbalancedTerminators)
                .map(IndentedText::fromString2)
                .map(text -> text.withIndent(indent))
                .map(IndentedText::toString)
                .map(text -> indent + text);
    }

    private AppSettingsState getSettingsState() {
        if (null == this.settings) {
            this.settings = AppSettingsState.getInstance();
        }
        return settings;
    }

    private OpenAI_API() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("OpenAI API Thread %d").build();
        this.pool = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(
                AppSettingsState.getInstance().apiThreads,
                AppSettingsState.getInstance().apiThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()));
        this.scheduledPool = MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(1, threadFactory));
    }

    @NotNull
    public ListenableFuture<ObjectNode> getEngines() {
        return pool.submit(() -> getMapper().readValue(get(getSettingsState().apiBase + "/engines"), ObjectNode.class));
    }

    private String post(String url, @NotNull String body) throws IOException, InterruptedException {
        return post(url, body, 1);
    }

    private @NotNull ListenableFuture<CompletionResponse> complete(@Nullable Project project, @NotNull CompletionRequest completionRequest) {
        AppSettingsState settings = getSettingsState();
        CompletionRequest.@NotNull CompletionRequestWithModel withModel = completionRequest.uiIntercept();
        withModel.fixup(settings);
        return complete(project, new CompletionRequest(withModel), settings, withModel.model);
    }

    private @NotNull ListenableFuture<CompletionResponse> edit(@Nullable Project project, @NotNull EditRequest request) {
        return edit(project, request, getSettingsState());
    }

    @NotNull
    private ListenableFuture<CompletionResponse> edit(@Nullable Project project, @NotNull EditRequest editRequest, @NotNull AppSettingsState settings) {
        return OpenAI_API.map(moderateAsync(project, editRequest.toString()), x -> {
            try {
                Task.@NotNull WithResult<CompletionResponse, Exception> task = new Task.WithResult<>(project, "OpenAI Text Completion", false) {
                    @Override
                    protected @NotNull CompletionResponse compute(@NotNull ProgressIndicator indicator) {
                        try {
                            if (editRequest.input == null) {
                                log(settings.apiLogLevel, String.format("Text Edit Request\nInstruction:\n\t%s\n",
                                        editRequest.instruction.replace("\n", "\n\t")));
                            } else {
                                log(settings.apiLogLevel, String.format("Text Edit Request\nInstruction:\n\t%s\nInput:\n\t%s\n",
                                        editRequest.instruction.replace("\n", "\n\t"),
                                        editRequest.input.replace("\n", "\n\t")));
                            }
                            String request = getMapper().writeValueAsString(editRequest);
                            String result = post(settings.apiBase + "/edits", request);
                            CompletionResponse completionResponse = processResponse(result, settings);
                            logComplete(completionResponse.getFirstChoice().orElse("").toString().trim(), settings);
                            return completionResponse;
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                if (null != project && !AppSettingsState.getInstance().suppressProgress) {
                    return ProgressManager.getInstance().run(task);
                } else {
                    task.run(new AbstractProgressIndicatorBase());
                    return task.getResult();
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Processes the response from the server.
     *
     * @param result   The response from the server.
     * @param settings The application settings.
     * @return The completion response.
     * @throws IOException If an error occurs while processing the response.
     */
    private CompletionResponse processResponse(String result, @NotNull AppSettingsState settings) throws IOException {
        JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
        if (jsonObject.has("error")) {
            JsonObject errorObject = jsonObject.getAsJsonObject("error");
            String errorMessage = errorObject.get("message").getAsString();
            throw new IOException(errorMessage);
        }
        CompletionResponse completionResponse = getMapper().readValue(result, CompletionResponse.class);
        if (completionResponse.usage != null) {
            settings.tokenCounter += completionResponse.usage.total_tokens;
        }
        return completionResponse;
    }

    @NotNull
    private ListenableFuture<CompletionResponse> complete(@Nullable Project project, @NotNull CompletionRequest completionRequest, @NotNull AppSettingsState settings, @NotNull final String model) {
        boolean canBeCancelled = true; // Cancel doesn't seem to work; the cancel event is only dispatched after the request completes.
        return OpenAI_API.map(moderateAsync(project, completionRequest.prompt), x -> run(project,
                new Task.WithResult<>(project, "OpenAI Text Completion", canBeCancelled) {
                    AtomicReference<Thread> threadRef = new AtomicReference<>(null);

                    @Override
                    protected @NotNull CompletionResponse compute(@NotNull ProgressIndicator indicator) {
                        ListenableScheduledFuture<?> cancelMonitor = scheduledPool.scheduleAtFixedRate(() -> checkCanceled(indicator, threadRef), 0, 100, TimeUnit.MILLISECONDS);
                        threadRef.getAndSet(Thread.currentThread());
                        try {
                            logStart(completionRequest, settings);
                            String request = getMapper().writeValueAsString(completionRequest);
                            String result = post(settings.apiBase + "/engines/" + model + "/completions", request);
                            CompletionResponse completionResponse = processResponse(result, settings);
                            @NotNull String completionResult = stripPrefix(completionResponse.getFirstChoice().orElse("").toString().trim(), completionRequest.prompt.trim());
                            logComplete(completionResult, settings);
                            return completionResponse;
                        } catch (IOException e) {
                            log.error(e);
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } finally {
                            threadRef.getAndSet(null);
                            cancelMonitor.cancel(true);
                        }
                    }

                    @Override
                    public void onCancel() {
                        Thread thread = threadRef.get();
                        if (null != thread) {
                            log.warn(Arrays.stream(thread.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n")));
                            thread.interrupt();
                        }
                        super.onCancel();
                    }
                }, 3));
    }

    private void checkCanceled(@NotNull ProgressIndicator indicator, AtomicReference<Thread> threadRef) {
        if (indicator.isCanceled()) {
            Thread thread = threadRef.get();
            if (null != thread) {
                thread.interrupt();
                try {
                    clients.get(thread).close();
                } catch (IOException e) {
                    log.warn("Error closing client: " + e.getMessage());
                }
            }
        }
    }

    private static <T> T run(@Nullable Project project, Task.@NotNull WithResult<T, Exception> task, int retries) {
        try {
            if (null != project && !AppSettingsState.getInstance().suppressProgress) {
                ProgressManager progressManager = ProgressManager.getInstance();
                ProgressIndicator progressIndicator = progressManager.getProgressIndicator();
                return progressManager.run(task);
            } else {
                task.run(new AbstractProgressIndicatorBase());
                return task.getResult();
            }
        } catch (RuntimeException e) {
            if (isInterruptedException(e)) throw e;
            if (retries > 0) {
                log.warn("Retrying request", e);
                return run(project, task, retries - 1);
            } else {
                throw e;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            if (isInterruptedException(e)) throw new RuntimeException(e);
            if (retries > 0) {
                log.warn("Retrying request", e);
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                return run(project, task, retries - 1);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean isInterruptedException(Throwable e) {
        if (e instanceof InterruptedException) return true;
        if (e.getCause() != null && e.getCause() != e) return isInterruptedException(e.getCause());
        return false;
    }

    private void logComplete(@NotNull String completionResult, @NotNull AppSettingsState settings) {
        log(settings.apiLogLevel, String.format("Text Completion Completion:\n\t%s",
                completionResult.replace("\n", "\n\t")));
    }

    private void logStart(@NotNull CompletionRequest completionRequest, @NotNull AppSettingsState settings) {
        if (completionRequest.suffix == null) {
            log(settings.apiLogLevel, String.format("Text Completion Request\nPrefix:\n\t%s\n",
                    completionRequest.prompt.replace("\n", "\n\t")));
        } else {
            log(settings.apiLogLevel, String.format("Text Completion Request\nPrefix:\n\t%s\nSuffix:\n\t%s\n",
                    completionRequest.prompt.replace("\n", "\n\t"),
                    completionRequest.suffix.replace("\n", "\n\t")));
        }
    }

    public static <I extends @Nullable Object, O extends @Nullable Object>
    @NotNull ListenableFuture<O> map(@NotNull ListenableFuture<I> moderateAsync, @NotNull Function<? super I, ? extends O> o) {
        return Futures.transform(moderateAsync, o, INSTANCE.pool);
    }

    public static <I extends @Nullable Object> void onSuccess(@NotNull ListenableFuture<I> moderateAsync, @NotNull Consumer<? super I> o) {
        Futures.addCallback(moderateAsync, new FutureCallback<>() {
            @Override
            public void onSuccess(I result) {
                o.accept(result);
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                UITools.handle(t);
            }
        }, INSTANCE.pool);
    }

    private void log(@NotNull LogLevel level, @NotNull String msg) {
        @NotNull String message = msg.trim().replace("\n", "\n\t");
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
    private ListenableFuture<?> moderateAsync(@Nullable Project project, @NotNull String text) {
        return run(project, new Task.WithResult<>(project, "OpenAI Moderation", false) {
            @Override
            protected @NotNull ListenableFuture<?> compute(@NotNull ProgressIndicator indicator) {
                return pool.submit(() -> {
                    @Nullable String body;
                    try {
                        body = getMapper().writeValueAsString(Map.of("input", text));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    AppSettingsState settings1 = getSettingsState();
                    @Nullable String result;
                    try {
                        result = post(settings1.apiBase + "/moderations", body);
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
            }
        }, 0);
    }

    private final Map<Thread, CloseableHttpClient> clients = new ConcurrentHashMap<>();

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
    private String post(String url, @NotNull String json, int retries) throws IOException, InterruptedException {
        try {
            @NotNull HttpClientBuilder client = HttpClientBuilder.create();
            @NotNull HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            authorize(request);
            request.setEntity(new StringEntity(json));
            try (CloseableHttpClient httpClient = client.build()) {
                clients.put(Thread.currentThread(), httpClient);
                HttpResponse response = httpClient.execute(request);
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            } finally {
                clients.remove(Thread.currentThread());
            }
        } catch (IOException e) {
            if (retries > 0) {
                log.warn("Error posting request to " + url + ", retrying in 15 seconds", e);
                Thread.sleep(15000);
                return post(url, json, retries - 1);
            }
            throw e;
        }
    }

    @NotNull
    private ObjectMapper getMapper() {
        @NotNull ObjectMapper mapper = new ObjectMapper();
        mapper
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(MapperFeature.USE_STD_BEAN_NAMING)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .activateDefaultTyping(mapper.getPolymorphicTypeValidator());
        return mapper;
    }

    private void authorize(@NotNull HttpRequestBase request) throws IOException {
        AppSettingsState settingsState = getSettingsState();
        @Nullable String apiKey = settingsState.apiKey;
        if (apiKey.length() == 0) {
            synchronized (settingsState) {
                apiKey = settingsState.apiKey;
                if (apiKey.length() == 0) {
                    apiKey = UITools.queryAPIKey();
                    settingsState.apiKey = Objects.requireNonNull(apiKey);
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
        @NotNull HttpClientBuilder client = HttpClientBuilder.create();
        @NotNull HttpGet request = new HttpGet(url);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        authorize(request);
        try (CloseableHttpClient httpClient = client.build()) {
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        }
    }

}