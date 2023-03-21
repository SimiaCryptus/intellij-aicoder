package com.github.simiacryptus.openai.proxy

import com.github.simiacryptus.openai.CompletionRequest
import com.github.simiacryptus.openai.OpenAIClient
import com.jetbrains.rd.util.LogLevel

class CompletionProxy(
    apiKey: String,
    private val model: String = "text-davinci-003",
    private val maxTokens: Int = 4000,
    private val temperature: Double = 0.7,
    private val verbose: Boolean = false,
    private val moderated: Boolean = true,
    base: String = "https://api.openai.com/v1",
    apiLog: String
) : GPTProxyBase(apiLog, 3) {
    val api: OpenAIClient

    init {
        api = OpenAIClient(base, apiKey, LogLevel.Debug)
    }

    override fun complete(prompt: ProxyRequest, vararg examples: ProxyRecord): String {
        if(verbose) println(prompt)
        val request = CompletionRequest()
        request.prompt = """
        |Method: ${prompt.methodName}
        |Response Type: 
        |    ${prompt.apiYaml.replace("\n", "\n            ")}
        |Request: 
        |    {
        |        ${
            prompt.argList.entries.joinToString(",\n", transform = { (argName, argValue) ->
                """"$argName": $argValue"""
            }).replace("\n", "\n                ")
        }
        |    }
        |Response:
        |    {""".trim().trimIndent()
        request.max_tokens = maxTokens
        request.temperature = temperature
        if (moderated) api.moderate(toJson(request))
        val completion = api.complete(request, model).firstChoice.get().toString()
        if(verbose) println(completion)
        return "{$completion"
    }
}