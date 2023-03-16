package com.github.simiacryptus.aicoder.openai.core

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.simiacryptus.aicoder.openai.async.AsyncAPI
import com.github.simiacryptus.aicoder.openai.core.*
import com.github.simiacryptus.aicoder.openai.ui.OpenAI_API
import com.github.simiacryptus.aicoder.util.StringTools
import com.github.simiacryptus.aicoder.util.UITools
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.Logger
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
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.collections.MutableMap
import kotlin.collections.first
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.set

open class CoreAPI(
    val apiBase: String,
    var key: String,
    val logLevel: LogLevel
) {
    fun getEngines(): Array<CharSequence?> {
        val engines = mapper.readValue(
            get(OpenAI_API.settingsState!!.apiBase + "/engines"),
            ObjectNode::class.java
        )
        val data = engines["data"]
        val items =
            arrayOfNulls<CharSequence>(data.size())
        for (i in 0 until data.size()) {
            items[i] = data[i]["id"].asText()
        }
        Arrays.sort(items)
        return items
    }

    val clients: MutableMap<Thread, CloseableHttpClient> = ConcurrentHashMap()

    @Throws(IOException::class, InterruptedException::class)
    fun post(url: String, body: String): String {
        return post(url, body, 3)
    }


    fun logComplete(completionResult: CharSequence) {
        log(
            logLevel, String.format(
                "Text Completion Completion:\n\t%s",
                completionResult.toString().replace("\n", "\n\t")
            )
        )
    }

    fun logStart(completionRequest: CompletionRequest) {
        if (completionRequest.suffix == null) {
            log(
                logLevel, String.format(
                    "Text Completion Request\nPrefix:\n\t%s\n",
                    completionRequest.prompt.replace("\n", "\n\t")
                )
            )
        } else {
            log(
                logLevel, String.format(
                    "Text Completion Request\nPrefix:\n\t%s\nSuffix:\n\t%s\n",
                    completionRequest.prompt.replace("\n", "\n\t"),
                    completionRequest.suffix!!.replace("\n", "\n\t")
                )
            )
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun post(url: String, json: String, retries: Int): String {
        return post(jsonRequest(url, json), retries)
    }

    fun post(
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

    fun jsonRequest(url: String, json: String): HttpPost {
        val request = HttpPost(url)
        request.addHeader("Content-Type", "application/json")
        request.addHeader("Accept", "application/json")
        authorize(request)
        request.entity = StringEntity(json)
        return request
    }

    @Throws(IOException::class)
    fun authorize(request: HttpRequestBase) {
        var apiKey: CharSequence = key
        if (apiKey.length == 0) {
            synchronized(OpenAI_API.javaClass) {
                apiKey = key
                if (apiKey.length == 0) {
                    apiKey = UITools.queryAPIKey()!!
                    key = apiKey.toString()
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

    fun text_to_speech(wavAudio: ByteArray, prompt: String = ""): String {
        val url = apiBase + "/audio/transcriptions"
        val request = HttpPost(url)
        request.addHeader("Accept", "application/json")
        authorize(request)
        val entity = MultipartEntityBuilder.create()
        entity.setMode(HttpMultipartMode.RFC6532)
        entity.addBinaryBody("file", wavAudio, ContentType.create("audio/x-wav"), "audio.wav")
        entity.addTextBody("model", "whisper-1")
        if (!prompt.isEmpty()) entity.addTextBody("prompt", prompt)
        request.entity = entity.build()
        val response = post(request, 3)
        val jsonObject = Gson().fromJson(response, JsonObject::class.java)
        if (jsonObject.has("error")) {
            val errorObject = jsonObject.getAsJsonObject("error")
            throw RuntimeException(IOException(errorObject["message"].asString))
        }
        return jsonObject.get("text").asString!!
    }

    fun text_to_image(prompt: String = "", resolution: Int = 1024, count: Int = 1): List<BufferedImage> {
        val url = apiBase + "/images/generations"
        val request = HttpPost(url)
        request.addHeader("Accept", "application/json")
        request.addHeader("Content-Type", "application/json")
        authorize(request)
        val jsonObject = JsonObject()
        jsonObject.addProperty("prompt", prompt)
        jsonObject.addProperty("n", count)
        jsonObject.addProperty("size", "${resolution}x$resolution")
        request.entity = StringEntity(jsonObject.toString())
        val response = post(request, 3)
        val jsonObject2 = Gson().fromJson(response, JsonObject::class.java)
        if (jsonObject2.has("error")) {
            val errorObject = jsonObject2.getAsJsonObject("error")
            throw RuntimeException(IOException(errorObject["message"].asString))
        }
        val dataArray = jsonObject2.getAsJsonArray("data")
        val images = ArrayList<BufferedImage>()
        for (i in 0 until dataArray.size()) {
            images.add(ImageIO.read(URL(dataArray[i].asJsonObject.get("url").asString)))
        }
        return images
    }

    @Throws(IOException::class)
    fun processCompletionResponse(result: String): CompletionResponse {
        checkError(result)
        val response = mapper.readValue(
            result,
            CompletionResponse::class.java
        )
        if (response.usage != null) {
            incrementTokens(response.usage!!.total_tokens)
        }
        return response
    }

    @Throws(IOException::class)
    fun processChatResponse(result: String): ChatResponse {
        checkError(result)
        val response = mapper.readValue(
            result,
            ChatResponse::class.java
        )
        if (response.usage != null) {
            incrementTokens(response.usage!!.total_tokens)
        }
        return response
    }

    private val maxTokenErrorMessage = Pattern.compile(
        """This model's maximum context length is (\d+) tokens. However, you requested (\d+) tokens \((\d+) in the messages, (\d+) in the completion\). Please reduce the length of the messages or completion."""
    )

    private fun checkError(result: String) {
        try {
            val jsonObject = Gson().fromJson(
                result,
                JsonObject::class.java
            )
            if (jsonObject.has("error")) {
                val errorObject = jsonObject.getAsJsonObject("error")
                val errorMessage = errorObject["message"].asString
                val matcher = maxTokenErrorMessage.matcher(errorMessage)
                if (matcher.find()) {
                    val modelMax = matcher.group(1).toInt()
                    val request = matcher.group(2).toInt()
                    val messages = matcher.group(3).toInt()
                    val completion = matcher.group(4).toInt()
                    throw ModelMaxException(modelMax, request, messages, completion)
                }
                throw IOException(errorMessage)
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            throw IOException("Invalid JSON response: $result")
        }
    }

    open fun incrementTokens(totalTokens: Int) {}

    companion object {
        val log = Logger.getInstance(CoreAPI::class.java)

        fun log(level: LogLevel, msg: String) {
            val message = msg.trim { it <= ' ' }.replace("\n", "\n\t")
            when (level) {
                LogLevel.Error -> log.error(message)
                LogLevel.Warn -> log.warn(message)
                LogLevel.Info -> log.info(message)
                else -> log.debug(message)
            }
        }
    }

    val allowedCharset = Charset.forName("ASCII")
    val mapper: ObjectMapper
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

    fun complete(
        completionRequest: CompletionRequest,
        model: String
    ): CompletionResponse {
        logStart(completionRequest)
        val completionResponse = try {
            val request: String =
                StringTools.restrictCharacterSet(
                    AsyncAPI.mapper.writeValueAsString(completionRequest),
                    allowedCharset
                )
            val result =
                post(apiBase + "/engines/" + model + "/completions", request)
            processCompletionResponse(result)
        } catch (e: ModelMaxException) {
            completionRequest.max_tokens = (e.modelMax - e.messages) - 1
            val request: String =
                StringTools.restrictCharacterSet(
                    AsyncAPI.mapper.writeValueAsString(completionRequest),
                    allowedCharset
                )
            val result =
                post(apiBase + "/engines/" + model + "/completions", request)
            processCompletionResponse(result)
        }
        val completionResult = StringTools.stripPrefix(
            completionResponse.firstChoice.orElse("").toString().trim { it <= ' ' },
            completionRequest.prompt.trim { it <= ' ' })
        logComplete(completionResult)
        return completionResponse
    }

    fun chat(
        completionRequest: ChatRequest
    ): ChatResponse {
        logStart(completionRequest)
        val url = apiBase + "/chat/completions"
        val completionResponse = try {
            val result = post(
                url, StringTools.restrictCharacterSet(
                    AsyncAPI.mapper.writeValueAsString(completionRequest),
                    allowedCharset
                )
            )
            processChatResponse(result)
        } catch (e: ModelMaxException) {
            completionRequest.max_tokens = (e.modelMax - e.messages) - 1
            val result = post(
                url, StringTools.restrictCharacterSet(
                    AsyncAPI.mapper.writeValueAsString(completionRequest),
                    allowedCharset
                )
            )
            processChatResponse(result)
        }
        val completionResult = completionResponse.choices.first().message!!.content!!.trim { it <= ' ' }
        logComplete(completionResult)
        return completionResponse
    }

    private fun logStart(completionRequest: ChatRequest) {
        log(
            logLevel, String.format(
                "Text Completion Request\nPrefix:\n\t%s\n",
                completionRequest.messages.map { it.content }.joinToString { "\n" }.replace("\n", "\n\t")
            )
        )
    }

    fun moderate(text: String) {
        val body: String = try {
            AsyncAPI.mapper.writeValueAsString(
                Map.of(
                    "input",
                    StringTools.restrictCharacterSet(text, allowedCharset)
                )
            )
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
        val result: String = try {
            this.post(apiBase + "/moderations", body)
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
        AsyncAPI.log(
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