package com.github.simiacryptus.aicoder

import com.github.simiacryptus.openai.proxy.Description
import com.github.simiacryptus.openai.proxy.ValidatedObject
import java.io.File
import java.util.*
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
        val name: String? = "",
        val description: String? = "",
        val languages: List<String>? = listOf(),
        val features: List<String>? = listOf(),
        val libraries: List<String>? = listOf(),
        val buildTools: List<String>? = listOf(),
    ) : ValidatedObject

    fun getProjectStatements(description: String, project: Project): ProjectStatements

    data class ProjectStatements(
        val assumptions: List<String>? = listOf(),
        val designPatterns: List<String>? = listOf(),
        val requirements: List<String>? = listOf(),
        val risks: List<String>? = listOf(),
    ) : ValidatedObject

    fun buildProjectDesign(project: Project, requirements: ProjectStatements): ProjectDesign

    data class ProjectDesign(
        @Description("Major components e.g. 'core', 'ui', 'api', etc.")
        val components: List<ComponentDetails>? = listOf(),
        @Description("Documentation files e.g. README.md, LICENSE, etc.")
        val documents: List<DocumentationDetails>? = listOf(),
        @Description("Individual test cases")
        val testCases: List<TestCase>? = listOf(),
    ) : ValidatedObject

    data class ComponentDetails(
        val name: String? = "",
        val description: String? = "",
        val requirements: List<String>? = listOf(),
        val dependencies: List<String>? = listOf(),
    )

    data class TestCase(
        val name: String? = "",
        val steps: List<String>? = listOf(),
        val expectations: List<String>? = listOf(),
    ) : ValidatedObject

    data class DocumentationDetails(
        val name: String? = "",
        val description: String? = "",
        val sections: List<String>? = listOf(),
    ) : ValidatedObject

    data class CodeSpecifications(
        val specifications: List<CodeSpecification>? = listOf(),
    ) : ValidatedObject

    fun getComponentFiles(
        project: Project,
        requirements: ProjectStatements,
        design: ComponentDetails,
    ): CodeSpecifications

    fun getTestFiles(
        project: Project,
        requirements: ProjectStatements,
        test: TestCase,
    ): TestSpecifications

    data class TestSpecifications(
        val specifications: List<TestSpecification>? = listOf(),
    ) : ValidatedObject

    fun buildDocumentationFileSpecifications(
        project: Project,
        requirements: ProjectStatements,
        design: DocumentationDetails,
    ): DocumentSpecifications

    data class DocumentSpecifications(
        @Description("Specifications for each human-language document. Does not include code files.")
        val documents: List<DocumentSpecification>? = listOf(),
    ) : ValidatedObject

    data class CodeSpecification(
        val description: String? = "",
        val requires: List<FilePath>? = listOf(),
        val publicProperties: List<String>? = listOf(),
        val publicMethodSignatures: List<String>? = listOf(),
        val language: String? = "",
        val location: FilePath? = FilePath(),
    ) : ValidatedObject {
        override fun validate(): Boolean {
            if(description?.isBlank() != false) return false
            return super.validate()
        }
    }

    data class DocumentSpecification(
        val description: String? = "",
        val requires: List<FilePath>? = listOf(),
        val sections: List<String>? = listOf(),
        val language: String? = "",
        val location: FilePath? = FilePath(),
    ) : ValidatedObject {
        override fun validate(): Boolean {
            if(description?.isBlank() != false) return false
            return super.validate()
        }
    }

    data class TestSpecification(
        val description: String? = "",
        val requires: List<FilePath>? = listOf(),
        val steps: List<String>? = listOf(),
        val expectations: List<String>? = listOf(),
        val language: String? = "",
        val location: FilePath? = FilePath(),
    ) : ValidatedObject {
        override fun validate(): Boolean {
            if(description?.isBlank() != false) return false
            return super.validate()
        }
    }

    data class FilePath(
        @Description("File name relative to project root, e.g. src/main/java/Foo.java")
        val file: String? = "",
    ) : ValidatedObject {
        override fun toString(): String {
            return file ?: ""
        }

        override fun validate(): Boolean {
            if (file?.isBlank() != false) return false
            return super.validate()
        }
    }

    fun implementCode(
        project: Project,
        component: ComponentDetails,
        imports: List<Any>,
        specification: CodeSpecification,
    ): SourceCode


    fun implementTest(
        project: Project,
        test: TestCase,
        imports: List<Any>,
        specification: TestSpecification,
    ): SourceCode


    fun writeDocument(
        project: Project,
        documentation: DocumentationDetails,
        imports: List<Any>,
        specification: DocumentSpecification,
    ): Document

    data class Document(
        @Description("e.g. \"markdown\" or \"text\"")
        val language: String? = "",
        @Description("Complete Document Text")
        val text: String? = "",
    ) : ValidatedObject

    data class SourceCode(
        @Description("e.g. \"java\" or \"kotlin\"")
        val language: String? = "",
        @Description("Raw File Contents")
        val code: String? = "",
    ) : ValidatedObject

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(SoftwareProjectAI::class.java)
        fun parallelImplement(
            api: SoftwareProjectAI,
            project: Project,
            components: Map<ComponentDetails, CodeSpecifications>?,
            documents: Map<DocumentationDetails, DocumentSpecifications>?,
            tests: Map<TestCase, TestSpecifications>?,
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
        ).mapValues { it.value.maxByOrNull { it.code?.length ?: 0 } }

        fun parallelImplementWithAlternates(
            api: SoftwareProjectAI,
            project: Project,
            components: Map<ComponentDetails, CodeSpecifications>,
            documents: Map<DocumentationDetails, DocumentSpecifications>,
            tests: Map<TestCase, TestSpecifications>,
            drafts: Int,
            threads: Int,
            progress: (Double) -> Unit = {}
        ): Map<FilePath, List<SourceCode>> {
            val threadPool = Executors.newFixedThreadPool(threads)
            try {
                val totalDrafts = (
                        components.values.sumOf { it.specifications?.size ?: 0 } +
                                tests.values.sumOf { it.specifications?.size ?: 0 } +
                                documents.values.sumOf { it.documents?.size ?: 0 }
                        ) * drafts
                val currentDraft = AtomicInteger(0)
                val fileImplCache = ConcurrentHashMap<String, List<Future<Pair<FilePath, SourceCode>>>>()
                val normalizeFileName: (String?) -> String = {
                    it?.trimStart('/', '.') ?: ""
                }

                // Build Components
                fun buildCodeSpec(
                    component: ComponentDetails,
                    files: List<CodeSpecification>,
                    file: CodeSpecification
                ): List<Future<Pair<FilePath, SourceCode>>> {
                    if (file.location == null) {
                        return emptyList()
                    }
                    return fileImplCache.getOrPut(normalizeFileName(file.location.file)) {
                        (0 until drafts).map { _ ->
                            threadPool.submit(Callable {
                                val implement = api.implementCode(
                                    project,
                                    component,
                                    files.filter { file.requires?.contains(it.location) ?: false }.toList(),
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

                fun buildComponentDetails(
                    component: ComponentDetails,
                    files: List<CodeSpecification>
                ): List<Future<Pair<FilePath, SourceCode>>> {
                    return files.flatMap(fun(file: CodeSpecification): List<Future<Pair<FilePath, SourceCode>>> {
                        return buildCodeSpec(component, files, file)
                    }).toTypedArray().toList()
                }

                val componentFutures = components.flatMap { (component, files) ->
                    buildComponentDetails(component, files.specifications ?: listOf())
                }.toTypedArray()

                // Build Documents
                fun buildDocumentSpec(
                    documentation: DocumentationDetails,
                    files: List<DocumentSpecification>,
                    file: DocumentSpecification
                ): List<Future<Pair<FilePath, SourceCode>>> {
                    if (file.location == null) {
                        return emptyList()
                    }
                    return fileImplCache.getOrPut(normalizeFileName(file.location.file)) {
                        (0 until drafts).map { _ ->
                            threadPool.submit(Callable {
                                val implement = api.writeDocument(
                                    project,
                                    documentation,
                                    files.filter { file.requires?.contains(it.location) ?: false }.toList(),
                                    file.copy(requires = listOf())
                                )
                                (currentDraft.incrementAndGet().toDouble() / totalDrafts)
                                    .also { progress(it) }
                                    .also { log.info("Progress: $it") }
                                file.location to SourceCode(
                                    language = implement.language,
                                    code = implement.text
                                )
                            })
                        }
                    }
                }

                fun buildDocumentDetails(
                    documentation: DocumentationDetails,
                    files: List<DocumentSpecification>
                ): List<Future<Pair<FilePath, SourceCode>>> {
                    return files.flatMap(fun(file: DocumentSpecification): List<Future<Pair<FilePath, SourceCode>>> {
                        return buildDocumentSpec(documentation, files, file)
                    }).toTypedArray().toList()
                }

                val documentFutures = documents.flatMap { (documentation, files) ->
                    buildDocumentDetails(documentation, files.documents ?: listOf())
                }.toTypedArray()

                // Build Tests
                fun buildTestSpec(
                    test: TestCase,
                    files: List<TestSpecification>,
                    file: TestSpecification
                ): List<Future<Pair<FilePath, SourceCode>>> {
                    if (file.location == null) {
                        return emptyList()
                    }
                    return fileImplCache.getOrPut(normalizeFileName(file.location.file)) {
                        (0 until drafts).map { _ ->
                            threadPool.submit(Callable {
                                val implement = api.implementTest(
                                    project,
                                    test,
                                    files.filter { file.requires?.contains(it.location) ?: false }.toList(),
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

                fun buildTestDetails(
                    test: TestCase,
                    files: List<TestSpecification>
                ): List<Future<Pair<FilePath, SourceCode>>> {
                    return files.flatMap(fun(file: TestSpecification): List<Future<Pair<FilePath, SourceCode>>> {
                        return buildTestSpec(test, files, file)
                    }).toTypedArray().toList()
                }

                val testFutures = tests.flatMap { (test, files) ->
                    buildTestDetails(test, files.specifications ?: listOf())
                }.toTypedArray()

                return (getAll(componentFutures) + getAll(documentFutures) + getAll(testFutures)).mapValues {
                    it.value.map { it.second }.sortedBy { it.code?.length ?: 0 }
                }
            } finally {
                threadPool.shutdown()
            }
        }

        private fun <K, V> getAll(testFutures: Array<Future<Pair<K, V>>>) =
            testFutures.map {
                try {
                    Optional.ofNullable(it.get())
                } catch (e: Throwable) {
                    Optional.empty()
                }
            }.filter { !it.isEmpty }.map { it.get() }.groupBy { it.first }

        fun write(
            zipArchiveFile: File,
            implementations: Map<FilePath, SourceCode?>
        ) {
            ZipOutputStream(zipArchiveFile.outputStream()).use { zip ->
                implementations.forEach { (file, sourceCodes) ->
                    zip.putNextEntry(ZipEntry(file.toString()))
                    zip.write(sourceCodes!!.code?.toByteArray() ?: byteArrayOf())
                    zip.closeEntry()
                }
            }
        }
    }


}

