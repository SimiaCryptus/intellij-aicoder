package com.github.simiacryptus.aicoder.proxy

import com.simiacryptus.openai.proxy.Description
import org.junit.Test

class PersonalityTest : GenerationReportBase<PersonalityTest.PersonalityTest>(PersonalityTest::class) {

    interface PersonalityTest {

        data class PersonalityAttributeDelta(
            var traits: Map<String, PersonalityValue> = mapOf(),
        )

        data class PersonalityValue(
            @Description("The numerical change in the attribute")
            var delta: Double = 0.0,
            var explanation: String = "",
        )

        data class PersonalityQuestion(
            var question: String = "",
            var answers: List<PersonalityAnswer> = listOf(),
        )

        data class PersonalityAnswer(
            var answer: String = "",
            var delta: PersonalityAttributeDelta = PersonalityAttributeDelta(),
        )

        data class PersonalityQuestionnaire(
            var questions: List<PersonalityQuestion> = listOf(),
        )

        data class PersonalityQuestionAnswer(
            var question: String = "",
            var answer: String = "",
        )

        fun createQuestionnaire(title: String, numberOfQuestions: Int = 20): PersonalityQuestionnaire

        fun evaluateQuestionnaire(answers: List<PersonalityQuestionAnswer>): PersonalityTestResult

        data class PersonalityTestResult(
            var text: String = "",
        )

    }

    @Test
    fun test() {
        runReport("PersonalityTest") { api, logJson, out ->
            proxy.temperature = 0.1
            val questionnaire = api.createQuestionnaire("Who are you?")
            questionnaire.questions.forEach { question ->
                logJson(question)
            }

        }
    }


}