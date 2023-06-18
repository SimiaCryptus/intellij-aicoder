package com.github.simiacryptus.aicoder.proxy

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.openai.proxy.CompletionProxy
import com.intellij.openapi.util.io.FileUtil
import com.simiacryptus.openai.OpenAIClient
import org.junit.Test
import org.slf4j.event.Level
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProxyTest {
    companion object {
        fun <T:Any> chatProxy(clazz : Class<T>,
                              apiLog: String = "api.${
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        }.log.json"): ChatProxy<T> = ChatProxy(
            clazz,
            api = OpenAIClient(
                key = OpenAIClient.keyTxt,
                apiBase = AppSettingsState.instance.apiBase,
                logLevel = Level.WARN
            ),
            model = AppSettingsState.instance.defaultChatModel(),
            apiLog = apiLog,
            deserializerRetries = 5
        )
        fun <T:Any> completionProxy(clazz : Class<T>,
                            apiLog: String = "api.log.json"): CompletionProxy<T> = CompletionProxy(
            clazz,
            apiKey = OpenAIClient.keyTxt,
            apiLog = apiLog,
            deserializerRetries = 5
        )
    }

    interface TestAPI {
        fun simpleQuestion(question: SimpleQuestion): SimpleResponse

        class SimpleQuestion(var question: String = "")

        class SimpleResponse {
            var answer: String = ""
            var why: String = ""
        }

        fun topTen(question: TopTenQuestion): TopTenResponse

        class TopTenQuestion(var question: String = "")

        class TopTenResponse {
            var answers: List<String> = listOf()
        }
    }

    interface EssayAPI {
        fun essayOutline(thesis: Thesis = Thesis(), essayLength: String = ""): EssayOutline

        class Thesis(var statement: String = "")

        class EssayOutline(
            var introduction: Introduction = Introduction(),
            var bodyParagraphs: List<BodyParagraph> = listOf(),
            var conclusion: Conclusion = Conclusion(),
            var topics: List<String> = listOf()
        )

        class Introduction(var thesis: Thesis = Thesis(""))

        class BodyParagraph(
            var topicSentence: TopicSentence = TopicSentence(),
            var supportingDetails: List<SupportingDetail> = listOf()
        )

        class TopicSentence(var sentence: String = "")

        class SupportingDetail(var detail: String = "") {
            fun examples(api: EssayAPI): List<SupportingDetail> {
                return api.findExamples(this)
            }
        }

        class Conclusion(var thesis: Thesis = Thesis())

        fun findExamples(paragraph: SupportingDetail = SupportingDetail()): List<SupportingDetail>
    }

    @Test
    fun test_essayOutline() {
        if (OpenAIClient.keyTxt.isBlank()) return
        //println(TestGPTInterfaceProxy().api.getEngines().joinToString("\n"))
        val statement = "The meaning of life is to live a life of meaning."
        for (proxyFactory in listOf(completionProxy(EssayAPI::class.java), chatProxy(EssayAPI::class.java))) {
            val proxy = proxyFactory.create()
            val essayOutline = proxy.essayOutline(
                EssayAPI.Thesis(statement), "5000 words"
            )
            println(essayOutline.introduction.thesis.statement)
        }
    }

    @Test
    fun test_simpleQuestion() {
        if (OpenAIClient.keyTxt.isBlank()) return
        val question = "What is the meaning of life?"
        for (proxyFactory in listOf(completionProxy(TestAPI::class.java), chatProxy(TestAPI::class.java))) {
            val proxy = proxyFactory.create()
            println(proxy.simpleQuestion(TestAPI.SimpleQuestion(question)).answer)
        }
    }


    @Test
    fun test_topTen() {
        if (OpenAIClient.keyTxt.isBlank()) return
        val proxy = chatProxy(TestAPI::class.java).create()
        println(proxy.topTen(TestAPI.TopTenQuestion()).answers.joinToString("\n"))
    }


}