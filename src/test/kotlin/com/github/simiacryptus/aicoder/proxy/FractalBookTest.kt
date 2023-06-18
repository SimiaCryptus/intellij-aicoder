@file:Suppress("unused")

package com.github.simiacryptus.aicoder.proxy

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.describe.Description
import org.junit.Test


class FractalBookTest : GenerationReportBase<FractalBookTest.FractalBook>(FractalBook::class) {
    interface FractalBook {
        data class Idea(var title: String? = "", var description: String? = "")

        fun generateIdeas(prompt: String): Ideas

        data class Ideas(var ideas: List<Idea>? = listOf())

        fun generatePlot(idea: Idea, numberOfCharacters: Int = 5, numberOfSettings: Int = 5): StoryTemplate
        data class Character(
            var name: String? = "",
            var age: Int? = 0,

            var bio: String? = "",
            var role: String? = "",
            var development: String? = "",

//            var nationality: String? = "",
//            var motivation: String? = "",
//            var backstory: String? = "",
//            var personalityTraits: List<String>? = listOf(),
//            var dialogTraits: List<String>? = listOf(),
        )

        data class Setting(
            var location: String? = "",
            var timePeriod: String? = "",
            var description: String? = "",
            var significance: String? = "",
        )
        data class StoryTemplate(
            var characters: List<Character>? = listOf(),
            var settings: List<Setting>? = listOf(),

            @Description("Date and time of the start of the story (e.g. \"2021-01-01 12:00:00\")")
            var startDateTime: String? = "",
            @Description("Date and time of the end of the story (e.g. \"2021-01-01 12:00:00\")")
            var endDateTime: String? = "",

//            var conflict: String? = "",
//            var resolution: String? = "",

            @Description("Genre of the story (e.g. \"fantasy\", \"sci-fi\", \"romance\")")
            var genre: String? = "",
            @Description("Tone of the story (e.g. \"serious\", \"funny\", \"sad\")")
            var tone: String? = "",
            @Description("Mood of the story (e.g. \"happy\", \"sad\", \"angry\")")
            var mood: String? = "",
            @Description("Theme of the story (e.g. \"love\", \"death\", \"revenge\")")
            var theme: String? = "",

            )

        data class StoryEvents(var storyEvents: List<StoryEvent>? = listOf())

        data class StoryEvent(
            var who: String? = "",
            var what: String? = "",
            var where: String? = "",
            @Description("Date and time of the event (e.g. \"2021-01-01 12:00:00\")")
            var `when`: String? = "",
            var why: String? = "",
            var how: String? = "",
            var result: String? = "",

            var punchline: String? = "",

//            @Description("""
//                previous plot mention for each actor/object, or origin explanation; e.g. "Jackie" -> "Jackie was introduced in the previous chapter."
//                """)
//            var origins: Map<String, String>? = mapOf(),
            var origins: List<StoryObjectOrigin>? = listOf(),

//            var plotSignificance: String? = "",
//            var characterSignificance: String? = "",

//            @Description("Single sentence")
//            var title: String? = "",

//            @Description("About 3 paragraphs, written in the first person")
//            var text: String? = "",

//            @Description("The character who is experiencing the event")
//            var perspective: String? = ""

        )

        data class StoryObjectOrigin(
            @Description("Actor or object name")
            var name: String? = "",
            @Description("Where did this object come from? e.g. \"Jackie was introduced in the previous chapter.\"  or \"the book was found on a bookshelf\"")
            var origin: String? = "",
        )

        fun generatePlotPoints(idea: Idea, story: StoryTemplate): StoryEvents

        fun expandEvents(
            @Description("Overall story for context")
            context: StoryTemplate,
            @Description("Previous events (for continuity)")
            previous: StoryEvents?,
            @Description("Next events; Expanded events will lead to these events")
            next: StoryEvents?,
            @Description("Current event to expand")
            event: StoryEvent?,
            @Description("The number of events to generate")
            count: Int = 5,
        ): StoryEvents

        fun writeScreenplaySegment(
            story: StoryTemplate,
            event: StoryEvent,
            prevoiousSegment: ScreenplaySegment?,
            segmentItemCount: Int = 30,
        ): ScreenplaySegment

        data class ScreenplaySegment(
            var settingStart: String? = "",
            var items: List<ScreenplayItem>? = listOf(),
            var settingEnd: String? = "",
        )

        data class ScreenplayItem(
            var actor: String? = "",
            //@Description("Either \"dialog\" or \"action\"")
            var type: String? = "",
            var text: String? = "",
        )

        fun writeStoryPage(
            style: String,
            scene: ScreenplaySegment,
            previousPage: Page?,
            pageWordCount: Int = 500,
        ): Page

