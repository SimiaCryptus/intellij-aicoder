package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.config.Name
import com.github.simiacryptus.aicoder.util.UITools
import com.github.simiacryptus.aicoder.SoftwareProjectAI
import com.simiacryptus.openai.proxy.ChatProxy
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.simiacryptus.openai.OpenAIClient
import java.io.File
import javax.swing.JCheckBox
import javax.swing.JTextArea
import javax.swing.JTextField

class GenerateProjectAction : AnAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled()
        super.update(e)
    }

    @Suppress("UNUSED")
    class SettingsUI {
        @Name("Project Description")
        val description: JTextArea = JTextArea()
        @Name("Drafts Per File")
        val drafts: JTextField = JTextField("2")
        val saveAlternates: JCheckBox = JCheckBox("Save Alternates")
    }

    data class Settings(
        var description: String = "",
        var drafts: Int = 2,
        var saveAlternates: Boolean = false
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
        val api = ChatProxy(
            SoftwareProjectAI::class.java,
            api = UITools.api,
            maxTokens = AppSettingsState.instance.maxTokens,
            deserializerRetries = 5,
        ).create()
        val project = UITools.run(
            e.project, "Parsing Request", true
        ) {
            val newProject = api.newProject("""
                ${config.description}
                """.trimIndent().trim())
            if (it.isCanceled) throw InterruptedException()
            newProject
        }
        val requirements = UITools.run(
            e.project, "Specifying Project", true
        ) {
            val projectStatements = api.getProjectStatements(config.description, project)
            if (it.isCanceled) throw InterruptedException()
            projectStatements
        }
        val projectDesign = UITools.run(
            e.project, "Designing Project", true
        ) {
            val buildProjectDesign = api.buildProjectDesign(project, requirements)
            if (it.isCanceled) throw InterruptedException()
            buildProjectDesign
        }

        val components =
            UITools.run(
                e.project, "Specifying Components", true
            ) {
                projectDesign.components?.associate { it to api.buildComponentFileSpecifications(project, requirements, it) }
            }

        val documents =
            UITools.run(
                e.project, "Specifying Documents", true
            ) {
                projectDesign.documents?.associate {
                    it to api.buildDocumentationFileSpecifications(
                        project,
                        requirements,
                        it
                    )
                }
            }

        val tests = UITools.run(
            e.project, "Specifying Tests", true
        ) { projectDesign.tests?.associate { it to api.buildTestFileSpecifications(project, requirements, it) } }

        val sourceCodeMap = UITools.run(
            e.project, "Implementing Files", true
        ) {
            SoftwareProjectAI.parallelImplementWithAlternates(
                api = api,
                project = project,
                components = components ?: emptyMap(),
                documents = documents ?: emptyMap(),
                tests = tests ?: emptyMap(),
                drafts = config.drafts,
                threads = AppSettingsState.instance.apiThreads
            ) { progress ->
                if (it.isCanceled) throw InterruptedException()
                it.fraction = progress
            }
        }
        UITools.run(e.project, "Writing Files", false) {
            val outputDir = File(selectedFolder.canonicalPath!!)
            sourceCodeMap.forEach { (file, sourceCode) ->
                val relative = file.file
                    ?.trimEnd('/')
                    ?.trimStart('/', '.') ?: ""
                if (File(relative).isRooted) {
                    log.warn("Invalid path: $relative")
                } else {
                    val outFile = outputDir.resolve(relative)
                    outFile.parentFile.mkdirs()
                    val best = sourceCode.maxByOrNull { it.code?.length ?: 0 }!!
                    outFile.writeText(best.code ?: "")
                    log.debug("Wrote ${outFile.canonicalPath} (Resolved from $relative)")
                    if (config.saveAlternates)
                        for ((index, alternate) in sourceCode.filter { it != best }.withIndex()) {
                            val outFileAlternate =
                                outputDir.resolve(
                                    relative + ".${index + 1}"
                                )
                            outFileAlternate.parentFile.mkdirs()
                            outFileAlternate.writeText(alternate.code ?: "")
                            log.debug("Wrote ${outFileAlternate.canonicalPath} (Resolved from $relative)")
                        }
                }
            }
            selectedFolder.refresh(false, true)
        }
    }.start()

    private fun isEnabled(): Boolean {
        if (UITools.isSanctioned()) return false
        if (!AppSettingsState.instance.devActions) return false
        return true
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(GenerateProjectAction::class.java)
    }
}
