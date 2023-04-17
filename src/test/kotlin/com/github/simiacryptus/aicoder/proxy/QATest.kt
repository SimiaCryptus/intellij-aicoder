package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

class QATest : GenerationReportBase<QATest.QATest>(QATest::class) {

    interface QATest {
        fun simpleQuestion(question: SimpleQuestion): SimpleResponse

        class SimpleQuestion(var question: String = "")

        class SimpleResponse {
            var answer: String = ""
            var why: String = ""
        }
    }

    @Test
    fun test_question() {
        runReport("", { api, logJson, out ->
            out(api.simpleQuestion(QATest.SimpleQuestion("What is the meaning of life?")).answer)
        })
    }


}