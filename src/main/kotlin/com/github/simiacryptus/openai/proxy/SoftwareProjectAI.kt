package com.github.simiacryptus.openai.proxy

import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

interface SoftwareProjectAI {
    fun newProject(description: String): Project

    data class Project(
        val name: String = "",
        val description: String = "",
        val language: String = "",
        val features: List<String> = listOf(),
        val libraries: List<String> = listOf(),
        val buildTools: List<String> = listOf(),
    )

    fun getProjectStatements(description: String, project: Project): ProjectStatements

    data class ProjectStatements(
        val assumptions: List<String> = listOf(),
        val designPatterns: List<String> = listOf(),
        val requirements: List<String> = listOf(),
        val risks: List<String> = listOf(),
    )

    fun buildProjectDesign(project: Project, requirements: ProjectStatements): ProjectDesign

    data class ProjectDesign(
        val components: List<ComponentDetails> = listOf(),
        val documents: List<DocumentationDetails> = listOf(),
        val tests: List<TestDetails> = listOf(),
    )

    data class ComponentDetails(
        val name: String = "",
        val description: String = "",
        val features: List<String> = listOf(),
    )

    data class TestDetails(
        val name: String = "",
        val steps: List<String> = listOf(),
        val expectations: List<String> = listOf(),
    )

    data class DocumentationDetails(
        val name: String = "",
        val description: String = "",
        val sections: List<String> = listOf(),
    )

    fun buildProjectFileSpecifications(
        project: Project,
        requirements: ProjectStatements,
        design: ProjectDesign,
        recursive: Boolean = true
    ): List<CodeSpecification>

    fun buildComponentFileSpecifications(
        project: Project,
        requirements: ProjectStatements,
        design: ComponentDetails,
        recursive: Boolean = true
    ): List<CodeSpecification>

    fun buildTestFileSpecifications(
        project: Project,
        requirements: ProjectStatements,
        design: TestDetails,
        recursive: Boolean = true
    ): List<TestSpecification>

    fun buildDocumentationFileSpecifications(
        project: Project,
        requirements: ProjectStatements,
        design: DocumentationDetails,
        recursive: Boolean = true
    ): List<DocumentSpecification>

    data class CodeSpecification(
        val description: String = "",
        val requires: List<FilePath> = listOf(),
        val publicProperties: List<String> = listOf(),
        val publicMethodSignatures: List<String> = listOf(),
        val language: String = "",
        val location: FilePath = FilePath(),
    )

    data class DocumentSpecification(
        val description: String = "",
        val requires: List<FilePath> = listOf(),
        val sections: List<String> = listOf(),
        val language: String = "",
        val location: FilePath = FilePath(),
    )

    data class TestSpecification(
        val description: String = "",
        val requires: List<FilePath> = listOf(),
        val steps: List<String> = listOf(),
        val expectations: List<String> = listOf(),
        val language: String = "",
        val location: FilePath = FilePath(),
    )

    data class FilePath(
        @Notes("e.g. projectRoot/README.md") val fullFilePathName: String = "",
    ) {
        override fun toString(): String {
            return fullFilePathName
        }
    }

    fun implementComponentSpecification(
        project: Project,
        specification: CodeSpecification,
        component: ComponentDetails,
        imports: List<Any>,
        specificationAgain: CodeSpecification,
    ): SourceCode


    fun implementTestSpecification(
        project: Project,
        specification: TestSpecification,
        test: TestDetails,
        imports: List<Any>,
        specificationAgain: TestSpecification,
    ): SourceCode


    fun implementDocumentationSpecification(
        project: Project,
        specification: DocumentSpecification,
        documentation: DocumentationDetails,
        imports: List<Any>,
        specificationAgain: DocumentSpecification,
    ): SourceCode

    data class SourceCode(
        val language: String = "",
        val code: String = "",
    )

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(SoftwareProjectAI::class.java)
        fun parallelImplement(
            api: SoftwareProjectAI,
            project: Project,
            components: Map<ComponentDetails, List<CodeSpecification>>?,
            documents: Map<DocumentationDetails, List<DocumentSpecification>>?,
            tests: Map<TestDetails, List<TestSpecification>>?,
            drafts: Int,
            threads: Int
        ): Map<FilePath, SourceCode?> = parallelImplementWithAlternates(
            api,
            project,
            components ?: mapOf(),
            documents ?: mapOf(),
            tests ?: mapOf(),
            drafts,
            threads
        ).mapValues { it.value.maxByOrNull { it.code.length } }

