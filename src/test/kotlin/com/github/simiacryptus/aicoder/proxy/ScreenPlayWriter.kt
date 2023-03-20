package com.github.simiacryptus.aicoder.proxy

import org.junit.Test
import java.io.BufferedWriter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScreenPlayWriter {
    interface StoryTelling {
        fun newStory(
            moral: String = "",
            themes: List<String> = listOf("")
        ): StorySummary

        data class StorySummary(
            val title: String = "",
            val description: String = "",
            val characters: List<Character> = listOf(),
            val setting: String = "",
            val genre: String = "",
            val theme: String = "",
            val conflict: String = "",
            val resolution: String = "",
            val moral: String = "",
            val tone: String = "",
            val style: String = ""
        )

        data class Character(
            val name: String = "",
            val personality: String = "",
            val backstory: String = "",
            val motivation: String = ""
        )

        fun actSummaries(story: StorySummary): ActSummaryList
        data class ActSummaryList(val acts: List<ActSummary> = listOf())
        data class ActSummary(
            val title: String = "",
            val index: Int = 0,
            val description: String = ""
        ) {
            override fun toString(): String = """
            |# Act ${this.index} - ${this.title}
            |${this.description}
            |""".trimMargin()
        }

        fun sceneSummaries(story: StorySummary, act: ActSummary, previousActScene: SceneSummary?): SceneSummaryList
        data class SceneSummaryList(val scenes: List<SceneSummary> = listOf())
        data class SceneSummary(
            val title: String = "",
            val index: Int = 0,
            val description: String = "",
            val characters: List<String> = listOf(),
            val setting: String = "",
            val dialogLines: Int = 10,
        ) {
            override fun toString(): String = """
                |## Scene ${this.index} - ${this.title}
                |${this.description}
                |
                |Setting: ${this.setting}
                |""".trimMargin()
        }

        fun sceneToScreenplay(
            story: StorySummary,
            act: ActSummary,
            currentScene: SceneSummary,
            previousSceneSummary: Screenplay?
        ): Screenplay

        data class Screenplay(val segments: List<ScreenplaySegment> = listOf()) {
            override fun toString(): String = segments.joinToString("\n\n") { segment ->
                when (segment.character) {
                    "Narrator" -> segment.text
                    else -> "${segment.character}: ${segment.text}"
                }
            }
        }

        data class ScreenplaySegment(
            val character: String = "",
            val text: String = ""
        )
    }

    @Test
    fun writeNewStory() {
        if (!ProxyTest.keyFile.exists()) return
        val outputDir = File(".")
        val markdownOutputFile = File(
            outputDir,
            "Story_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))}.md"
        )
        BufferedWriter(markdownOutputFile.writer()).use { writer ->
            fun out(s: Any) {
                println(s.toString())
                writer.write(s.toString())
                writer.newLine()
            }

            val proxy = ProxyTest.chatProxy()
            val file = File("story.examples.json")
            if (file.exists()) proxy.addExamples(file)
            val storyApi = proxy.create(StoryTelling::class.java)
            val story =
                storyApi.newStory(
                    themes = listOf("Children's Book", "Dark", "Funny For Adults"),
                    moral = "Never try to steal from a wizard"
                )
            out("```json\n" + proxy.toJson(story) + "\n```\n")
            val acts = storyApi.actSummaries(story)
            var previousActScene: StoryTelling.SceneSummary? = null
            val sceneMap = acts.acts.associateWith { act ->
                out("```json\n" + proxy.toJson(act) + "\n```\n")
                val scenes = storyApi.sceneSummaries(story, act, previousActScene)
                previousActScene = scenes.scenes.lastOrNull()
                out("```json\n" + proxy.toJson(scenes) + "\n```\n")
                scenes
            }
            var previousScene: StoryTelling.Screenplay? = null
            for ((act, scenes) in sceneMap) {
                out(act)
                for (scene in scenes.scenes) {
                    out(scene)
                    val screenplay: StoryTelling.Screenplay =
                        storyApi.sceneToScreenplay(story, act, scene, previousScene)
                    previousScene = screenplay
                    out(screenplay)
                }
            }
        }
    }
}

