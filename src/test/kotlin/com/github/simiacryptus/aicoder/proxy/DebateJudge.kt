package com.github.simiacryptus.aicoder.proxy

import org.junit.Test
import java.io.BufferedWriter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DebateJudge {
    interface Debate {
        fun newRandomDebate(
            participantNames: List<String> = listOf(""),
            topic: String = "",
            questionCount: Int = 5,
        ): DebateSummary

        data class DebateSummary(
            val topic: String = "",
            val participants: List<DebateParticipant> = listOf(),
            val questions: List<String> = listOf()
        )

        data class DebateParticipant(
            val name: String = "",
            val positions: List<String> = listOf(),
            val writingStyle: String = "",
        )

        data class DebateJudgement(
            val winner: String = "", val reasoning: String = "", val pointsAwarded: Int = 0
        )

        fun judgeDebate(debate: DebateSummary, dialog: DebateArguments): DebateJudgement
        fun poseQuestion(debate: DebateSummary, question: String): DebateArgument
        fun rebuttal(
            debate: DebateSummary, question: String, speaker: String, argument: DebateArguments
        ): DebateArgument

        data class DebateArguments(
            val lines: List<DebateArgument> = listOf()
        )

        data class DebateArgument(
            val speaker: String = "",
            val summary: String = "",
            val supportingDetails: List<String> = listOf(),
        )

        fun writeArgumentText(debate: DebateSummary, argument: DebateArgument): ArgumentText

        data class ArgumentText(
            val speaker: String = "",
            val text: String = "",
        )
    }

    @Test
    fun judgeDebate() {
        if (!ProxyTest.keyFile.exists()) return
        val outputDir = File(".")
        val markdownOutputFile = File(
            outputDir, "Debate_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))}.md"
        )
        BufferedWriter(markdownOutputFile.writer()).use { writer ->
            fun out(s: Any) {
                println(s.toString())
                writer.write(s.toString())
                writer.newLine()
            }

            val proxy = ProxyTest.chatProxy()
            fun logJson(
                obj: Any
            ) {
                out(
                    """
                    |```json
                    |${proxy.toJson(obj)}
                    |```
                    |""".trimMargin()
                )
            }

            val file = File("debate.examples.json")
            if (file.exists()) proxy.addExamples(file)
            val debateApi = proxy.create(Debate::class.java)
            val debate = debateApi.newRandomDebate(
//                topic = "What is the secret to a happy life?",
                topic = "What is the best way to solve a problem?",
                participantNames = listOf("Socrates", "Buddha", "Jesus", "Confucius", "Nietzsche"),
            )
            logJson(debate)
            for (question in debate.questions) {
                val argument = debateApi.poseQuestion(debate, question)
                val dialog = listOf(argument).toMutableList()
                logJson(argument)
                debateApi.writeArgumentText(debate, argument).let { spokenText ->
                    out(
                        """
                        |
                        |${spokenText.speaker}: ${spokenText.text}
                        |
                        |""".trimMargin()
                    )
                }
                debate.participants.map { it.name }.filter { it != argument.speaker }.shuffled().forEach { speaker ->
                    val rebuttal =
                        debateApi.rebuttal(debate, question, speaker, Debate.DebateArguments(dialog.takeLast(1)))
                    logJson(rebuttal)
                    debateApi.writeArgumentText(debate, rebuttal).let { spokenText ->
                        out(
                            """
                            |
                            |${spokenText.speaker}: ${spokenText.text}
                            |
                            |""".trimMargin()
                        )
                    }
                    dialog.add(rebuttal)
                }
                logJson(debateApi.judgeDebate(debate, Debate.DebateArguments(dialog)))
            }
        }
    }

}


