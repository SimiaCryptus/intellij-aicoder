package com.github.simiacryptus.aicoder.actions.generic

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.util.describe.Description
import org.apache.commons.io.FileUtils

import javax.swing.*

class GenerateStoryAction extends FileContextAction<GenerateStoryAction.Settings> {

    GenerateStoryAction() {
        super(false, true)
    }

    interface AuthorAPI {
        class Idea {
            public String title = ""
            public String description = ""

            Idea() {}

            Idea(String title, String description) {
                this.title = title
                this.description = description
            }
        }

        StoryTemplate generatePlot(Idea idea, int numberOfCharacters, int numberOfSettings)

        class Character {
            public String name = ""
            public int age = 0
            public String bio = ""
            public String role = ""
            public String development = ""

            Character() {}
        }

        class Setting {
            public String location = ""
            public String timePeriod = ""
            public String description = ""
            public String significance = ""

            Setting() {}
        }

        class StoryTemplate {
            public List<Character> characters = []
            public List<Setting> settings = []
            @Description("Date and time of the start of the story (e.g. \"2021-01-01 12:00:00\")")
            public String startDateTime = ""
            @Description("Date and time of the end of the story (e.g. \"2021-01-01 12:00:00\")")
            public String endDateTime = ""
            @Description("Genre of the story (e.g. \"fantasy\", \"sci-fi\", \"romance\")")
            public String genre = ""
            @Description("Tone of the story (e.g. \"serious\", \"funny\", \"sad\")")
            public String tone = ""
            @Description("Mood of the story (e.g. \"happy\", \"sad\", \"angry\")")
            public String mood = ""
            @Description("Theme of the story (e.g. \"love\", \"death\", \"revenge\")")
            public String theme = ""

            StoryTemplate() {}
        }

        class StoryEvents {
            public List<StoryEvent> storyEvents = []

            StoryEvents() {}
        }

        class StoryEvent {
            public String who = ""
            public String what = ""
            public String where = ""
            @Description("Date and time of the event (e.g. \"2021-01-01 12:00:00\")")
            public String when = ""
            public String why = ""
            public String how = ""
            public String result = ""
            public String punchline = ""
            public List<StoryObjectOrigin> origins = []

            StoryEvent() {}
        }

        class StoryObjectOrigin {
            @Description("Actor or object name")
            public String name = ""
            @Description("Where did this object come from? e.g. \"Jackie was introduced in the previous chapter.\"  or \"the book was found on a bookshelf\"")
            public String origin = ""

            StoryObjectOrigin() {}
        }

        StoryEvents generatePlotPoints(Idea idea, StoryTemplate story)

        StoryEvents expandEvents(
                @Description("Overall story for context")
                        StoryTemplate context,
                @Description("Previous events (for continuity)")
                        StoryEvents previous,
                @Description("Next events; Expanded events will lead to these events")
                        StoryEvents next,
                @Description("Current event to expand")
                        StoryEvent event,
                @Description("The number of events to generate")
                        int count
        )

        ScreenplaySegment writeScreenplaySegment(
                StoryTemplate story,
                StoryEvent event,
                ScreenplaySegment prevoiousSegment,
                int segmentItemCount
        )

        class ScreenplaySegment {
            public String settingStart = ""
            public List<ScreenplayItem> items = []
            public String settingEnd = ""

            ScreenplaySegment() {}
        }

        class ScreenplayItem {
            public String actor = ""
            public String type = ""
            public String text = ""

            ScreenplayItem() {}
        }

        Page writeStoryPage(
                String style,
                ScreenplaySegment scene,
                Page previousPage,
                int pageWordCount
        )

        class Page {
            public int pageNumber
            @Description("Full page text")
            public String text = ""

            Page() {}
        }
    }


    @SuppressWarnings("UNUSED")
    static class SettingsUI {
        @Name("Title")
        public JTextField title = new JTextField("How to write a book")

        @Name("Description")
        public JTextArea description = new JTextArea(
                """
            |A software developer teaches a computer how to teach another computer how to write a book. 
            |They then teach another computer to use that computer to publish and sell books online.
            |Chaos ensues. Society collapses. The world ends.
            |""".stripMargin().trim()
        )

        @Name("Title")
        public JTextField writingStyle = new JTextField("First Person Narrative, Present Tense, 8th Grade Reading Level, Funny")
    }

    static class Settings {
        public String title = ""
        public String description = ""
        public String writingStyle = ""

        Settings() {}
    }

    @Override
    Settings getConfig(Project project) {
        return UITools.showDialog(project, SettingsUI.class, Settings.class, "Generate Story", {})
    }

    AuthorAPI proxy = null

    @Override
    File[] processSelection(SelectionState state, Settings config) {
        proxy = new ChatProxy<AuthorAPI>(
                clazz: AuthorAPI.class,
                api: api,
                model: AppSettingsState.instance.defaultChatModel(),
                temperature: AppSettingsState.instance.temperature,
                deserializerRetries: 2,
        ).create()
        
        List<File> outputFiles = []

        if (config) {
            File selectedFolder = state.selectedFile
            def idea = new AuthorAPI.Idea(config.title, config.description)
            def storyTemplate = proxy.generatePlot(idea, 5, 5)
            def storyEvents = proxy.generatePlotPoints(idea, storyTemplate)
            List<AuthorAPI.ScreenplaySegment> segments = []
            AuthorAPI.ScreenplaySegment previousSegment = null
            storyEvents.storyEvents.each { event ->
                segments << proxy.writeScreenplaySegment(storyTemplate, event, previousSegment, 5)
                previousSegment = segments.last()
            }
            File screenplayFile = new File(new File(selectedFolder.path), config.title + "_screenplay.md")
            screenplayFile.parentFile.mkdirs()
            def fileContents = segments.collect {
                it.items.collect {
                    """
                    |
                    |**${it.actor}**: ${it.text}
                    |
                    """.stripMargin()
                }.join("\n")
            }.join("\n\n")
            FileUtils.write(screenplayFile, fileContents, "UTF-8")
            outputFiles << screenplayFile
            List<AuthorAPI.Page> pages = []
            AuthorAPI.Page previousPage = null
            segments.each { segment ->
                try {
                    pages << proxy.writeStoryPage(config.writingStyle, segment, previousPage, 2)
                    previousPage = pages.last()
                } catch (Exception e) {
                    UITools.error(log,"Failed to write page: ${e.message}", e)
                }
            }
            File storyFile = new File(new File(selectedFolder.path), config.title + ".md")
            FileUtils.write(storyFile, pages.collect { it.text }.join("\n\n"), "UTF-8")
            outputFiles << storyFile
        }

        return outputFiles.toArray()
    }

}