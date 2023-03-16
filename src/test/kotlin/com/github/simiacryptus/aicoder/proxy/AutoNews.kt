package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 *  FakeNews is a class that implements the News interface.
 *  It provides methods to get a publication, get stories,
 *  cover a story, and generate a report.
 */
class AutoNews : GenerationReportBase() {
    interface News {

        fun getPublication(publicationName: String): Publication

        data class Publication(
            val name: String = "",
            val description: String = "",
            val tags: List<String> = listOf(),
            val publishing_date: String? = null,
        )

        fun getStories(
            publication: Publication,
            category: String,
            storyCount: Int = 3,
            funny: Boolean = true
        ): StoryList

        data class StoryList(
            val stories: List<Story> = listOf(),
            val category: String = "",
        )

        data class Story(
            val who: String = "",
            val what: String = "",
            val `when`: String = "",
            val where: String = "",
            val why: String = "",
            val punchline: String = "",
            val commentary: String = "",
            val image: ImageDescription? = null,
            val isFunny: Boolean = false,
        )

        data class ImageDescription(
            val style: String = "",
            val subject: String = "",
            val background: String = "",
            val detailedCaption: String = "",
        )

        fun coverStory(publication: Publication, story: Story, paragraphCount: Int = 10): Article

        data class Article(
            val title: String = "",
            val author: String = "",
            val keywords: String = "",
            val content: List<String> = listOf(),
        )

    }

    @Test
    fun newsWebsite() {
        runReport("News", News::class) { api, logJson, out ->
            val categories = listOf(
                "politics",
                "science",
                "technology",
                "entertainment",
                "sports",
                "business",
                "health",
                "travel",
                "food",
                "weather",
                "fashion",
                "lifestyle",
                "finance",
                "education",
                "environment",
                "religion",
            )
            val publication = News.Publication(
                description = "A humorous exploration of the unknown",
                tags = listOf("satire", "funny", "science fiction", "future"),
                name = "Beyond Imagination",
                publishing_date = "2023-07-04",
            )
            logJson(publication)
            out(
                """
                |
                |# ${publication.name}
                |
                |${publication.description}
                |
                |Tags: ${publication.tags.joinToString(", ")}
                |
                |""".trimMargin()
            )
            for (category in categories) {
                out(
                    """
                    |
                    |## ${category.capitalize()}
                    |
                    |""".trimMargin()
                )
                val stories = api.getStories(publication, category)
                logJson(stories)
                for (story in stories.stories) {
                    val article = api.coverStory(publication, story)
                    logJson(article)
                    out(
                        """
                        |
                        |### ${article.title}
                        |
                        |![${story.image!!.detailedCaption}](${
                            writeImage(
                                proxy.api.text_to_image(
                                    story.image.detailedCaption,
                                    resolution = 512
                                )[0]
                            )
                        })
                        |
                        |${article.content.joinToString("\n\n")}
                        |
                        |Keywords: ${article.keywords}
                        |
                        |""".trimMargin()
                    )
                }
            }
        }
    }
}

