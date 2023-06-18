package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.BaseAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.util.describe.Description
import java.io.File
import javax.swing.JTextArea
import javax.swing.JTextField

class GenerateStoryAction : BaseAction() {

    interface VirtualAPI {
        data class Idea(var title: String? = "", var description: String? = "")

        fun generatePlot(idea: Idea, numberOfCharacters: Int = 5, numberOfSettings: Int = 5): StoryTemplate
        data class Character(
            var name: String? = "",
            var age: Int? = 0,

            var bio: String? = "",
            var role: String? = "",
            var development: String? = "",

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

            var origins: List<StoryObjectOrigin>? = listOf(),

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
        )

    }

    @Suppress("UNUSED")
    class SettingsUI {
        @Name("Title")
        val title = JTextField("How to write a book")

        @Name("Description")
        val description = JTextArea("""
            |A software developer teaches a computer how to teach another computer how to write a book. 
            |They then teach another computer to use that computer to publish and sell books online.
            |Chaos ensues. Society collapses. The world ends.
            |""".trimMargin().trim())

        @Name("Title")
        val writingStyle = JTextField("First Person Narrative, Present Tense, 8th Grade Reading Level, Funny")
    }

    data class Settings(
        var title: String = "",
        var description: String = "",
        val writingStyle: String = "",
    )

    override fun actionPerformed(e: AnActionEvent) {
        UITools.showDialog(e, SettingsUI::class.java, Settings::class.java) { config ->
            handleImplement(e, config)
        }
    }

    private fun handleImplement(
        e: AnActionEvent,
        config: Settings
    ) = Thread {

        val selectedFolder = UITools.getSelectedFolder(e)!!

        val proxy = ChatProxy(
            clazz = VirtualAPI::class.java,
            api = OpenAIClient(
                key = AppSettingsState.instance.apiKey,
                apiBase = AppSettingsState.instance.apiBase,
                logLevel = AppSettingsState.instance.apiLogLevel
            ),
            deserializerRetries = 5,
        ).create()

        val storyTemplate = UITools.run(
            e.project, "Generating Story Entities", true
        ) {
            try {
                proxy.generatePlot(
                    VirtualAPI.Idea(
                        config.title,
                        config.description
                    )
                )
            } finally {
                if (it.isCanceled) throw InterruptedException()
            }
        }

        // Generate plot points for the story
        val storyEvents = UITools.run(
            e.project, "Generating Plot Points", true
        ) {
            try {
                proxy.generatePlotPoints(
                    VirtualAPI.Idea(
                        config.title,
                        config.description
                    ), storyTemplate
                )
            } finally {
                if (it.isCanceled) throw InterruptedException()
            }
        }


        // writeScreenplaySegment
        val segments = mutableListOf<VirtualAPI.ScreenplaySegment>()
        UITools.run(
            e.project, "Writing Screenplay", true
        ) {
            var previousSegment: VirtualAPI.ScreenplaySegment? = null
            for (event in storyEvents.storyEvents!!) {
                try {
                    segments.add(proxy.writeScreenplaySegment(storyTemplate, event, previousSegment))
                } finally {
                    if (it.isCanceled) throw InterruptedException()
                }
                previousSegment = segments.last()
            }
            File(File(selectedFolder.canonicalPath!!), config.title + "_screenplay.md")
                .writeText(segments.joinToString("\n\n") { it.items!!.joinToString("\n") { """
                    |
                    |**${it.actor}**: ${it.text}
                    |
                """.trimMargin() } })
        }

        // Write pages
        UITools.run(
            e.project, "Writing Story", true
        ) {
            val pages = mutableListOf<VirtualAPI.Page>()
            var previousPage: VirtualAPI.Page? = null
            for (segment in segments) {
                try {
                    pages.add(proxy.writeStoryPage(config.writingStyle, segment, previousPage))
                } finally {
                    if (it.isCanceled) throw InterruptedException()
                }
                previousPage = pages.last()
            }
            File(File(selectedFolder.canonicalPath!!), config.title + ".md")
                .writeText(pages.joinToString("\n\n") { it.text!! })
            selectedFolder.refresh(false, true)
        }


//        // Write pages
//        UITools.run(
//            e.project, "Writing Story", true
//        ) {
//            val pages = mutableListOf<VirtualAPI.Page>()
//            var previousPage: VirtualAPI.Page? = null
//            for (event in storyEvents.storyEvents!!) {
//                try {
//                    pages.add(api.writeStoryPage(storyTemplate, event, previousPage))
//                } finally {
//                    if (it.isCanceled) throw InterruptedException()
//                }
//                previousPage = pages.last()
//            }
//            File(File(selectedFolder.canonicalPath!!), config.title + ".md")
//                .writeText(pages.joinToString("\n\n") { it.text!! })
//            selectedFolder.refresh(false, true)
//        }

    }.start()


    override fun isEnabled(event: AnActionEvent): Boolean {
        if(UITools.isSanctioned()) return false
        return AppSettingsState.instance.devActions
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(GenerateStoryAction::class.java)
    }
}
