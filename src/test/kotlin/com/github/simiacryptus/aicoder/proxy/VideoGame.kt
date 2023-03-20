package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * VideoGame creates a video game from an idea.
 */
class VideoGame : GenerationReportBase() {
    interface Game {

        fun getCharacters(
            characterCount: Int = 3,
            gender: String? = null,
            age: Int? = null,
            species: String? = null
        ): CharacterList

        data class CharacterList(
            val characters: List<Character> = listOf(),
        )

        data class Character(
            val name: String = "",
            val gender: String = "",
            val age: Int = 0,
            val species: String = "",
            val description: String = "",
            val image: ImageDescription? = null,
        )

        data class ImageDescription(
            val style: String = "",
            val subject: String = "",
            val background: String = "",
            val detailedCaption: String = "",
        )

        fun getGame(
            characters: CharacterList,
            plot: String,
            setting: String,
            levelCount: Int = 10
        ): Game

        data class Game(
            val title: String = "",
            val author: String = "",
            val characters: List<String> = listOf(),
            val plot: String = "",
            val setting: String = "",
            val levels: List<Level> = listOf(),
        )

        data class Level(
            val image: ImageDescription? = null,
            val description: String = "",
            val objectives: List<Objective> = listOf(),
        )

        data class Objective(
            val description: String = "",
            val reward: String = "",
        )

    }

    @Test
    fun videoGame() {
        runReport("Video Game", Game::class) { api, logJson, out ->
            val characters = api.getCharacters(
                characterCount = 3,
                gender = "male",
                age = 20,
                species = "human"
            )
            logJson(characters)
            out(
                """
                |
                |# Characters
                |
                |""".trimMargin()
            )
            for (character in characters.characters) {
                logJson(character)
                out(
                    """
                    |
                    |## ${character.name}
                    |
                    |![${character.image!!.detailedCaption}](${
                        writeImage(
                            proxy.api.text_to_image(
                                character.image.detailedCaption,
                                resolution = 512
                            )[0]
                        )
                    })
                    |
                    |Gender: ${character.gender}
                    |
                    |Age: ${character.age}
                    |
                    |Species: ${character.species}
                    |
                    |${character.description}
                    |
                    |""".trimMargin()
                )
            }
            val game = api.getGame(
                characters = characters,
                plot = "The characters must save the world from an alien invasion.",
                setting = "A post-apocalyptic world.",
            )
            logJson(game)
            out(
                """
                |
                |# ${game.title}
                |
                |Author: ${game.author}
                |
                |Characters: ${game.characters.joinToString(", ")}
                |
                |Plot: ${game.plot}
                |
                |Setting: ${game.setting}
                |
                |""".trimMargin()
            )
            for (level in game.levels) {
                logJson(level)
                out(
                    """
                    |
                    |## Level ${game.levels.indexOf(level) + 1}
                    |
                    |![${level.image!!.detailedCaption}](${
                        writeImage(
                            proxy.api.text_to_image(
                                level.image.detailedCaption,
                                resolution = 512
                            )[0]
                        )
                    })
                    |
                    |${level.description}
                    |
                    |Objectives:
                    |
                    |""".trimMargin()
                )
                for (objective in level.objectives) {
                    logJson(objective)
                    out(
                        """
                        |
                        |- ${objective.description} (Reward: ${objective.reward})
                        |
                        |""".trimMargin()
                    )
                }
            }
        }
    }
}