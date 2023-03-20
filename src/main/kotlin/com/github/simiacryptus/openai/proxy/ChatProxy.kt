package com.github.simiacryptus.openai.proxy

import com.github.simiacryptus.openai.ChatRequest
import com.github.simiacryptus.openai.ChatMessage
import com.github.simiacryptus.openai.CoreAPI
import com.jetbrains.rd.util.LogLevel
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@Suppress("MemberVisibilityCanBePrivate")
class ChatProxy(
    apiKey: String,
    private val model: String = "gpt-3.5-turbo",
    private val maxTokens: Int = 3500,
    private val temperature: Double = 0.7,
    private val verbose: Boolean = false,
    private val moderated: Boolean = true,
    base: String = "https://api.openai.com/v1",
    apiLog: String? = null,
    logLevel: LogLevel
) : GPTProxyBase(apiLog, 3) {
    val api: CoreAPI
    val totalPrefixLength = AtomicInteger(0)
    val totalSuffixLength = AtomicInteger(0)
    val totalInputLength = AtomicInteger(0)
    val totalOutputLength = AtomicInteger(0)

    init {
        api = CoreAPI(base, apiKey, logLevel)
    }

    override fun complete(prompt: ProxyRequest, vararg examples: ProxyRecord): String {
        if (verbose) println(prompt)
        val request = ChatRequest()
        request.messages = (
                listOf(
                    ChatMessage(
                        ChatMessage.Role.system, """
                |You are a JSON-RPC Service serving the following method:
                |${prompt.methodName}
                |Requests contain the following arguments:
                |${prompt.argList.keys.joinToString("\n  ")}
                |Responses are of type:
                |${prompt.responseType}
                |Responses are expected to be a single JSON object without explaining text.
                |All input arguments are optional
                |""".trimMargin().trim()
                    )
                ) +
                        examples.flatMap {
                            listOf(
                                ChatMessage(
                                    ChatMessage.Role.user,
                                    argsToString(it.argList)
                                ),
                                ChatMessage(
                                    ChatMessage.Role.assistant,
                                    it.response
                                )
                            )
                        } +
                        listOf(
                            ChatMessage(
                                ChatMessage.Role.user,
                                argsToString(prompt.argList)
                            )
                        )
                ).toTypedArray()
        request.model = model
        request.max_tokens = maxTokens
        request.temperature = temperature
        val completion = api.withTimeout(Duration.ofMinutes(10)) {
            val json = toJson(request)
            if (moderated) api.moderate(json)
            totalInputLength.addAndGet(json.length)
            api.chat(request).response.get().toString()
        }
        if (verbose) println(completion)
        totalOutputLength.addAndGet(completion.length)
        val trimPrefix = trimPrefix(completion)
        val trimSuffix = trimSuffix(trimPrefix.first)
        totalPrefixLength.addAndGet(trimPrefix.second.length)
        totalSuffixLength.addAndGet(trimSuffix.second.length)
        return trimSuffix.first
    }

    companion object {
        private fun trimPrefix(completion: String): Pair<String, String> {
            val start = completion.indexOf('{').coerceAtMost(completion.indexOf('['))
            if (start < 0) {
                return completion to ""
            } else {
                val substring = completion.substring(start)
                return substring to completion.substring(0, start)
            }
        }

        private fun trimSuffix(completion: String): Pair<String, String> {
            val end = completion.lastIndexOf('}').coerceAtLeast(completion.lastIndexOf(']'))
            if (end < 0) {
                return completion to ""
            } else {
                val substring = completion.substring(0, end + 1)
                return substring to completion.substring(end + 1)
            }
        }

        private fun argsToString(argList: Map<String, String>) =
            "{" + argList.entries.joinToString(",\n", transform = { (argName, argValue) ->
                """"$argName": $argValue"""
            }) + "}"
    }
}