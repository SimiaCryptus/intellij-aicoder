package com.github.simiacryptus.aicoder.proxy

import com.github.simiacryptus.openai.proxy.SoftwareProjectAI
import com.github.simiacryptus.openai.proxy.SoftwareProjectAI.Companion.parallelImplement
import com.github.simiacryptus.openai.proxy.SoftwareProjectAI.Companion.write
import org.junit.Test
import java.util.*

/**
 * AutoDevelop takes a software project description and generates a software project with all the necessary files.
 */
class AutoDevelop : GenerationReportBase() {

    @Test
    fun softwareProject() {
        runReport("SoftwareProject", SoftwareProjectAI::class) { api, logJson, out ->
            report(api, logJson, out)
        }
    }

    private fun report(
        api: SoftwareProjectAI,
        logJson: (Any?) -> Unit,
        out: (Any?) -> Unit
    ) {
        var description: String
        description = """
                |
                |Slack bot to monitor a support alias
                |All requests tagging an alias are recorded in a database
                |When requests are tagged with a specific label, the bot will send a message to a slack channel
                |Fully implement all functions
                |Do not comment code
                |Include documentation and build scripts
                |
                |Language: Kotlin
                |Frameworks: Gradle, Spring
                |
                """.trimMargin()
        description = """
                |
                |Create a website where users can upload stories, share them, and rate them
                |
                |Fully implement all functions
                |Do not comment code
                |Include documentation and build scripts
                |
                |Language: Kotlin
                |Frameworks: Gradle, Spring
                |
                """.trimMargin()
        out("""
                |
                |# Software Project Development Report
                |
                |## Description
                |
                |```
                |${description.trim() }}
                |```
                |
                |""".trimMargin())


        var project: SoftwareProjectAI.Project? = null
        var requirements: SoftwareProjectAI.ProjectStatements? = null
        var projectDesign: SoftwareProjectAI.ProjectDesign? = null
        var components: Map<SoftwareProjectAI.ComponentDetails, List<SoftwareProjectAI.CodeSpecification>>? = null
        var documents: Map<SoftwareProjectAI.DocumentationDetails, List<SoftwareProjectAI.DocumentSpecification>>? = null
        var tests: Map<SoftwareProjectAI.TestDetails, List<SoftwareProjectAI.TestSpecification>>? = null
        var implementations: Map<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode?>? = null

        try {
            project = api.newProject(description.trim())
            out("""
                |
                |Project Name: ${project.name}
                |
                |Description: ${project.description}
                |
                |Language: ${project.language}
                |
                |Libraries: ${project.libraries.joinToString(", ")}
                |
                |Build Tools: ${project.buildTools.joinToString(", ")}
                |
                |""".trimMargin()
            )
            logJson(project)
            requirements = api.getProjectStatements(description.trim(), project)
            out("""
                |
                |## Requirements
                |
                |""".trimMargin())
            logJson(requirements)
            projectDesign = api.buildProjectDesign(project, requirements)
            out("""
                |
                |## Design
                |
                |""".trimMargin())
            logJson(projectDesign)
            components =
                projectDesign.components.map { it to api.buildComponentFileSpecifications(project, requirements, it) }
                    .toMap()
            out("""
                |
                |## Components
                |
                |""".trimMargin())
            logJson(components)
            documents =
                projectDesign.documents.map {
                    it to api.buildDocumentationFileSpecifications(
                        project,
                        requirements,
                        it
                    )
                }.toMap()
            out("""
                |
                |## Documents
                |
                |""".trimMargin())
            logJson(documents)
            tests = projectDesign.tests.map { it to api.buildTestFileSpecifications(project, requirements, it) }.toMap()
            out("""
                |
                |## Tests
                |
                |""".trimMargin())
            logJson(tests)
            implementations = parallelImplement(api, project, components, documents, tests, 1, 7)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (implementations != null) {
            val relative = "projects/${project?.name ?: UUID.randomUUID()}.zip"
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
            implementations.toList().sortedBy { it.first.fullFilePathName }.forEach { (file, sourceCodes) ->
                out(
                    """
                    |
                    |### ${file.fullFilePathName}
                    |
                    |```${sourceCodes!!.language.lowercase()}
                    |${sourceCodes.code}
                    |```
                    |
                    |""".trimMargin()
                )
            }
        }

    }
}