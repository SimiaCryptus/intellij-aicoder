package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * ChildrenStory creates an illustrated story for children.
 * It first generates a plot, consisting of a protagonist, a setting, and a conflict.
 * It teaches a lesson about the theme.
 * It then generates a cast of characters, and some plot twists.
 * Finally, it generates a series of illustrations, one for each page, along with the text for each page.
 */
class ChildrensStory : GenerationReportBase(){
    interface Story {

        fun generatePlot(theme: String): Plot

        data class Plot(
            val protagonist: Character = Character(),
            val setting: Setting = Setting(),
            val conflict: Conflict = Conflict(),
            val theme: String = "",
        )

        data class Character(
            val name: String = "",
            val age: Int = 0,
            val gender: String = "",
            val description: String = "",
        )

        data class Setting(
            val location: String = "",
            val time: String = "",
            val description: String = "",
        )

        data class Conflict(
            val type: String = "",
            val description: String = "",
        )

        fun generateCharacters(plot: Plot): CharacterList

        data class CharacterList(
            val characters: List<Character> = listOf(),
        )

        fun generateTwists(plot: Plot): TwistList

        data class TwistList(
            val twists: List<Twist> = listOf(),
        )

        data class Twist(
            val type: String = "",
            val description: String = "",
        )

        fun generateIllustrations(plot: Plot): IllustrationList

        data class IllustrationList(
            val illustrations: List<Illustration> = listOf(),
        )

        data class Illustration(
            val image: ImageDescription = ImageDescription(),
            val text: String = "",
        )

        data class ImageDescription(
            val style: String = "",
            val subject: String = "",
            val background: String = "",
            val detailedCaption: String = "",
        )

    }

    @Test
    fun childrenStory() {
        runReport("Children Story", Story::class) { api, logJson, out ->
            val theme = "friendship"
            val plot = api.generatePlot(theme)
            logJson(plot)
            out(
                """
                |
                |# ${plot.protagonist.name} and the ${plot.theme.capitalize()}
                |
                |${plot.protagonist.name} is a ${plot.protagonist.age} year old ${plot.protagonist.gender} living in ${plot.setting.location}.
                |
                |${plot.protagonist.description}
                |
                |${plot.setting.description}
                |
                |${plot.protagonist.name} is faced with a ${plot.conflict.type} - ${plot.conflict.description}
                |
                |""".trimMargin()
            )
            val characters = api.generateCharacters(plot).characters
            logJson(characters)
            out(
                """
                |
                |## Characters
                |
                |${characters.joinToString("\n\n") {
                    """
                    |
                    |### ${it.name}
                    |
                    |${it.description}
                    |
                    |""".trimMargin()
                }}
                |
                |""".trimMargin()
            )
            val twists = api.generateTwists(plot).twists
            logJson(twists)
            out(
                """
                |
                |## Plot Twists
                |
                |${twists.joinToString("\n\n") {
                    """
                    |
                    |### ${it.type}
                    |
                    |${it.description}
                    |
                    |""".trimMargin()
                }}
                |
                |""".trimMargin()
            )
            val illustrations = api.generateIllustrations(plot).illustrations
            logJson(illustrations)
            out(
                """
                |
                |## Story
                |
                |${illustrations.joinToString("\n\n") {
                    """
                    |
                    |### ${it.text}
                    |
                    |![${it.image.detailedCaption}](${
                        writeImage(
                            proxy.api.text_to_image(
                                it.image.detailedCaption,
                                resolution = 512
                            )[0]
                        )
                    })
                    |
                    |""".trimMargin()
                }}
                |
                |""".trimMargin()
            )
        }
    }
}