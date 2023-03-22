package com.github.simiacryptus.aicoder.proxy

import com.github.simiacryptus.openai.proxy.Description
import org.junit.Test
import kotlin.reflect.javaType
import kotlin.reflect.typeOf


class KidsBookTest : GenerationReportBase() {
    /**
     * I want to write a children's book for a 10 year old. However, I don't know how to write an entertaining book! Please provide some instructions to gather ideas, develop the story, and then write the book. Describe this like a programmer would - describe objects in terms of Kotlin data classes, and operations as method or api calls.
     */

    interface KidsBook {
        data class Idea(var title: String? = "", var description: String? = "")

        fun generateIdeas(prompt: String): Ideas

        data class Ideas(var ideas: List<Idea>? = listOf())
        data class Character(var name: String? = "", var age: Int? = 0, var traits: List<String>? = listOf())
        data class Setting(var location: String? = "", var timePeriod: String? = "")
        data class Plot(
            var protagonist: Character? = Character(),
            var antagonist: Character? = Character(),
            var setting: Setting? = Setting(),
            var conflict: String? = "",
            var supportingCharacters: List<Character>? = listOf(),
            var events: List<String>? = listOf(),
            var resolution: String? = "",
        )

        fun generatePlot(idea: Idea): Plot

        data class ChapterSummary(var title: String? = "", var subPlot: List<String>? = listOf())

        fun planChapters(idea: Idea, plot: Plot, subPlotPointsPerChapter: Int = 5): ChapterSummaries

        data class ChapterSummaries(var chapters: List<ChapterSummary>? = listOf())

        data class Chapter(var title: String? = "", var pages: List<Page>? = listOf())

        data class Page(@Description("One page (~3 paragraphs) of narrative text in the format of a story.") var text: String? = "",
                        var image: Image? = Image())

        data class Image(var filename: String? = "",
                         @Description("Caption used for Dall-E 2 image generation") var caption: String? = "")

        fun writeChapter(
            idea: Idea, plot: Plot,
            previousChapter: ChapterSummary?, nextChapter: ChapterSummary?,
            thisChapter: ChapterSummary, pageCount: Int = 10
        ): Chapter

    }

    @Test
    fun writeKidsBook() {
        runReport("KidsBook", KidsBook::class) { api, logJson, out ->
            val ideas = api.generateIdeas("""
                I want to write a children's book for a 10 year old boy. He likes Roblox and Subnautica.
                """.trimIndent())
            logJson(ideas)
            val selectedIdea = ideas.ideas!!.random()
            out("""
                |Selected Idea: ${selectedIdea.title}
                |""".trimMargin())

            val plot = api.generatePlot(selectedIdea)
            val characters = listOf(plot.antagonist, plot.protagonist)
            val setting = plot?.setting
            logJson(characters)
            out("""Characters: ${characters?.joinToString { it?.name ?: "" }}""".trimMargin())

            logJson(setting)
            out("""Setting: ${setting?.location}, ${setting?.timePeriod}""".trimMargin())

            logJson(plot)
            out("""
                |Plot: 
                |
                |${plot.events?.map {"1. " + it}?.joinToString("\n") ?: ""}
                |
                |""".trimMargin())

            val chapterSummaries = api.planChapters(selectedIdea, plot)
            logJson(chapterSummaries)
            val summaries = chapterSummaries.chapters!!
            out("""
                |Chapter Summaries:
                |
                |${summaries?.joinToString("\n") { "1. " + (it?.title ?: "") }}
                |
                |""".trimMargin())

            val chapters = mutableListOf<KidsBook.Chapter>()
            for (i in chapterSummaries.chapters?.indices ?: listOf()) {
                val prevChapter = if (i > 0) summaries[i - 1] else null
                val nextChapter = if (i < summaries.lastIndex) summaries[i + 1] else null
                val thisChapter = summaries[i]

                val chapter =
                    api.writeChapter(selectedIdea, plot, prevChapter, nextChapter, thisChapter)
                chapters.add(chapter)
                logJson(chapter)
            }

            out("""
                |
                |# ${selectedIdea.title}
                |
                |""".trimMargin())
            for (chapter in chapters) {
                out(
                    """
                    |
                    |## Chapter: ${chapter.title}
                    |
                    """.trimMargin()
                )
                for (page in chapter?.pages ?: listOf()) {
                    val caption = page.image?.caption ?: ""
                    if(caption.isNotBlank()) {
                        val bufferedImage = proxy.api.render(
                            caption,
                            resolution = 512
                        )[0]
                        val writeImage = writeImage(bufferedImage)
                        out("""
                            |
                            |![${caption}](${writeImage})
                            |
                            |${page.text}
                            |
                            |""".trimMargin())
                    } else {
                        out("""
                            |
                            |${page.text}
                            |
                            |""".trimMargin())
                    }
                }
            }
        }
    }
}
