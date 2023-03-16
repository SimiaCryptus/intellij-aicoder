package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

class QATest {

    interface QA {
        fun simpleQuestion(question: SimpleQuestion): SimpleResponse

        class SimpleQuestion(var question: String = "")

        class SimpleResponse {
            var answer: String = ""
            var why: String = ""
        }
    }

    @Test
    fun test_question() {
        if (!ProxyTest.keyFile.exists()) return
        val proxy = ProxyTest.completionProxy().create(QA::class.java)
        println(proxy.simpleQuestion(QA.SimpleQuestion("What is the meaning of life?")).answer)
    }


}