        fun parallelImplementWithAlternates(
            api: SoftwareProjectAI,
            project: Project,
            components: Map<ComponentDetails, List<CodeSpecification>>,
            documents: Map<DocumentationDetails, List<DocumentSpecification>>,
            tests: Map<TestDetails, List<TestSpecification>>,
            drafts: Int,
            threads: Int,
            progress: (Double) -> Unit = {}
        ): Map<FilePath, List<SourceCode>> {
            val threadPool = Executors.newFixedThreadPool(threads)
            try {
                val totalDrafts = (components + tests + documents).values.filterNotNull().sumOf { it.size } * drafts
                val currentDraft = AtomicInteger(0)
                val fileImplCache = ConcurrentHashMap<String, List<Future<Pair<FilePath, SourceCode>>>>()
                val normalizeFileName : (String) -> String = {
                    it.trimStart('/','.')
                }
                val componentImpl = components.filter { it.value != null }.flatMap { (component, files) ->
                    files.flatMap { file ->
                        fileImplCache.getOrPut(normalizeFileName(file.location.fullFilePathName)) {
                            (0 until drafts).map { _ ->
                                threadPool.submit(Callable {
                                    val implement = api.implementComponentSpecification(
                                        project,
                                        file.copy(requires = listOf()),
                                        component,
                                        files.filter { file.requires.contains(it.location) }.toList(),
                                        file.copy(requires = listOf())
                                    )
                                    (currentDraft.incrementAndGet().toDouble() / totalDrafts)
                                        .also { progress(it) }
                                        .also { log.info("Progress: $it") }
                                    file.location to implement
                                })
                            }
                        }
                    }
                }.toTypedArray().map {
                    try {
                        it.get()
                    } catch (e: Throwable) {
                        null
                    }
                }.filterNotNull().groupBy { it.first }
                    .mapValues { it.value.map { it.second }.sortedBy { it.code.length } }
                val testImpl = tests.filter { it.value != null }.flatMap { (testDetails, files) ->
                    files.flatMap { file ->
                        fileImplCache.getOrPut(normalizeFileName(file.location.fullFilePathName)) {
                            (0 until drafts).map { _ ->
                                threadPool.submit(Callable {
                                    val implement = api.implementTestSpecification(
                                        project,
                                        file,
                                        testDetails,
                                        files.filter { file.requires.contains(it.location) }.toList(),
                                        file
                                    )
                                    (currentDraft.incrementAndGet().toDouble() / totalDrafts)
                                        .also { progress(it) }
                                        .also { log.info("Progress: $it") }
                                    file.location to implement
                                })
                            }
                        }
                    }
                }.toTypedArray().map {
                    try {
                        it.get()
                    } catch (e: Throwable) {
                        null
                    }
                }.filterNotNull().groupBy { it.first }
                    .mapValues { it.value.map { it.second }.sortedBy { it.code.length } }
                val docImpl = documents.filter { it.value != null }.flatMap { (documentationDetails, files) ->
                    files.flatMap { file ->
                        fileImplCache.getOrPut(normalizeFileName(file.location.fullFilePathName)) {
                            (0 until drafts).map { _ ->
                                threadPool.submit(Callable {
                                    val implement = api.implementDocumentationSpecification(
                                        project,
                                        file,
                                        documentationDetails,
                                        files.filter { file.requires.contains(it.location) }.toList(),
                                        file,
                                    )
                                    (currentDraft.incrementAndGet().toDouble() / totalDrafts)
                                        .also { progress(it) }
                                        .also { log.info("Progress: $it") }
                                    file.location to implement
                                })
                            }
                        }
                    }
                }.toTypedArray().map {
                    try {
                        it.get()
                    } catch (e: Throwable) {
                        null
                    }
                }.filterNotNull().groupBy { it.first }
                    .mapValues { it.value.map { it.second }.sortedBy { it.code.length } }
                return componentImpl + docImpl + testImpl
            } finally {
                threadPool.shutdown()
            }
        }

        fun write(
            zipArchiveFile: File,
            implementations: Map<FilePath, SourceCode?>
        ) {
            ZipOutputStream(zipArchiveFile.outputStream()).use { zip ->
                implementations.forEach { (file, sourceCodes) ->
                    zip.putNextEntry(ZipEntry(file.toString()))
                    zip.write(sourceCodes!!.code.toByteArray())
                    zip.closeEntry()
                }
            }
        }
    }
}

