package com.github.simiacryptus.aicoder.proxy

import com.github.simiacryptus.aicoder.openai.proxy.SoftwareProjectAI
import org.junit.Test
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
        logJson: (Any) -> Unit,
        out: (Any) -> Unit
    ) {
        val project = api.newProject(
            """
                    |
                    |Slack bot to monitor a support alias and automatically respond to common questions
                    |
                    |Language: Kotlin
                    |
                    """.trimMargin().trim()
        )
        logJson(project)
        out(
            """
                    |
                    |# ${project.name}
                    |
                    |${project.description}
                    |
                    |Language: ${project.language}
                    |
                    |Libraries: ${project.libraries.joinToString(", ")}
                    |
                    |Build Tools: ${project.buildTools.joinToString(", ")}
                    |
                    |""".trimMargin()
        )
        val requirements = api.getProjectStatements(project)
        logJson(requirements)

        val projectDesign = api.buildProjectDesign(project, requirements)
        logJson(projectDesign)

        val files = api.buildProjectFileSpecifications(project, requirements, projectDesign)
        logJson(files)


        val zipArchiveFile = outputDir.resolve("projects/${project.name}.zip")
        zipArchiveFile.parentFile.mkdirs()
        out(
            """
                    |
                    |## Project Files
                    |
                    |[Download](projects/${project.name}.zip)
                    |
                    |""".trimMargin()
        )
        ZipOutputStream(zipArchiveFile.outputStream()).use { zip ->
            for (file in files.files) {
            }
            for (file in files.files) {
                val sourceCode = api.implement(
                    project,
                    files.files.map { it.location }.filter { file.requires.contains(it) }.toList(),
                    file
                )
                zip.putNextEntry(ZipEntry(file.location.toString()))
                zip.write(sourceCode.code.toByteArray())
                zip.closeEntry()
                out(
                    """
                            |
                            |## ${file.location.name}.${file.location.extension}
                            |
                            |${file.description}
                            |
                            |```${sourceCode.language}
                            |${sourceCode.code}
                            |```
                            |
                            |""".trimMargin()
                )
            }
        }
    }


}