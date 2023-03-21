package com.github.simiacryptus.openai

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.simiacryptus.aicoder.openai.ui.OpenAI_API
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.util.StringTools
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
import org.apache.http.util.EntityUtils
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import java.util.Map
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.first
import kotlin.collections.set

@Suppress("unused")
open class OpenAIClient(
    private val apiBase: String,
    var key: String,
    private val logLevel: LogLevel
) : HttpClientManager() {

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

    private fun logComplete(completionResult: CharSequence) {
        log(
            logLevel, String.format(
                "Chat Completion:\n\t%s",
                completionResult.toString().replace("\n", "\n\t")
            )
        )
    }

    private fun logStart(completionRequest: CompletionRequest) {
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
    protected fun post(url: String, json: String): String {
        return post(jsonRequest(url, json))
    }

    private fun post(
        request: HttpPost
    ): String {
        val client = getClient()
        try {
            client.use { httpClient ->
                synchronized(clients) {
                    clients[Thread.currentThread()] = httpClient
                }
                val response: HttpResponse = httpClient.execute(request)
                val entity = response.entity
                return EntityUtils.toString(entity)
            }
        } finally {
            synchronized(clients) {
                clients.remove(Thread.currentThread())
            }
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

    @Throws(IOException::class)
    protected fun authorize(request: HttpRequestBase) {
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
    protected operator fun get(url: String?): String {
        val client = getClient()
        val request = HttpGet(url)
        request.addHeader("Content-Type", "application/json")
        request.addHeader("Accept", "application/json")
        authorize(request)
        client.use { httpClient ->
            val response: HttpResponse = httpClient.execute(request)
            val entity = response.entity
            return EntityUtils.toString(entity)
        }
    }

    fun dictate(wavAudio: ByteArray, prompt: String = ""): String = withReliability {
        withPerformanceLogging {
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
            val response = post(request)
            val jsonObject = Gson().fromJson(response, JsonObject::class.java)
            if (jsonObject.has("error")) {
                val errorObject = jsonObject.getAsJsonObject("error")
                throw RuntimeException(IOException(errorObject["message"].asString))
            }
            jsonObject.get("text").asString!!
        }
    }

    fun render(prompt: String = "", resolution: Int = 1024, count: Int = 1): List<BufferedImage> = withReliability {
        withPerformanceLogging {
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
        val response = post(request)
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
        images
    } }

    @Throws(IOException::class)
    private fun processCompletionResponse(result: String): CompletionResponse {
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
    protected fun processChatResponse(result: String): ChatResponse {
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

    private fun checkError(result: String) {
        try {
            val jsonObject = Gson().fromJson(
                result,
                JsonObject::class.java
            )
            if (jsonObject.has("error")) {
                val errorObject = jsonObject.getAsJsonObject("error")
                val errorMessage = errorObject["message"].asString
                if (errorMessage.startsWith("That model is currently overloaded with other requests.")) {
                    throw RequestOverloadException(errorMessage)
                }
                maxTokenErrorMessage.find { it.matcher(errorMessage).matches() }?.let {
                    val matcher = it.matcher(errorMessage)
                    if (matcher.find()) {
                        val modelMax = matcher.group(1).toInt()
                        val request = matcher.group(2).toInt()
                        val messages = matcher.group(3).toInt()
                        val completion = matcher.group(4).toInt()
                        throw ModelMaxException(modelMax, request, messages, completion)
                    }
                }
                throw IOException(errorMessage)
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            throw IOException("Invalid JSON response: $result")
        }
    }

    class RequestOverloadException(message: String = "That model is currently overloaded with other requests.") :
        IOException(message)

    open fun incrementTokens(totalTokens: Int) {}

    companion object {
        val log = Logger.getInstance(OpenAIClient::class.java)

        fun log(level: LogLevel, msg: String) {
            val message = msg.trim { it <= ' ' }.replace("\n", "\n\t")
            when (level) {
                LogLevel.Error -> log.error(message)
                LogLevel.Warn -> log.warn(message)
                LogLevel.Info -> log.info(message)
                else -> log.debug(message)
            }
        }

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
        val allowedCharset = Charset.forName("ASCII")
        private val maxTokenErrorMessage = listOf(
            Pattern.compile(
                """This model's maximum context length is (\d+) tokens. However, you requested (\d+) tokens \((\d+) in the messages, (\d+) in the completion\).*"""
            ),
            // This model's maximum context length is 4097 tokens, however you requested 80052 tokens (52 in your prompt; 80000 for the completion). Please reduce your prompt; or completion length.
            Pattern.compile(
                """This model's maximum context length is (\d+) tokens, however you requested (\d+) tokens \((\d+) in your prompt; (\d+) for the completion\).*"""
            )
        )

    }

    fun complete(
        completionRequest: CompletionRequest,
        model: String
    ): CompletionResponse = withReliability {
        withPerformanceLogging {
            logStart(completionRequest)
            val completionResponse = try {
                val request: String =
                    StringTools.restrictCharacterSet(
                        mapper.writeValueAsString(completionRequest),
                        allowedCharset
                    )
                val result =
                    post(apiBase + "/engines/" + model + "/completions", request)
                processCompletionResponse(result)
            } catch (e: ModelMaxException) {
                completionRequest.max_tokens = (e.modelMax - e.messages) - 1
                val request: String =
                    StringTools.restrictCharacterSet(
                        mapper.writeValueAsString(completionRequest),
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
            completionResponse
        }
    }

    fun chat(
        completionRequest: ChatRequest
    ): ChatResponse = withReliability {
        withPerformanceLogging {
            logStart(completionRequest)
            val url = apiBase + "/chat/completions"
            val completionResponse = try {
                processChatResponse(
                    post(
                        url, StringTools.restrictCharacterSet(
                            mapper.writeValueAsString(completionRequest),
                            allowedCharset
                        )
                    )
                )
            } catch (e: ModelMaxException) {
                completionRequest.max_tokens = (e.modelMax - e.messages) - 1
                processChatResponse(
                    post(
                        url, StringTools.restrictCharacterSet(
                            mapper.writeValueAsString(completionRequest),
                            allowedCharset
                        )
                    )
                )
            }
            logComplete(completionResponse.choices.first().message!!.content!!.trim { it <= ' ' })
            completionResponse
        }
    }

    private fun logStart(completionRequest: ChatRequest) {
        log(
            logLevel, String.format(
                "Chat Request\nPrefix:\n\t%s\n",
                mapper.writeValueAsString(completionRequest).replace("\n", "\n\t")
            )
        )
    }

    fun moderate(text: String) = withReliability {
        withPerformanceLogging {
            val body: String = try {
                mapper.writeValueAsString(
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

    fun edit(
        editRequest: EditRequest
    ): CompletionResponse = withReliability {
        withPerformanceLogging {
            logStart(editRequest, logLevel)
            val request: String =
                StringTools.restrictCharacterSet(
                    OpenAIClient.mapper.writeValueAsString(editRequest),
                    allowedCharset
                )
            val result = post(apiBase + "/edits", request)
            val completionResponse = processCompletionResponse(result)
            logComplete(
                completionResponse.firstChoice.orElse("").toString().trim { it <= ' ' }
            )
            completionResponse
        }
    }

    private fun logStart(
        editRequest: EditRequest,
        level: LogLevel
    ) {
        if (editRequest.input == null) {
            log(
                level, String.format(
                    "Text Edit Request\nInstruction:\n\t%s\n",
                    editRequest.instruction.replace("\n", "\n\t")
                )
            )
        } else {
            log(
                level, String.format(
                    "Text Edit Request\nInstruction:\n\t%s\nInput:\n\t%s\n",
                    editRequest.instruction.replace("\n", "\n\t"),
                    editRequest.input!!.replace("\n", "\n\t")
                )
            )
        }
    }

}