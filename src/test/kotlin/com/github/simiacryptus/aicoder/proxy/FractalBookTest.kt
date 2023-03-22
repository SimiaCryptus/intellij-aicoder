package com.github.simiacryptus.aicoder.proxy

import com.github.simiacryptus.openai.proxy.Description
import org.junit.Test


class FractalBookTest : GenerationReportBase() {
    interface FractalBook {
        data class Idea(var title: String? = "", var description: String? = "")

        fun generateIdeas(prompt: String): Ideas

        data class Ideas(var ideas: List<Idea>? = listOf())

        fun generatePlot(idea: Idea): StoryTemplate
        data class Character(var name: String? = "", var age: Int? = 0, var traits: List<String>? = listOf())
        data class Setting(var location: String? = "", var timePeriod: String? = "")
        data class StoryTemplate(
            var protagonist: Character? = Character(),
            var antagonist: Character? = Character(),
            var setting: Setting? = Setting(),
            var conflict: String? = "",
            var supportingCharacters: List<Character>? = listOf(),
            var resolution: String? = "",
        )

        data class PlotPoints(var plotPoints: List<PlotPoint>? = listOf())

        data class PlotPoint(
            @Description("Single sentence description of the plot point")
            var title: String? = "",
            @Description("Text describing the plot point; about 3 paragraphs")
            var description: String? = ""
        )

        fun generatePlotPoints(story: StoryTemplate): PlotPoints

        fun expandEvents(
            //story: StoryTemplate,
            @Description("Previous events to use for interpolation")
            previous: PlotPoints?,
            @Description("Next events to use for interpolation")
            next: PlotPoints?,
            @Description("Current event titla to expand")
            title: String?,
            @Description("Current event text")
            description: String?,
            @Description("The number of events to generate between the previous and next event")
            count: Int = 5,
        ): PlotPoints

    }

    @Test
    fun writeBook() {
        runReport("WriteBook", FractalBook::class) {
            // api to use
                api: FractalBook,
                // logJson is a function to write JSON for debugging
                logJson: (Any?) -> Unit,
                // out is a function to write markdown to the report
                out: (Any?) -> Unit ->

            val ideas = api.generateIdeas(
                """
                I want to write a children's book for a 10 year old boy. He likes Roblox and Subnautica.
                """.trimIndent()
            )
            logJson(ideas)
            val selectedIdea = ideas.ideas!!.random()
            out(
                """
                |Selected Idea: ${selectedIdea.title}
                |""".trimMargin()
            )


            val story = api.generatePlot(selectedIdea)

            out(
                """
                |Story: ${story.protagonist!!.name} is a ${story.protagonist!!.age} year old ${story.protagonist!!.traits!!.random()} who lives in ${story.setting!!.location} during the ${story.setting!!.timePeriod}. ${story.protagonist!!.name} is ${story.conflict} and ${story.resolution}.
                |""".trimMargin()
            )

            val plotPoints = api.generatePlotPoints(story)


            // Expand plot points by keeping track of the previous and next points
            var points = plotPoints.plotPoints!!
            for (i in 0..2) {
                out(
                    """
                    |# Level $i:
                    |""".trimMargin()
                )
                logJson(points)
                points = getExpandedPlotPoints(api, story, points)
            }

        }
    }

    private fun getExpandedPlotPoints(api: FractalBook, story: FractalBook.StoryTemplate, points: List<FractalBook.PlotPoint>): List<FractalBook.PlotPoint>
    {
        val previousBuffer = mutableListOf<FractalBook.PlotPoint>()
        for (point in points.withIndex()) {
            val expandedPoints = api.expandEvents(
                previous = FractalBook.PlotPoints(previousBuffer.takeLast(4).toList()),
                next = FractalBook.PlotPoints(points.drop(point.index + 1).take(1)),
                title = point.value.title,
                description = point.value.description
            )
            previousBuffer.addAll(expandedPoints.plotPoints!!)
        }
        return previousBuffer.toList()
    }

    private fun getExpandedPlotPoints1(api: FractalBook, story: FractalBook.StoryTemplate, points: List<FractalBook.PlotPoint>): List<FractalBook.PlotPoint>
    {
        return points.mapIndexed { index, plotPoint ->
            // Call the expandEvents function on the api
            api.expandEvents(
                // Pass in the plot points from 0 to the current index - 1
                FractalBook.PlotPoints(points.take(Math.max(0,index - 1)).takeLast(2)),
                // Pass in the plot points from the current index + 1 to the end
                FractalBook.PlotPoints(points.drop(index+1).take(2)),
                // Pass in the current plot point
                plotPoint.title,
                plotPoint.description
            ).plotPoints
            // Flat map the result
        }.flatMap { it!! }
    }
}
