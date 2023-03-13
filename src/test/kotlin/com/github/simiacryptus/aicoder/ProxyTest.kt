package com.github.simiacryptus.aicoder

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.CompletionRequest
import com.github.simiacryptus.aicoder.openai.OpenAI_API
import com.github.simiacryptus.aicoder.openai.translate.GPTInterfaceProxy
import com.intellij.openapi.util.io.FileUtil
import org.junit.Test
import java.io.File


class ProxyTest {

    interface TestAPI {
        fun simpleQuestion(question: SimpleQuestion): SimpleResponse

        class SimpleQuestion(var question: String = "What is the answer to life, the universe, and everything?")

        class SimpleResponse {
            var answer: String = "42"
            var why: String = "Because it is the answer to life, the universe, and everything."
        }

        fun topTen(question: TopTenQuestion): TopTenResponse

        class TopTenQuestion(var question: String = "What are the top ten answers to life, the universe, and everything?")

        class TopTenResponse {
            var answers: List<String> = listOf(
                "42",
                "Love",
                "Happiness",
                "Peace",
                "Friendship",
                "Family",
                "Kindness",
                "Compassion",
                "Understanding",
                "Knowledge"
            )
        }

    }

    @Test
    fun test_simpleQuestion() {
        if (!keyFile.exists()) return
        val proxyFactory = TestGPTInterfaceProxy()
        val proxy = proxyFactory.proxy(TestAPI::class.java)
        println(proxy.simpleQuestion(TestAPI.SimpleQuestion()).answer)
    }

    @Test
    fun test_topTen() {
        if (!keyFile.exists()) return
        val proxyFactory = TestGPTInterfaceProxy()
        val proxy = proxyFactory.proxy(TestAPI::class.java)
        println(proxy.topTen(TestAPI.TopTenQuestion()).answers.joinToString("\n"))
    }

    companion object {
        val keyFile = File("C:\\Users\\andre\\code\\all-projects\\openai.key")

        class TestGPTInterfaceProxy : GPTInterfaceProxy() {
            init {
                val settings = AppSettingsState()
                settings.apiKey = FileUtil.loadFile(keyFile).trim()
                OpenAI_API.lastFetchedSettingsState = Long.MAX_VALUE
                OpenAI_API.settings = settings
            }

            override fun complete(prompt: String): String {
                println(prompt)
                val request = CompletionRequest().appendPrompt(prompt)
                request.max_tokens = 1000
                request.temperature = 0.7
                val completion = OpenAI_API.complete(null, request, "").get().toString()
                println(completion)
                return completion
            }
        }

    }
}