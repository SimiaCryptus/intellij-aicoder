package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * ComicBook creates a comic book from an idea.
 */
class ComicBook: GenerationReportBase() {
    interface Comic {

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

        fun getStory(
            characters: CharacterList,
            plot: String,
            setting: String,
            pageCount: Int = 10
        ): Story

        data class Story(
            val title: String = "",
            val author: String = "",
            val characters: List<String> = listOf(),
            val plot: String = "",
            val setting: String = "",
            val pages: List<Page> = listOf(),
        )

        data class Page(
            val image: ImageDescription? = null,
            val text: String = "",
        )

    }

    @Test
    fun comicBook() {
        runReport("ComicBook", Comic::class) { api, logJson, out ->
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
            val story = api.getStory(
                characters = characters,
                plot = "The characters must save the world from an alien invasion.",
                setting = "A post-apocalyptic world.",
            )
            logJson(story)
            out(
                """
                |
                |# ${story.title}
                |
                |Author: ${story.author}
                |
                |Characters: ${story.characters.joinToString(", ")}
                |
                |Plot: ${story.plot}
                |
                |Setting: ${story.setting}
                |
                |""".trimMargin()
            )
            for (page in story.pages) {
                logJson(page)
                out(
                    """
                    |
                    |## Page ${story.pages.indexOf(page) + 1}
                    |
                    |![${page.image!!.detailedCaption}](${
                        writeImage(
                            proxy.api.text_to_image(
                                page.image.detailedCaption,
                                resolution = 512
                            )[0]
                        )
                    })
                    |
                    |${page.text}
                    |
                    |""".trimMargin()
                )
            }
        }
    }
}