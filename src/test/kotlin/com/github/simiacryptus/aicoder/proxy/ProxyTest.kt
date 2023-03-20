package com.github.simiacryptus.aicoder.proxy

import com.github.simiacryptus.openai.proxy.ChatProxy
import com.github.simiacryptus.openai.proxy.CompletionProxy
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rd.util.LogLevel
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProxyTest {
    companion object {
        val keyFile = File("C:\\Users\\andre\\code\\all-projects\\openai.key")
        fun chatProxy(apiLog: String = "api.${
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        }.log.json"): ChatProxy = ChatProxy(
            apiKey = FileUtil.loadFile(keyFile).trim(),
            apiLog = apiLog,
            logLevel = LogLevel.Warn,
            model = "gpt-3.5-turbo-0301",
            //model = "gpt-4-0314",
            maxTokens = 8912
        )
        fun completionProxy(apiLog: String = "api.log.json"): CompletionProxy = CompletionProxy(
            apiKey = FileUtil.loadFile(keyFile).trim(),
            apiLog = apiLog
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
        if (!keyFile.exists()) return
        //println(TestGPTInterfaceProxy().api.getEngines().joinToString("\n"))
        val statement = "The meaning of life is to live a life of meaning."
        for (proxyFactory in listOf(completionProxy(), chatProxy())) {
            val proxy = proxyFactory.create(EssayAPI::class.java)
            val essayOutline = proxy.essayOutline(
                EssayAPI.Thesis(statement), "5000 words"
            )
            println(essayOutline.introduction!!.thesis.statement)
        }
    }

    @Test
    fun test_simpleQuestion() {
        if (!keyFile.exists()) return
        val question = "What is the meaning of life?"
        for (proxyFactory in listOf(completionProxy(), chatProxy())) {
            val proxy = proxyFactory.create(TestAPI::class.java)
            println(proxy.simpleQuestion(TestAPI.SimpleQuestion(question)).answer)
        }
    }


    @Test
    fun test_topTen() {
        if (!keyFile.exists()) return
        val proxy = chatProxy().create(TestAPI::class.java)
        println(proxy.topTen(TestAPI.TopTenQuestion()).answers.joinToString("\n"))
    }


}