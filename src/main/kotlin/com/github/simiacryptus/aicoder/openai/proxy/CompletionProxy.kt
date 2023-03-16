package com.github.simiacryptus.aicoder.openai.proxy

import com.github.simiacryptus.aicoder.openai.core.CompletionRequest
import com.github.simiacryptus.aicoder.openai.core.CoreAPI
import com.jetbrains.rd.util.LogLevel

class CompletionProxy(
    apiKey: String,
    private val model: String = "text-davinci-003",
    private val maxTokens: Int = 1000,
    private val temperature: Double = 0.7,
    private val verbose: Boolean = false,
    base: String = "https://api.openai.com/v1",
    apiLog: String
) : GPTProxyBase(apiLog) {
    val api: CoreAPI

    init {
        api = CoreAPI(base, apiKey, LogLevel.Debug)
    }

    override fun complete(prompt: ProxyRequest, vararg examples: ProxyRecord): String {
        if(verbose) println(prompt)
        val request = CompletionRequest()
        val argList = prompt.argList
        val methodName = prompt.methodName
        val responseType = prompt.responseType
        request.prompt = """
        Method: $methodName
        Response Type: 
            ${responseType.replace("\n", "\n            ")}
        Request: 
            {
                ${argList.entries.joinToString(",\n", transform = { (argName, argValue) ->
                    """"$argName": $argValue"""
                }).replace("\n", "\n                ")}
            }
        Response:
            {""".trimIndent()
        request.max_tokens = maxTokens
        request.temperature = temperature
        val completion = api.complete(request, model).firstChoice.get().toString()
        if(verbose) println(completion)
        return "{$completion"
    }
}