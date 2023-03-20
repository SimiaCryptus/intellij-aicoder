package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * Simulate several participants debating a topic with a moderator
 */
class DebateSimulator : GenerationReportBase() {

    @Test
    fun judgeDebate() {
        runReport("Debate", Debate::class) { api, logJson, out ->
            val debate = api.newRandomDebate(
                topic = "What is the best way to solve a problem?",
                participantNames = listOf("Socrates", "Buddha", "Jesus", "Confucius", "Nietzsche"),
            )
            logJson(debate)
            for (question in debate.questions) {
                val argument = api.poseQuestion(debate, question)
                val dialog = listOf(argument).toMutableList()
                logJson(argument)
                api.writeArgumentText(debate, argument).let { spokenText ->
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
                        api.rebuttal(debate, question, speaker, Debate.DebateArguments(dialog.takeLast(1)))
                    logJson(rebuttal)
                    api.writeArgumentText(debate, rebuttal).let { spokenText ->
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
                logJson(api.judgeDebate(debate, Debate.DebateArguments(dialog)))
            }
        }
    }

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

        /**
        *
        *  A data class representing the judgement of a debate.
        *
        *  @property winner the team judged to have won the debate
        *  @property reasoning the judge's reasoning for the judgement
        *  @property pointsAwarded the number of points awarded to the winner
        */
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

}

