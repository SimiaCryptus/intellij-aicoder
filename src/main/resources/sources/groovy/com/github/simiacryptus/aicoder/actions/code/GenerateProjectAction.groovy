package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.actions.FileContextAction
import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.project.Project
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.openai.proxy.ValidatedObject
import kotlin.Pair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GenerateProjectAction extends FileContextAction<Settings> {
    static Logger logger = LoggerFactory.getLogger(GenerateProjectAction.class)


    GenerateProjectAction() {
        super(false, true)
    }

    static String trimStart(String s1, List<Character> prefixChars) {
        String s = s1
        while (s.length() > 0 && prefixChars.contains(s.charAt(0))) {
            s = s.substring(1)
        }
        return s
    }

    static String trimEnd(String string, String suffix) {
        if (string.endsWith(suffix)) {
            return string.substring(0, string.length() - suffix.length())
        } else {
            return string
        }
    }

    interface SoftwareProjectAI {

        Project newProject(String description)

        class Project implements ValidatedObject {
            public String name = ''
            public String description = ''
            public String language = ''
            public List<String> features = []
            public List<String> libraries = []
            public List<String> buildTools = []

            Project() {}
            boolean validate() {
                return true
            }
        }

        ProjectStatements getProjectStatements(String description, Project project)

        class ProjectStatements implements ValidatedObject {
            public List<String> assumptions = []
            public List<String> designPatterns = []
            public List<String> requirements = []
            public List<String> risks = []

            ProjectStatements() {}

            boolean validate() {
                return true
            }
        }

        ProjectDesign buildProjectDesign(Project project, ProjectStatements requirements)

        class ProjectDesign implements ValidatedObject {
            public List<ComponentDetails> components = []
            public List<DocumentationDetails> documents = []
            public List<TestDetails> tests = []

            ProjectDesign() {}

            boolean validate() {
                return true
            }
        }

        class ComponentDetails implements ValidatedObject {
            public String name = ''
            public String description = ''
            public List<String> features = []

            ComponentDetails() {}
            boolean validate() {
                return true
            }
        }

        class TestDetails implements ValidatedObject {
            public String name = ''
            public List<String> steps = []
            public List<String> expectations = []

            TestDetails() {}
            boolean validate() {
                return true
            }
        }

        class DocumentationDetails implements ValidatedObject {
            public String name = ''
            public String description = ''
            public List<String> sections = []

            DocumentationDetails() {}

            boolean validate() {
                return true
            }
        }

        CodeSpecificationList buildProjectFileSpecifications(Project project, ProjectStatements requirements, ProjectDesign design, boolean recursive)

        class CodeSpecificationList implements ValidatedObject {
            public List<CodeSpecification> files = []

            CodeSpecificationList() {}
            boolean validate() {
                return true
            }
        }

        CodeSpecificationList buildComponentFileSpecifications(Project project, ProjectStatements requirements, ComponentDetails design)

        TestSpecificationList buildTestFileSpecifications(Project project, ProjectStatements requirements, TestDetails design, boolean recursive)

        class TestSpecificationList implements ValidatedObject {
            public List<TestSpecification> files = []

            TestSpecificationList() {}
            boolean validate() {
                return true
            }
        }

        DocumentSpecificationList buildDocumentationFileSpecifications(Project project, ProjectStatements requirements, DocumentationDetails design, boolean recursive)

        class DocumentSpecificationList implements ValidatedObject {
            public List<DocumentSpecification> files = []

            DocumentSpecificationList() {}
            boolean validate() {
                return true
            }
        }

        class CodeSpecification implements ValidatedObject {
            public String description = ''
            public List<FilePath> requires = []
            public List<String> publicProperties = []
            public List<String> publicMethodSignatures = []
            public String language = ''
            public FilePath location = new FilePath()

            CodeSpecification() {}
            boolean validate() {
                return true
            }
        }

        class DocumentSpecification implements ValidatedObject {
            public String description = ''
            public List<FilePath> requires = []
            public List<String> sections = []
            public String language = ''
            public FilePath location = new FilePath()

            DocumentSpecification() {}
            boolean validate() {
                return true
            }
        }

        class TestSpecification implements ValidatedObject {
            public String description = ''
            public List<FilePath> requires = []
            public List<String> steps = []
            public List<String> expectations = []
            public String language = ''
            public FilePath location = new FilePath()

            TestSpecification() {}
            boolean validate() {
                return true
            }
        }

        class FilePath implements ValidatedObject {
            public String file = ''

            FilePath() {}

            boolean validate() {
                return file?.isBlank() == false
            }
        }

        SourceCode implementComponentSpecification(Project project, ComponentDetails component, List imports, CodeSpecification specification)

        SourceCode implementTestSpecification(Project project, TestSpecification specification, TestDetails test, List imports, TestSpecification specificationAgain)

        SourceCode implementDocumentationSpecification(Project project, DocumentSpecification specification, DocumentationDetails documentation, List imports, DocumentSpecification specificationAgain)

        class SourceCode implements ValidatedObject {
            public String language = ''
            public String code = ''

            SourceCode() {}
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

    private static final ExecutorService threadPool = Executors.newCachedThreadPool()

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
            return (components.collectMany { entry -> buildComponentDetails(project, entry.key, entry.value) } +
                    documents.collectMany { entry -> buildDocumentDetails(project, entry.key, entry.value) } +
                    tests.collectMany { entry -> buildTestDetails(project, entry.key, entry.value) }).collect {
                try {
                    Optional.ofNullable(it.get())
                } catch (Throwable ignored) {
                    Optional.<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>> empty()
                }
            }.findAll { !it.empty }.collect { it.get() }
                    .groupBy { it.first }.collectEntries { Map.Entry<SoftwareProjectAI.FilePath, List<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>> entry ->
                [(entry.key): entry.value.sort { Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode> p -> p.second.code.length() }]
            }
    }

    AtomicInteger currentDraft = new AtomicInteger(0)
    ConcurrentHashMap<String, List<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>> fileImplCache = new ConcurrentHashMap<String, List<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>>()

    static def normalizeFileName(String it) {
        trimStart(it, ['/', '.'])
    }

    def buildComponentDetails(SoftwareProjectAI.Project project, SoftwareProjectAI.ComponentDetails component, List<SoftwareProjectAI.CodeSpecification> files, int drafts) {
        files.collectMany { SoftwareProjectAI.CodeSpecification file ->
            //buildCodeSpec(component, files, file)
            if (file.location == null) {
                return new ArrayList<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>()
            }

            def key = normalizeFileName(file.location.file)
            if(!fileImplCache.containsKey(key)) {
                def value = (0..<drafts).collect { _ ->
                    threadPool.submit(new Callable<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>() {
                        @Override
                        Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode> call() throws Exception {
                            SoftwareProjectAI.SourceCode implement = projectAI.implementComponentSpecification(
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
//                            def progressVal = currentDraft.incrementAndGet().toDouble() / totalDrafts
//                            progress(progressVal)
//                            logger.info("Progress: $progressVal")
                            return new Pair(file.location, implement)
                        }
                    })
                }
                fileImplCache.put(key, value)
            }
            return fileImplCache.get(key)

        }
    }

    def buildDocumentDetails(SoftwareProjectAI.Project project, SoftwareProjectAI.DocumentationDetails documentation, List<SoftwareProjectAI.DocumentSpecification> files, int drafts) {
        files.collectMany { SoftwareProjectAI.DocumentSpecification file ->
            //buildDocumentSpec(documentation, files, file)
            if (file.location == null) {
                return new ArrayList<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>()
            }
            def key = normalizeFileName(file.location.file)
            if(!fileImplCache.containsKey(key)) {
                def value = (0..<drafts).collect { _ ->
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
//                            def progressVal = currentDraft.incrementAndGet().toDouble() / totalDrafts
//                            progress(progressVal)
//                            logger.info("Progress: $progressVal")
                            return new Pair(file.location, implement)
                        }
                    })
                }
                fileImplCache.put(key, value)
            }
            return fileImplCache.get(key)
        }
    }

    def buildTestDetails(SoftwareProjectAI.Project project, SoftwareProjectAI.TestDetails test, List<SoftwareProjectAI.TestSpecification> files, int drafts) {
        files.collectMany { SoftwareProjectAI.TestSpecification file ->
            //buildTestSpec(test, files, file)
            if (file.location == null) {
                return new ArrayList<Future<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>>()
            }

            def key = normalizeFileName(file.location.file)
            if(!fileImplCache.containsKey(key)) {
                def value = (0..<drafts).collect { _ ->
                    threadPool.submit(new Callable<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>() {
                        @Override
                        Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode> call() throws Exception {
                            def implement = projectAI.implementTestSpecification(
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
//                            def progressVal = currentDraft.incrementAndGet().toDouble() / totalDrafts
//                            progress(progressVal)
//                            logger.info("Progress: $progressVal")
                            return new Pair(file.location, implement)
                        }
                    })
                }
                fileImplCache.put(key, value)
            }
            return fileImplCache.get(key)
        }
    }

    @SuppressWarnings("UNUSED")
    static class SettingsUI {
        @Name("Project Description")
        public JTextArea description = new JTextArea()

        @Name("Drafts Per File")
        public JTextField drafts = new JTextField("2")
        public JCheckBox saveAlternates = new JCheckBox("Save Alternates")

        SettingsUI() {
            description.setLineWrap(true)
            description.setWrapStyleWord(true)
        }
    }

    static class Settings {
        public String description = ""
        public int drafts = 2
        public boolean saveAlternates = false

        Settings() {}
    }

    @Override
    Settings getConfig(Project project) {
        return UITools.showDialog(project, SettingsUI.class, Settings.class, "Project Settings", { config -> })
    }

    SoftwareProjectAI projectAI = null

    @Override
    File[] processSelection(SelectionState state, Settings config) {
        projectAI = new ChatProxy<SoftwareProjectAI>(
                clazz: SoftwareProjectAI.class,
                api: api,
                model: AppSettingsState.instance.defaultChatModel(),
                temperature: AppSettingsState.instance.temperature,
                deserializerRetries: 2,
        ).create()
        if (config == null) return new File[0]


        SoftwareProjectAI.Project project = projectAI.newProject(config.description.trim())
        def projectStatements = projectAI.getProjectStatements(config.description, project)
        def buildProjectDesign = projectAI.buildProjectDesign(project, projectStatements)

        def components = buildProjectDesign.components.<SoftwareProjectAI.ComponentDetails, List<SoftwareProjectAI.CodeSpecification>, SoftwareProjectAI.ComponentDetails> collectEntries {
                [(it): projectAI.buildComponentFileSpecifications(
                        project,
                        projectStatements,
                        it
                ).files]
        }

        def documents = buildProjectDesign.documents.<SoftwareProjectAI.DocumentationDetails, List<SoftwareProjectAI.DocumentSpecification>, SoftwareProjectAI.DocumentationDetails> collectEntries {
            [(it): projectAI.buildDocumentationFileSpecifications(
                    project,
                    projectStatements,
                    it,
                    false
            ).files]
        }

        def tests = buildProjectDesign.tests.<SoftwareProjectAI.TestDetails, List<SoftwareProjectAI.TestSpecification>, SoftwareProjectAI.TestDetails> collectEntries {
            [(it): projectAI.buildTestFileSpecifications(
                    project,
                    projectStatements,
                    it,
                    false
            ).files]
        }
        def outputDir = new File(state.selectedFile.canonicalPath)

        def entries = (
                components.collectMany { entry -> buildComponentDetails(project, entry.key, entry.value, config.drafts) }
                        + documents.collectMany { entry -> buildDocumentDetails(project, entry.key, entry.value, config.drafts) }
                        + tests.collectMany { entry -> buildTestDetails(project, entry.key, entry.value, config.drafts) })
                .collect {
                    try {
                        Optional.ofNullable(it.get())
                    } catch (Throwable ignored) {
                        Optional.<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>> empty()
                    }
                }.findAll { !it.empty }.collect { it.get() }
                .groupBy { it.first }.<SoftwareProjectAI.FilePath, List<SoftwareProjectAI.SourceCode>, SoftwareProjectAI.FilePath, List<Pair<SoftwareProjectAI.FilePath, SoftwareProjectAI.SourceCode>>> collectEntries {
            entry -> [(entry.key): entry.value.collect { it.second}.sort { a, b -> a.code.length() <=> b.code.length()}]
        }


        def generatedFiles = []
        entries.each { file, sourceCode ->
            def relative = trimStart(trimEnd(file.file, '/'), ['/', '.']) ?: ""
            if (new File(relative).isAbsolute()) {
                logger.warn("Invalid path: $relative")
            } else {
                def outFile = new File(outputDir, relative)
                outFile.parentFile.mkdirs()
                def best = sourceCode.max { it.code.length() }
                outFile.text = best.code
                logger.debug("Wrote ${outFile.canonicalPath} (Resolved from $relative)")
                generatedFiles << outFile
                if (config.saveAlternates)
                    sourceCode.findAll { it != best }.eachWithIndex { alternate, index ->
                        def outFileAlternate = new File(outputDir, relative + ".${index + 1}")
                        outFileAlternate.parentFile.mkdirs()
                        outFileAlternate.text = alternate.code
                        logger.debug("Wrote ${outFileAlternate.canonicalPath} (Resolved from $relative)")
                        generatedFiles << outFileAlternate
                    }
            }
        }
        return generatedFiles as File[]
    }


}