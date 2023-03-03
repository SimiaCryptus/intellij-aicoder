package com.github.simiacryptus.aicoder.openai

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.IndentedText
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools
import com.google.common.util.concurrent.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.jetbrains.rd.util.AtomicReference
import com.jetbrains.rd.util.LogLevel
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.util.*
import java.util.Map
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.swing.JComponent
import kotlin.collections.MutableMap
import kotlin.collections.set
import kotlin.collections.toTypedArray

object OpenAI_API {

    @JvmStatic
    private val threadFactory: ThreadFactory = ThreadFactoryBuilder().setNameFormat("API Thread %d").build()

    @JvmStatic
    val pool: ListeningExecutorService = MoreExecutors.listeningDecorator(
        ThreadPoolExecutor(
            AppSettingsState.getInstance().apiThreads,
            AppSettingsState.getInstance().apiThreads,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            threadFactory,
            ThreadPoolExecutor.AbortPolicy()
        )
    )

    @JvmStatic
    val scheduledPool: ListeningScheduledExecutorService =
        MoreExecutors.listeningDecorator(ScheduledThreadPoolExecutor(1, threadFactory))

    @Transient
    private var settings: AppSettingsState? = null

    @Transient
    private var comboBox: ComboBox<CharSequence?>? = null
    val modelSelector: JComponent
        get() {
            if (null != comboBox) {
                val element = ComboBox((IntStream.range(0, comboBox!!.itemCount).mapToObj { index: Int ->
                    comboBox!!.getItemAt(index)
                }).collect(Collectors.toList()).toTypedArray())
                activeModelUI[element] = Any()
                return element
            }
            val settings = AppSettingsState.getInstance()
            val apiKey: CharSequence = settings.apiKey
            if (apiKey.toString().trim { it <= ' ' }.length > 0) {
                try {
                    comboBox = ComboBox(arrayOf<CharSequence?>(settings.model_completion, settings.model_edit))
                    activeModelUI[comboBox] = Any()
                    onSuccess(
                        engines
                    ) { engines: ObjectNode ->
                        val data = engines["data"]
                        val items =
                            arrayOfNulls<CharSequence>(data.size())
                        for (i in 0 until data.size()) {
                            items[i] = data[i]["id"].asText()
                        }
                        Arrays.sort(items)
                        activeModelUI.keys.forEach(Consumer { ui: ComboBox<CharSequence?> ->
                            Arrays.stream(items).forEach { item: CharSequence? ->
                                ui.addItem(
                                    item
                                )
                            }
                        })
                    }
                    return comboBox!!
                } catch (e: Throwable) {
                    log.warn(e)
                }
            }
            return JBTextField()
        }

    fun complete(project: Project?, request: CompletionRequest, indent: CharSequence): ListenableFuture<CharSequence?> {
        return complete(project, request, filterCodeInsert(indent))
    }

    fun complete(
        project: Project?,
        request: CompletionRequest,
        filter: (Optional<CharSequence?>) -> Optional<String>
    ): ListenableFuture<CharSequence?> {
        return map(
            complete(project, request)
        ) { response: CompletionResponse ->
            filter(
                response.firstChoice
            ).orElse("")
        }
    }

    fun edit(project: Project?, request: EditRequest, indent: CharSequence): ListenableFuture<CharSequence?> {
        return edit(project, request, filterCodeInsert(indent))
    }

    fun edit(
        project: Project?,
        request: EditRequest,
        filter: (Optional<CharSequence?>) -> Optional<String>
    ): ListenableFuture<CharSequence?> {
        return map(
            edit(project, request)
        ) { response: CompletionResponse ->
            filter(
                response.firstChoice
            ).orElse("")
        }
    }

    private val settingsState: AppSettingsState?
        get() {
            if (null == settings) {
                settings = AppSettingsState.getInstance()
            }
            return settings
        }
    val engines: ListenableFuture<ObjectNode>
        get() = pool.submit<ObjectNode> {
            mapper.readValue(
                get(settingsState!!.apiBase + "/engines"),
                ObjectNode::class.java
            )
        }

    @Throws(IOException::class, InterruptedException::class)
    private fun post(url: String, body: String): String {
        return post(url, body, 1)
    }

    private fun complete(
        project: Project?,
        completionRequest: CompletionRequest
    ): ListenableFuture<CompletionResponse> {
        val settings = settingsState
        val withModel = completionRequest.uiIntercept()
        withModel.fixup(settings!!)
        return complete(project, CompletionRequest(withModel), settings, withModel.model)
    }

    private fun edit(project: Project?, request: EditRequest): ListenableFuture<CompletionResponse> {
        return edit(project, request, settingsState!!)
    }

