package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * Simulate several participants debating a topic with a moderator
 *
 */
class DebateSimulator : GenerationReportBase<DebateSimulator.Debate>(Debate::class) {

    @Test
    fun judgeDebate() {
        runReport("Debate") { api, logJson, out ->
            proxy.model = "gpt-4-0314"
            proxy.temperature = 0.5
            val debate = api.newRandomDebate(
                topic = "What is the best way to solve a problem?",
                participantNames = listOf("Socrates", "Buddha", "Jesus", "Confucius", "Nietzsche"),
            )
            logJson(debate)
            val pointsAwarded = HashMap<String, Double>()
            for (question in debate.questions) {

                out(
                    """
                    |
                    |## $question
                    |
                    |""".trimMargin()
                )

                val arguments = api.poseQuestion(question, debate.participants.shuffled())
                logJson(arguments)
                for (argument in arguments.lines) {
                    api.writeArgumentText(debate, argument).let { spokenText ->
                        out(
                            """
                            |
                            |### ${spokenText.speaker}
                            |
                            |${spokenText.text}
                            |
                            |""".trimMargin()
                        )
                        logJson(spokenText)
                    }
                }
                val judgement = api.judgeDebate(debate, arguments)
                judgement.pointsAwarded.forEach { (name, points) ->
                    pointsAwarded[name] = (pointsAwarded[name] ?: 0.0) + points
                }
                logJson(judgement)
            }
            logJson(pointsAwarded)
            val winner = pointsAwarded.maxByOrNull { it.value }?.key
            out(
                """
                |
                |Winner: $winner
                |
                |""".trimMargin()
            )
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

        fun poseQuestion(question: String, speakers: List<DebateParticipant>): DebateArguments
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

        data class DebateJudgement(
            val winner: String = "", val reasoning: String = "", val pointsAwarded: Map<String, Double> = mapOf()
        )

        fun judgeDebate(debate: DebateSummary, dialog: DebateArguments): DebateJudgement

    }

}