        fun writeStoryPage(
            story: StoryTemplate,
            event: StoryEvent,
            previousPage: Page?,
        ): Page

        data class Page(
            var pageNumber: Int? = 0,
            @Description("Full page text")
            var text: String? = "",
//            var image: String? = "",
        )

    }

    @Test
    fun testFullPageWriter() {


//        proxy.addExample(returnValue = FractalBook.Page(1, """
//            |
//            |The team arrives in the Wild West and immediately notices something is wrong. The town is in chaos and people are running in all directions.
//            |
//            | """.trimMargin().trim())) {
//            it.writeStoryPage(
//                style = "easy to read, funny, and light-hearted",
//                scene = FractalBook.ScreenplaySegment(
//                    setting = "Wild West",
//                    listOf(
//                        FractalBook.ScreenplayItem("Dr. Amelia, Jackie, Professor Jameson, Alex, Tommy", "action", "The team arrives in the Wild West and immediately notices something is wrong. The town is in chaos and people are running in all directions."),
//                        FractalBook.ScreenplayItem("Jackie", "dialog", "What's going on here? Why is everyone so scared?"),
//                    )
//                ),
//                previousPage = null,
//                pageWordCount = 50
//            )
//        }


        runReport("WriteBookFull") {
            // api to use
                api: FractalBook,
                // logJson is a function to write JSON for debugging
                logJson: (Any?) -> Unit,
                // out is a function to write markdown to the report
                out: (Any?) -> Unit ->

            proxy.model = AppSettingsState.instance.defaultChatModel()
            proxy.temperature = 0.1
            val expansionIterations = 0
            val idea = FractalBook.Idea(
                title = "Time Travel for Dummies",
                description = """
                    Explore the mishaps and misadventures of a group of time-traveling friends as they navigate through history, 
                    trying to fix their mistakes and avoid creating even more chaos. 
                    Expect plenty of laughs, unexpected twists, and a healthy dose of historical humor.
                    """.trimIndent()
            )
            val writingStyle =
                """
                    |easy to read, funny, and light-hearted
                    |third-person present perspective with scene imagery, dialogue, and action
                    |""".trimMargin().trim()
            // third-person omniscient perspective, allowing the reader to see into the thoughts and emotions of multiple characters


            val story = api.generatePlot(idea)
            logJson(story)

            val plotPoints = api.generatePlotPoints(idea, story)

            var points = plotPoints.storyEvents!!

            out(
                """
                    |
                    |# Initial Plot Points:
                    |
                    |""".trimMargin()
            )
            logJson(points)

            val parentMap = HashMap<FractalBook.StoryEvent, FractalBook.StoryEvent>()
            @Suppress("EmptyRange")
            for (i in 1..expansionIterations) {
                points = run {
                    val previousBuffer = mutableListOf<FractalBook.StoryEvent>()
                    for (point in points.withIndex()) {
                        val expandedPoints = api.expandEvents(
                            context = story,
                            previous = FractalBook.StoryEvents(previousBuffer.takeLast(4).toList()),
                            next = FractalBook.StoryEvents(points.drop(point.index + 1).take(1)),
                            event = point.value
                        )
                        expandedPoints.storyEvents!!.forEach { parentMap[it] = point.value }
                        previousBuffer.addAll(expandedPoints.storyEvents!!)
                    }
                    previousBuffer.toList()
                }
                out(
                    """
                    |
                    |# Iteration ${i}:
                    |
                    |""".trimMargin()
                )
                logJson(points)

            }

            out(
                """
                    |
                    |# Screenplay:
                    |
                    |""".trimMargin()
            )

            // Write screenplay
            val screenplay = mutableListOf<FractalBook.ScreenplaySegment>()
            var previousSegment: FractalBook.ScreenplaySegment? = null
            for (point in points) {
                val segment: FractalBook.ScreenplaySegment =
                    api.writeScreenplaySegment(story, point, previousSegment)
                previousSegment = segment
                logJson(segment)
                screenplay.add(segment)
            }
            for (segment in screenplay) {
                for (item in segment.items!!) {
                    out(
                        """
                            |
                            |**${item.actor}**: ${item.text}
                            |
                            |""".trimMargin()
                    )
                }
            }


            out(
                """
                    |
                    |# Story:
                    |
                    |""".trimMargin()
            )

            // Write the story
            val pages = mutableListOf<FractalBook.Page>()
            var previous: FractalBook.Page? = null

            for (segment in screenplay) {
                try {
                    val page: FractalBook.Page = api.writeStoryPage(writingStyle, segment, previous)
                    previous = page
                    logJson(page)
                    pages.add(page)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            for (page in pages) {
                out(
                    """
                        |
                        |${page.text}
                        |
                        |""".trimMargin()
                )
            }

        }


    }
}
