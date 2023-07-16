package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.openai.proxy.ValidatedObject
import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import kotlin.Pair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GenerateProjectAction extends FileContextAction<Settings> {
    static Logger logger = LoggerFactory.getLogger(GenerateProjectAction.class)


    GenerateProjectAction() {
        super(false,true)
        setDevAction(true)
    }

    static String trimStart(String s1, char[] prefixChars) {
        String s = s1
        while (s.length() > 0 && Arrays.asList(prefixChars).contains(s.charAt(0))) {
            s = s.substring(1)
        }
        return s
    }

    interface SoftwareProjectAI {

        Project newProject(String description)

        class Project implements ValidatedObject {
            String name = ''
            String description = ''
            String language = ''
            List<String> features = []
            List<String> libraries = []
            List<String> buildTools = []

            boolean validate() {
                return true
            }
        }

        ProjectStatements getProjectStatements(String description, Project project)

        class ProjectStatements implements ValidatedObject {
            List<String> assumptions = []
            List<String> designPatterns = []
            List<String> requirements = []
            List<String> risks = []

            boolean validate() {
                return true
            }
        }

        ProjectDesign buildProjectDesign(Project project, ProjectStatements requirements)

        class ProjectDesign implements ValidatedObject {
            List<ComponentDetails> components = []
            List<DocumentationDetails> documents = []
            List<TestDetails> tests = []

            boolean validate() {
                return true
            }
        }

        class ComponentDetails implements ValidatedObject {
            String name = ''
            String description = ''
            List<String> features = []

            boolean validate() {
                return true
            }
        }

        class TestDetails implements ValidatedObject {
            String name = ''
            List<String> steps = []
            List<String> expectations = []

            boolean validate() {
                return true
            }
        }

        class DocumentationDetails implements ValidatedObject {
            String name = ''
            String description = ''
            List<String> sections = []

            boolean validate() {
                return true
            }
        }

        List<CodeSpecification> buildProjectFileSpecifications(Project project, ProjectStatements requirements, ProjectDesign design, boolean recursive)

        List<CodeSpecification> buildComponentFileSpecifications(Project project, ProjectStatements requirements, ComponentDetails design)

        List<TestSpecification> buildTestFileSpecifications(Project project, ProjectStatements requirements, TestDetails design, boolean recursive)

        List<DocumentSpecification> buildDocumentationFileSpecifications(Project project, ProjectStatements requirements, DocumentationDetails design, boolean recursive)

        class CodeSpecification implements ValidatedObject {
            String description = ''
            List<FilePath> requires = []
            List<String> publicProperties = []
            List<String> publicMethodSignatures = []
            String language = ''
            FilePath location = new FilePath()

            boolean validate() {
                return true
            }
        }

        class DocumentSpecification implements ValidatedObject {
            String description = ''
            List<FilePath> requires = []
            List<String> sections = []
            String language = ''
            FilePath location = new FilePath()

            boolean validate() {
                return true
            }
        }

        class TestSpecification implements ValidatedObject {
            String description = ''
            List<FilePath> requires = []
            List<String> steps = []
            List<String> expectations = []
            String language = ''
            FilePath location = new FilePath()

            boolean validate() {
                return true
            }
        }

        @ToString(includeNames = true)
        @EqualsAndHashCode()
        @Canonical
        class FilePath implements ValidatedObject {
            String file = ''

            boolean validate() {
                return file?.isBlank() == false
            }
        }

        SourceCode implementComponentSpecification(Project project, ComponentDetails component, List imports, CodeSpecification specification)

        SourceCode implementTestSpecification(Project project, TestSpecification specification, TestDetails test, List imports, TestSpecification specificationAgain)

        SourceCode implementDocumentationSpecification(Project project, DocumentSpecification specification, DocumentationDetails documentation, List imports, DocumentSpecification specificationAgain)

        @ToString(includeNames = true)
        @EqualsAndHashCode()
        @Canonical
        class SourceCode implements ValidatedObject {
            String language = ''
            String code = ''

            boolean validate() {
                return true
            }
        }
    }


    Map<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode> parallelImplement(
            SoftwareProjectAI api,
            Project project,
            Map<SoftwareProjectAI.ComponentDetails, List<SoftwareProjectAI.CodeSpecification>> components = [:],
            Map<SoftwareProjectAI.DocumentationDetails, List<SoftwareProjectAI.DocumentSpecification>> documents = [:],
            Map<SoftwareProjectAI.TestDetails, List<SoftwareProjectAI.TestSpecification>> tests = [:],
            int drafts,
            int threads
    ) {
        Map<SoftwareProjectAI.FilePath, List<SoftwareProjectAI.SourceCode>> alternates = parallelImplementWithAlternates(
                api,
                project,
                components,
                documents,
                tests,
                drafts,
                threads,
                { -> return 0.0 }
        )
        alternates.collectEntries { [(it.key): it.value.max { it.code?.length() ?: 0 }] }
    }

    void write(
            File zipArchiveFile,
            Map<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode> implementations
    ) {
        new ZipOutputStream(zipArchiveFile.newOutputStream()).with { zip ->
            implementations.each { file, sourceCodes ->
                zip.putNextEntry(new ZipEntry(file.toString()))
                zip.write(sourceCodes.code.bytes)
                zip.closeEntry()
            }
        }
    }

    Map<SoftwareProjectAI.FilePath, List<SoftwareProjectAI.SourceCode>> parallelImplementWithAlternates(
            SoftwareProjectAI projectAI,
            Project project,
            Map<SoftwareProjectAI.ComponentDetails, List<SoftwareProjectAI.CodeSpecification>> components,
            Map<SoftwareProjectAI.DocumentationDetails, List<SoftwareProjectAI.DocumentSpecification>> documents,
            Map<SoftwareProjectAI.TestDetails, List<SoftwareProjectAI.TestSpecification>> tests,
            int drafts,
            int threads,
            Closure<Double> progress
    ) {
        def threadPool = Executors.newFixedThreadPool(threads)
        try {
            def totalDrafts = (
                    components.values.toList().flatten().size() +
                            tests.values.toList().flatten().size() +
                            documents.values.toList().flatten().size()
            ) * drafts


            return components.collectMany { entry ->
                buildComponentDetails(entry.key, entry.value) + buildDocumentDetails(entry.key, entry.value) + buildTestDetails(entry.key, entry.value)
            }.collect {
                try {
                    Optional.ofNullable(it.get())
                } catch (Throwable ignored) {
                    Optional.<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>> empty()
                }
            }.findAll { !it.empty }.collect { it.get() }
                    .groupBy { it.first }.collectEntries { Map.Entry<SoftwareProjectAI.FilePath, List<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>> entry ->
                [(entry.key): entry.value.sort { Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode> p -> p.second.code.length() }]
            }
        } finally {
            threadPool.shutdown()
        }
    }

    AtomicInteger currentDraft = new AtomicInteger(0)
    ConcurrentHashMap<String, List<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>> fileImplCache = new ConcurrentHashMap<String, List<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>>()

    def normalizeFileName(String it) {
        trimStart(it, ['/', '.']) ?: ""
    }


    def buildComponentDetails(SoftwareProjectAI.ComponentDetails component, List<SoftwareProjectAI.CodeSpecification> files) {
        files.collectMany { SoftwareProjectAI.CodeSpecification file ->
            //buildCodeSpec(component, files, file)
            if (file.location == null) {
                return new ArrayList<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>()
            }
            fileImplCache.getOrPut(normalizeFileName(file.location.file)) {
                (0..<drafts).collect { _ ->
                    threadPool.submit(new Callable<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>() {
                        @Override
                        Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode> call() throws Exception {
                            SoftwareProjectAI.SourceCode implement = api.implementComponentSpecification(
                                    project,
                                    component,
                                    files.findAll { file.requires?.contains(it.location) ?: false },
                                    new SoftwareProjectAI.CodeSpecification(
                                            description: file.description,
                                            requires: [],
                                            publicProperties: file.publicProperties,
                                            publicMethodSignatures: file.publicMethodSignatures,
                                            language: file.language,
                                            location: file.location
                                    )
                            )
                            def progressVal = currentDraft.incrementAndGet().toDouble() / totalDrafts
                            progress(progressVal)
                            logger.info("Progress: $progressVal")
                            return new Pair(file.location, implement)
                        }
                    })
                }
            }

        }
    }

    def buildDocumentDetails(SoftwareProjectAI.DocumentationDetails documentation, List<SoftwareProjectAI.DocumentSpecification> files) {
        files.collectMany { SoftwareProjectAI.DocumentSpecification file ->
            //buildDocumentSpec(documentation, files, file)
            if (file.location == null) {
                return new ArrayList<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>()
            }
            fileImplCache.getOrPut(normalizeFileName(file.location.file)) {
                (0..<drafts).collect { _ ->
                    threadPool.submit(new Callable<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.DocumentSpecification>>() {

                        @Override
                        Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.DocumentSpecification> call() throws Exception {
                            def implement = projectAI.implementDocumentationSpecification(
                                    project,
                                    new SoftwareProjectAI.DocumentSpecification(
                                            description: file.description,
                                            requires: [],
                                            sections: file.sections,
                                            language: file.language,
                                            location: file.location
                                    ),
                                    documentation,
                                    files.findAll { file.requires?.contains(it.location) ?: false },
                                    new SoftwareProjectAI.DocumentSpecification(
                                            description: file.description,
                                            requires: [],
                                            sections: file.sections,
                                            language: file.language,
                                            location: file.location
                                    )
                            )
                            def progressVal = currentDraft.incrementAndGet().toDouble() / totalDrafts
                            progress(progressVal)
                            logger.info("Progress: $progressVal")
                            return new Pair(file.location, implement)
                        }
                    })
                }
            }
        }
    }


    def buildTestDetails(SoftwareProjectAI.TestDetails test, List<SoftwareProjectAI.TestSpecification> files) {
        files.collectMany { SoftwareProjectAI.TestSpecification file ->
            //buildTestSpec(test, files, file)
            if (file.location == null) {
                return new ArrayList<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>()
            }
            fileImplCache.getOrPut(normalizeFileName(file.location.file)) {
                def futures = (0..<drafts).collect { _ ->
                    def future = threadPool.submit(new Callable<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>() {
                        @Override
                        Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode> call() throws Exception {
                            def implement = api.implementTestSpecification(
                                    project,
                                    new SoftwareProjectAI.TestSpecification(
                                            description: file.description,
                                            requires: [],
                                            steps: file.steps,
                                            expectations: file.expectations,
                                            language: file.language,
                                            location: file.location
                                    ),
                                    test,
                                    files.findAll { file.requires?.contains(it.location) ?: false },
                                    new SoftwareProjectAI.TestSpecification(
                                            description: file.description,
                                            requires: [],
                                            steps: file.steps,
                                            expectations: file.expectations,
                                            language: file.language,
                                            location: file.location
                                    )
                            )
                            def progressVal = currentDraft.incrementAndGet().toDouble() / totalDrafts
                            progress(progressVal)
                            logger.info("Progress: $progressVal")
                            return new Pair(file.location, implement)
                        }
                    })
                    future
                }
                futures
            }
        }
    }

    @SuppressWarnings("UNUSED")
    class SettingsUI {
        @Name("Project Description")
        JTextArea description = new JTextArea()

        @Name("Drafts Per File")
        JTextField drafts = new JTextField("2")
        JCheckBox saveAlternates = new JCheckBox("Save Alternates")
    }

    static class Settings {
        String description = ""
        int drafts = 2
        boolean saveAlternates = false
    }

    @Override
    Settings getConfig(Project project) {
        return UITools.showDialog(project, SettingsUI.class, Settings.class)
    }

    @Override
    File[] processSelection(SelectionState state, Settings config) {
        if (config == null) return new File[0]

        SoftwareProjectAI projectAI = new ChatProxy<SoftwareProjectAI>(
                clazz: SoftwareProjectAI.class,
                api: api,
                model: AppSettingsState.instance.defaultChatModel(),
                temperature: AppSettingsState.instance.temperature,
                deserializerRetries: 2,
        ).create()

        SoftwareProjectAI.Project newProject = projectAI.newProject(config.description.trim())

        def projectStatements = projectAI.getProjectStatements(config.description, newProject)
        def buildProjectDesign = projectAI.buildProjectDesign(newProject, projectStatements)

        def components = buildProjectDesign.components.<SoftwareProjectAI.ComponentDetails,List<SoftwareProjectAI.CodeSpecification>,SoftwareProjectAI.ComponentDetails>collectEntries { SoftwareProjectAI.ComponentDetails details ->
            List<SoftwareProjectAI.CodeSpecification> specifications = projectAI.buildComponentFileSpecifications(
                    newProject,
                    projectStatements,
                    details
            )
            [details: specifications]
        }

        def documents = buildProjectDesign.documents.<SoftwareProjectAI.DocumentationDetails,List<SoftwareProjectAI.DocumentSpecification>,SoftwareProjectAI.DocumentationDetails>collectEntries {
            [(it): projectAI.buildDocumentationFileSpecifications(
                    newProject,
                    projectStatements,
                    it,
                    false
            )]
        }

        def tests = buildProjectDesign.tests.<SoftwareProjectAI.TestDetails,List<SoftwareProjectAI.TestSpecification>,SoftwareProjectAI.TestDetails>collectEntries {
            [(it): projectAI.buildTestFileSpecifications(
                    newProject,
                    projectStatements,
                    it,
                    false
            )]
        }
        def outputDir = new File(state.selectedFile.canonicalPath)

        def threadPool = Executors.newFixedThreadPool(4)
        Map<SoftwareProjectAI.FilePath, List<SoftwareProjectAI.SourceCode>> entries
        try {
//            def totalDrafts = (
//                    components.values.toList().flatten().size() +
//                            tests.values.toList().flatten().size() +
//                            documents.values.toList().flatten().size()
//            ) * drafts

            def groupBy = components.collectMany { entry ->
                buildComponentDetails(entry.key, entry.value) + buildDocumentDetails(entry.key, entry.value) + buildTestDetails(entry.key, entry.value)
            }.collect {
                try {
                    Optional.ofNullable(it.get())
                } catch (Throwable ignored) {
                    Optional.<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>> empty()
                }
            }.findAll { !it.empty }.collect { it.get() }
                    .groupBy { it.first }
            entries = groupBy.<SoftwareProjectAI.FilePath, List<SoftwareProjectAI.SourceCode>, SoftwareProjectAI.FilePath, List<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>> collectEntries { Map.Entry<SoftwareProjectAI.FilePath, List<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>> entry ->
                [(entry.key): entry.value.sort { Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode> p -> p.second.code.length() }]
            }
        } finally {
            threadPool.shutdown()
        }


        def generatedFiles = []
        entries.each { file, sourceCode ->
            def relative = trimStart(file.file?.trimEnd('/'), ['/', '.']) ?: ""
            if (new File(relative).isAbsolute()) {
                logger.warn("Invalid path: $relative")
            } else {
                def outFile = new File(outputDir, relative)
                outFile.parentFile.mkdirs()
                def best = sourceCode.max { it.code?.length() ?: 0 }
                outFile.text = best.code ?: ""
                logger.debug("Wrote ${outFile.canonicalPath} (Resolved from $relative)")
                generatedFiles << outFile
                if (config.saveAlternates)
                    sourceCode.findAll { it != best }.eachWithIndex { alternate, index ->
                        def outFileAlternate = new File(outputDir, relative + ".${index + 1}")
                        outFileAlternate.parentFile.mkdirs()
                        outFileAlternate.text = alternate.code ?: ""
                        logger.debug("Wrote ${outFileAlternate.canonicalPath} (Resolved from $relative)")
                        generatedFiles << outFileAlternate
                    }
            }
        }
        return generatedFiles as File[]
    }


}