    private fun edit(
        project: Project?,
        editRequest: EditRequest,
        settings: AppSettingsState
    ): ListenableFuture<CompletionResponse> {
        return map(moderateAsync(project, editRequest.toString())) { _: Any? ->
            try {
                val task: Task.WithResult<CompletionResponse, Exception?> =
                    object : Task.WithResult<CompletionResponse, Exception?>(
                        project,
                        "Text Completion",
                        false
                    ) {
                        override fun compute(indicator: ProgressIndicator): CompletionResponse {
                            try {
                                if (editRequest.input == null) {
                                    log(
                                        settings.apiLogLevel, String.format(
                                            "Text Edit Request\nInstruction:\n\t%s\n",
                                            editRequest.instruction.replace("\n", "\n\t")
                                        )
                                    )
                                } else {
                                    log(
                                        settings.apiLogLevel, String.format(
                                            "Text Edit Request\nInstruction:\n\t%s\nInput:\n\t%s\n",
                                            editRequest.instruction.replace("\n", "\n\t"),
                                            editRequest.input!!.replace("\n", "\n\t")
                                        )
                                    )
                                }
                                val request: String = mapper.writeValueAsString(editRequest)
                                val result = post(settings.apiBase + "/edits", request)
                                val completionResponse = processResponse(result, settings)
                                logComplete(
                                    completionResponse.firstChoice.orElse("").toString().trim { it <= ' ' },
                                    settings
                                )
                                return completionResponse
                            } catch (e: IOException) {
                                throw RuntimeException(e)
                            } catch (e: InterruptedException) {
                                throw RuntimeException(e)
                            }
                        }
                    }
                if (null != project && !AppSettingsState.getInstance().suppressProgress) {
                    return@map ProgressManager.getInstance()
                        .run(task)
                } else {
                    task.run(AbstractProgressIndicatorBase())
                    return@map task.result
                }
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    /**
     * Processes the response from the server.
     *
     * @param result   The response from the server.
     * @param settings The application settings.
     * @return The completion response.
     * @throws IOException If an error occurs while processing the response.
     */
    @Throws(IOException::class)
    private fun processResponse(result: String, settings: AppSettingsState): CompletionResponse {
        val jsonObject = Gson().fromJson(
            result,
            JsonObject::class.java
        )
        if (jsonObject.has("error")) {
            val errorObject = jsonObject.getAsJsonObject("error")
            val errorMessage = errorObject["message"].asString
            throw IOException(errorMessage)
        }
        val completionResponse = mapper.readValue(
            result,
            CompletionResponse::class.java
        )
        if (completionResponse.usage != null) {
            settings.tokenCounter += completionResponse.usage.total_tokens
        }
        return completionResponse
    }

    private fun complete(
        project: Project?,
        completionRequest: CompletionRequest,
        settings: AppSettingsState,
        model: String
    ): ListenableFuture<CompletionResponse> {
        val canBeCancelled =
            true // Cancel doesn't seem to work; the cancel event is only dispatched after the request completes.
        return map(moderateAsync(project, completionRequest.prompt)) { _: Any? ->
            run(
                project,
                object : Task.WithResult<CompletionResponse, Exception?>(
                    project,
                    "Text Completion",
                    canBeCancelled
                ) {
                    var threadRef =
                        AtomicReference<Thread?>(null)

                    override fun compute(indicator: ProgressIndicator): CompletionResponse {
                        val cancelMonitor =
                            scheduledPool.scheduleAtFixedRate(
                                { checkCanceled(indicator, threadRef) },
                                0,
                                100,
                                TimeUnit.MILLISECONDS
                            )
                        threadRef.getAndSet(Thread.currentThread())
                        try {
                            logStart(completionRequest, settings)
                            val request: String = mapper.writeValueAsString(completionRequest)
                            val result =
                                post(settings.apiBase + "/engines/" + model + "/completions", request)
                            val completionResponse = processResponse(result, settings)
                            val completionResult = StringTools.stripPrefix(
                                completionResponse.firstChoice.orElse("").toString().trim { it <= ' ' },
                                completionRequest.prompt.trim { it <= ' ' })
                            logComplete(completionResult, settings)
                            return completionResponse
                        } catch (e: IOException) {
                            log.error(e)
                            throw RuntimeException(e)
                        } catch (e: InterruptedException) {
                            throw RuntimeException(e)
                        } finally {
                            threadRef.getAndSet(null)
                            cancelMonitor.cancel(true)
                        }
                    }

                    override fun onCancel() {
                        val thread = threadRef.get()
                        if (null != thread) {
                            log.warn(Arrays.stream(
                                thread.stackTrace
                            ).map { obj: StackTraceElement -> obj.toString() }
                                .collect(Collectors.joining("\n"))
                            )
                            thread.interrupt()
                        }
                        super.onCancel()
                    }
                }, 3
            )
        }
    }

    private fun checkCanceled(indicator: ProgressIndicator, threadRef: AtomicReference<Thread?>) {
        if (indicator.isCanceled) {
            val thread = threadRef.get()
            if (null != thread) {
                thread.interrupt()
                try {
                    clients[thread]!!.close()
                } catch (e: IOException) {
                    log.warn("Error closing client: " + e.message)
                }
            }
        }
    }

    private fun logComplete(completionResult: CharSequence, settings: AppSettingsState) {
        log(
            settings.apiLogLevel, String.format(
                "Text Completion Completion:\n\t%s",
                completionResult.toString().replace("\n", "\n\t")
            )
        )
    }

    private fun logStart(completionRequest: CompletionRequest, settings: AppSettingsState) {
        if (completionRequest.suffix == null) {
            log(
                settings.apiLogLevel, String.format(
                    "Text Completion Request\nPrefix:\n\t%s\n",
                    completionRequest.prompt.replace("\n", "\n\t")
                )
            )
        } else {
            log(
                settings.apiLogLevel, String.format(
                    "Text Completion Request\nPrefix:\n\t%s\nSuffix:\n\t%s\n",
                    completionRequest.prompt.replace("\n", "\n\t"),
                    completionRequest.suffix!!.replace("\n", "\n\t")
                )
            )
        }
    }

    private fun log(level: LogLevel, msg: String) {
        val message = msg.trim { it <= ' ' }.replace("\n", "\n\t")
        when (level) {
            LogLevel.Error -> log.error(message)
            LogLevel.Warn -> log.warn(message)
            LogLevel.Info -> log.info(message)
            else -> log.debug(message)
        }
    }

    private fun moderateAsync(project: Project?, text: String): ListenableFuture<*> {
        return run(
            project,
            object : Task.WithResult<ListenableFuture<*>, Exception?>(project, "Moderation", false) {
                override fun compute(indicator: ProgressIndicator): ListenableFuture<*> {
                    return pool.submit {
                        val body: String
                        body = try {
                            mapper.writeValueAsString(
                                Map.of(
                                    "input",
                                    text
                                )
                            )
                        } catch (e: JsonProcessingException) {
                            throw RuntimeException(e)
                        }
                        val settings1: AppSettingsState = settingsState!!
                        val result: String
                        result = try {
                            post(settings1.apiBase + "/moderations", body)
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        } catch (e: InterruptedException) {
                            throw RuntimeException(e)
                        }
                        val jsonObject =
                            Gson().fromJson(
                                result,
                                JsonObject::class.java
                            )
                        if (jsonObject.has("error")) {
                            val errorObject = jsonObject.getAsJsonObject("error")
                            throw RuntimeException(IOException(errorObject["message"].asString))
                        }
                        val moderationResult =
                            jsonObject.getAsJsonArray("results")[0].asJsonObject
                        log(
                            LogLevel.Debug,
                            String.format(
                                "Moderation Request\nText:\n%s\n\nResult:\n%s",
                                text.replace("\n", "\n\t"),
                                result
                            )
                        )
                        if (moderationResult["flagged"].asBoolean) {
                            val categoriesObj =
                                moderationResult["categories"].asJsonObject
                            throw RuntimeException(
                                ModerationException(
                                    "Moderation flagged this request due to " + categoriesObj.keySet()
                                        .stream().filter { c: String? ->
                                            categoriesObj[c].asBoolean
                                        }.reduce { a: String, b: String -> "$a, $b" }
                                        .orElse("???")
                                )
                            )
                        }
                    }
                }
            },
            0
        )
    }

    private val clients: MutableMap<Thread, CloseableHttpClient> = ConcurrentHashMap()

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
    @Throws(IOException::class, InterruptedException::class)
    private fun post(url: String, json: String, retries: Int): String {
        return post(jsonRequest(url, json), retries)
    }

    private fun post(
        request: HttpPost,
        retries: Int
    ): String {
        try {
            val client = HttpClientBuilder.create()
            try {
                client.build().use { httpClient ->
                    clients[Thread.currentThread()] = httpClient
                    val response: HttpResponse = httpClient.execute(request)
                    val entity = response.entity
                    return EntityUtils.toString(entity)
                }
            } finally {
                clients.remove(Thread.currentThread())
            }
        } catch (e: IOException) {
            if (retries > 0) {
                log.warn("Error posting request, retrying in 15 seconds", e)
                Thread.sleep(15000)
                return post(request, retries - 1)
            }
            throw e
        }
    }

    private fun jsonRequest(url: String, json: String): HttpPost {
        val request = HttpPost(url)
        request.addHeader("Content-Type", "application/json")
        request.addHeader("Accept", "application/json")
        authorize(request)
        request.entity = StringEntity(json)
        return request
    }

    private val mapper: ObjectMapper
        get() {
            val mapper = ObjectMapper()
            mapper
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(MapperFeature.USE_STD_BEAN_NAMING)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .activateDefaultTyping(mapper.polymorphicTypeValidator)
            return mapper
        }

    @Throws(IOException::class)
    private fun authorize(request: HttpRequestBase) {
        val settingsState = settingsState
        var apiKey: CharSequence = settingsState!!.apiKey
        if (apiKey.length == 0) {
            synchronized(settingsState) {
                apiKey = settingsState.apiKey
                if (apiKey.length == 0) {
                    apiKey = UITools.queryAPIKey()!!
                    settingsState.apiKey = apiKey.toString()
                }
            }
        }
        request.addHeader("Authorization", "Bearer $apiKey")
    }

    /**
     * Gets the response from the given URL.
     *
     * @param url The URL to GET the response from.
     * @return The response from the given URL.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    operator fun get(url: String?): String {
        val client = HttpClientBuilder.create()
        val request = HttpGet(url)
        request.addHeader("Content-Type", "application/json")
        request.addHeader("Accept", "application/json")
        authorize(request)
        client.build().use { httpClient ->
            val response: HttpResponse = httpClient.execute(request)
            val entity = response.entity
            return EntityUtils.toString(entity)
        }
    }

    private val log = Logger.getInstance(OpenAI_API::class.java)
    private val activeModelUI = WeakHashMap<ComboBox<CharSequence?>, Any>()
    fun filterCodeInsert(indent: CharSequence): (Optional<CharSequence?>) -> Optional<String> {
        return { response: Optional<CharSequence?> ->
            response
                .map { o: CharSequence? ->
                    Objects.toString(
                        o
                    )
                }
                .map { obj: String -> obj.trim { it <= ' ' } }
                .map { input: String? ->
                    StringTools.stripUnbalancedTerminators(
                        input!!
                    )
                }
                .map { text: CharSequence? ->
                    IndentedText.fromString2(
                        text!!.toString()
                    )
                }
                .map { text: IndentedText ->
                    text.withIndent(
                        indent
                    )
                }
                .map { obj: IndentedText -> obj.toString() }
                .map { text: String -> indent.toString() + text }
        }
    }

    private fun <T> run(project: Project?, task: Task.WithResult<T, Exception?>, retries: Int): T {
        return try {
            if (null != project && !AppSettingsState.getInstance().suppressProgress) {
                ProgressManager.getInstance().run(task)
            } else {
                task.run(AbstractProgressIndicatorBase())
                task.result
            }
        } catch (e: RuntimeException) {
            if (isInterruptedException(e)) throw e
            if (retries > 0) {
                log.warn("Retrying request", e)
                run(project, task, retries - 1)
            } else {
                throw e
            }
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: Exception) {
            if (isInterruptedException(e)) throw RuntimeException(e)
            if (retries > 0) {
                log.warn("Retrying request", e)
                try {
                    Thread.sleep(15000)
                } catch (ex: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                run(project, task, retries - 1)
            } else {
                throw RuntimeException(e)
            }
        }
    }

    private fun isInterruptedException(e: Throwable?): Boolean {
        if (e is InterruptedException) return true
        return if (e!!.cause != null && e.cause !== e) isInterruptedException(
            e.cause
        ) else false
    }

    fun <I : Any?, O : Any?> map(
        moderateAsync: ListenableFuture<I>,
        o: com.google.common.base.Function<in I, out O>
    ): ListenableFuture<O> = Futures.transform(moderateAsync, o, pool)

    fun <I : Any?> onSuccess(moderateAsync: ListenableFuture<I>, o: Consumer<in I>) {
        Futures.addCallback(moderateAsync, object : FutureCallback<I> {
            override fun onSuccess(result: I) {
                o.accept(result)
            }

            override fun onFailure(t: Throwable) {
                UITools.handle(t)
            }
        }, pool)
    }

    fun text_to_speech(wavAudio: ByteArray): String {
        val settings = AppSettingsState.getInstance()
        val url = settings!!.apiBase + "/audio/transcriptions"
        val request = HttpPost(url)
        request.addHeader("Accept", "application/json")
        authorize(request)
        val entity = MultipartEntityBuilder.create()
        entity.setMode(HttpMultipartMode.RFC6532)
        entity.addBinaryBody("file", wavAudio, ContentType.create("audio/x-wav"), "audio.wav")
        entity.addTextBody("model", "whisper-1")
        request.entity = entity.build()
        val response = post(request, 3)
        val jsonObject = Gson().fromJson(response, JsonObject::class.java)
        if (jsonObject.has("error")) {
            val errorObject = jsonObject.getAsJsonObject("error")
            throw RuntimeException(IOException(errorObject["message"].asString))
        }
        return jsonObject.get("text").asString!!
    }

}