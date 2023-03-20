package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * Create an entertaining fictional story with several characters and a plot, with a twist ending and a moral
 */
class ShortStory : GenerationReportBase() {
    interface Story {

        fun getCharacters(characterCount: Int = 5): CharacterList

        data class CharacterList(
            val characters: List<Character> = listOf(),
        )

        data class Character(
            val name: String = "",
            val age: Int = 0,
            val occupation: String = "",
            val description: String = "",
            val traits: List<String> = listOf(),
            val goals: List<String> = listOf(),
            val secrets: List<String> = listOf(),
        )

        fun getPlot(
            characters: List<Character> = listOf(),
            setting: String = "",
            plotPoints: Int = 5
        ): Plot

        data class Plot(
            val characters: List<Character> = listOf(),
            val setting: String = "",
            val plotPoints: List<String> = listOf(),
        )

        fun getTwistEnding(plot: Plot): String

        fun getMoral(plot: Plot): String
    }

    @Test
    fun shortStory() {
        runReport("Short Story", Story::class) { api, logJson, out ->
            val characters = api.getCharacters()
            logJson(characters)
            out(
                """
                            |
                            |# Characters
                            |
                            |""".trimMargin()
            )
            for (character in characters.characters) {
                out(
                    """
                            |
                            |## ${character.name}
                            |
                            |Age: ${character.age}
                            |
                            |Occupation: ${character.occupation}
                            |
                            |Description: ${character.description}
                            |
                            |Traits: ${character.traits.joinToString(", ")}
                            |
                            |Goals: ${character.goals.joinToString(", ")}
                            |
                            |Secrets: ${character.secrets.joinToString(", ")}
                            |
                            |""".trimMargin()
                )
            }
            val setting = "A small town in the middle of nowhere"
            out(
                """
                            |
                            |# Plot
                            |
                            |Setting: $setting
                            |
                            |""".trimMargin()
            )
            val plot = api.getPlot(characters.characters, setting)
            logJson(plot)
            for (plotPoint in plot.plotPoints) {
                out(
                    """
                            |
                            |* $plotPoint
                            |
                            |""".trimMargin()
                )
            }
            val twistEnding = api.getTwistEnding(plot)
            out(
                """
                            |
                            |# Twist Ending
                            |
                            |$twistEnding
                            |
                            |""".trimMargin()
            )
            val moral = api.getMoral(plot)
            out(
                """
                            |
                            |# Moral
                            |
                            |$moral
                            |
                            |""".trimMargin()
            )
        }
    }
}