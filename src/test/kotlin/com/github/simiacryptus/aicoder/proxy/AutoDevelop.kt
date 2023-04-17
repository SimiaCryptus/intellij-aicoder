package com.github.simiacryptus.aicoder.proxy

import com.github.simiacryptus.aicoder.SoftwareProjectAI
import com.github.simiacryptus.aicoder.SoftwareProjectAI.Companion.parallelImplement
import com.github.simiacryptus.aicoder.SoftwareProjectAI.Companion.write
import com.simiacryptus.util.JsonUtil.fromJson
import org.junit.Test
import java.util.*

/**
 * AutoDevelop takes a software project description and generates a software project with all the necessary files.
 */
class AutoDevelop : GenerationReportBase<SoftwareProjectAI>(SoftwareProjectAI::class) {

    @Test
    fun softwareProject() {
        runReport("SoftwareProject") { api, logJson, out ->
            report(api, logJson, out)
        }
    }

    @Test
    fun testMethodImplementation() {
        runReport("SoftwareProject_Impl") { api, logJson, out ->
            val sourceCode = api.implementComponentSpecification(
                fromJson(
                    "{\r\n  \"name\" : \"SupportAliasBot\",\r\n  \"description\" : \"Slack bot to monitor a support alias\",\r\n  \"language\" : \"Kotlin\",\r\n  \"features\" : [ \"Record all requests tagging an alias in a database\", \"Send a message to a Slack channel when requests are tagged with a specific label\" ],\r\n  \"libraries\" : [ \"Gradle\", \"Spring\" ],\r\n  \"buildTools\" : [ \"Gradle\" ]\r\n}",
                    SoftwareProjectAI.Project::class.java
                ),
                fromJson(
                    "{\r\n  \"description\" : \"Main class for the SupportAliasBot\",\r\n  \"requires\" : [ ],\r\n  \"publicProperties\" : [ ],\r\n  \"publicMethodSignatures\" : [ \"fun main(args: Array<String>): Unit\" ],\r\n  \"language\" : \"Kotlin\",\r\n  \"location\" : {\r\n    \"file\" : \"src/main/kotlin/com/example/supportaliasbot/SupportAliasBot.kt\"\r\n  }\r\n}",
                    SoftwareProjectAI.ComponentDetails::class.java
                ),
                listOf(),
                fromJson(
                    "{\r\n  \"name\" : \"SupportAliasBot\",\r\n  \"description\" : \"Slack bot to monitor a support alias\",\r\n  \"language\" : \"Kotlin\",\r\n  \"features\" : [ \"Record all requests tagging an alias in a database\", \"Send a message to a Slack channel when requests are tagged with a specific label\" ],\r\n  \"libraries\" : [ \"Gradle\", \"Spring\" ],\r\n  \"buildTools\" : [ \"Gradle\" ]\r\n}",
                    SoftwareProjectAI.CodeSpecification::class.java
                ),
            )
            out("""
                |```${sourceCode.language}
                |${sourceCode.code}
                |```
                |""".trimMargin())
        }
    }

    private fun report(
        api: SoftwareProjectAI,
        logJson: (Any?) -> Unit,
        out: (Any?) -> Unit
    ) {
        val drafts = 1
        val threads = 7
        proxy.temperature = 0.5
        proxy.model = "gpt-4-0314"

        @Suppress("JoinDeclarationAndAssignment")
        val description: String
        """
                |
                |Slack bot to monitor a support alias
                |All requests tagging an alias are recorded in a database
                |When requests are tagged with a specific label, the bot will send a message to a slack channel
                |
                |Language: Kotlin
                |Frameworks: Gradle, Spring
                |
                """.trimMargin()
        """
                |
                |Create a website where users can upload stories, share them, and rate them
                |
                |Language: Kotlin
                |Frameworks: Gradle, Spring
                |
                """.trimMargin()
        description = """
        | Implement a game of Pong using Javascript
        | The game will be served from a web server based on Jetty
        | The game will be rendered using HTML5 Canvas
        | The game will be controlled using the keyboard
        | The game uses WebSockets to communicate with the server and peers
        | User can login via Google or Facebook
        | Invites can be sent to other users via url
        """.trimMargin()
        out(
            """
                |
                |# Software Project Development Report
                |
                |## Description
                |
                |```
                |${description.trim()}}
                |```
                |
                |""".trimMargin()
        )


        var project: SoftwareProjectAI.Project? = null
        var requirements: SoftwareProjectAI.ProjectStatements?
        var projectDesign: SoftwareProjectAI.ProjectDesign?
        var components: Map<SoftwareProjectAI.ComponentDetails, List<SoftwareProjectAI.CodeSpecification>>?
        var documents: Map<SoftwareProjectAI.DocumentationDetails, List<SoftwareProjectAI.DocumentSpecification>>?
        var tests: Map<SoftwareProjectAI.TestDetails, List<SoftwareProjectAI.TestSpecification>>?
        var implementations: Map<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode?>? = null

        try {
            project = api.newProject(description.trim())
            out(
                """
                |
                |Project Name: ${project.name}
                |
                |Description: ${project.description}
                |
                |Language: ${project.language}
                |
                |Libraries: ${project.libraries?.joinToString(", ")}
                |
                |Build Tools: ${project.buildTools?.joinToString(", ")}
                |
                |""".trimMargin()
            )
            logJson(project)
            requirements = api.getProjectStatements(description.trim(), project)
            out(
                """
                |
                |## Requirements
                |
                |""".trimMargin()
            )
            logJson(requirements)

            projectDesign = api.buildProjectDesign(project, requirements)
            out(
                """
                |
                |## Design
                |
                |""".trimMargin()
            )
            logJson(projectDesign)

            components =
                projectDesign.components?.associate {
                    it to (api.buildComponentFileSpecifications(
                        project,
                        requirements,
                        it
                    ))
                }
            documents =
                projectDesign.documents?.associate {
                    it to (api.buildDocumentationFileSpecifications(
                        project,
                        requirements,
                        it
                    )
                            )
                }
            tests = projectDesign.tests?.associate {
                it to (api.buildTestFileSpecifications(project, requirements, it))
            }

            out(
                """
                |
                |## Components
                |
                |""".trimMargin()
            )
            logJson(components)

            out(
                """
                |
                |## Documents
                |
                |""".trimMargin()
            )
            logJson(documents)
            out(
                """
                |
                |## Tests
                |
                |""".trimMargin()
            )
            logJson(tests)
            implementations = parallelImplement(api, project, components, documents, tests, drafts, threads)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (implementations != null) {
            val name = (project?.name ?: UUID.randomUUID().toString()).replace(Regex("[^a-zA-Z0-9]"), "_")
            val relative = "projects/$name.zip"
            val zipArchiveFile = outputDir.resolve(relative)
            zipArchiveFile.parentFile.mkdirs()
            write(zipArchiveFile, implementations)
            out(
                """
                |
                |## Project Files
                |
                |[Download]($relative)
                |
                |""".trimMargin()
            )
            implementations.toList().sortedBy { it.first.file }.forEach { (file, sourceCodes) ->
                out(
                    """
                    |
                    |### ${file.file}
                    |
                    |```${sourceCodes!!.language?.lowercase()}
                    |${sourceCodes.code}
                    |```
                    |
                    |""".trimMargin()
                )
            }
        }

    }
}