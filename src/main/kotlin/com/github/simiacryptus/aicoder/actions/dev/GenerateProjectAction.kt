package com.github.simiacryptus.aicoder.actions.dev

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.openai.proxy.ChatProxy
import com.github.simiacryptus.aicoder.openai.proxy.SoftwareProjectAI
import com.github.simiacryptus.aicoder.util.UITools
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.io.File
import javax.swing.JTextArea

class GenerateProjectAction : AnAction() {

    fun createProjectFiles(e: AnActionEvent, description: String) {
        val outputDir = File(UITools.getSelectedFolder(e)!!.canonicalPath)
        val api = ChatProxy(apiKey = AppSettingsState.instance.apiKey, base = AppSettingsState.instance.apiBase).create(
            SoftwareProjectAI::class.java
        )
        val project = api.newProject(description)
        val requirements = api.getProjectStatements(project)
        val projectDesign = api.buildProjectDesign(project, requirements)
        val files = api.buildProjectFileSpecifications(project, requirements, projectDesign)
        for (file in files.files) {
            val sourceCode = api.implement(
                project,
                files.files.map { it.location }.filter { file.requires.contains(it) }.toList(),
                file
            )
            val outFile =
                outputDir.resolve(file.location.path.replace('\\','/').trimEnd('/') + "/${file.location.name}.${file.location.extension}")
            outFile.parentFile.mkdirs()
            outFile.writeText(sourceCode.code)
            log.warn("Wrote ${outFile.canonicalPath}")
        }

    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    data class SettingsUI(val description: JTextArea = JTextArea())
    data class Settings(var description: String = "")

    override fun actionPerformed(e: AnActionEvent) {
        UITools.showDialog(e, SettingsUI::class.java, Settings::class.java) { config ->
            createProjectFiles(e, config.description)
        }
    }

    private fun isEnabled(e: AnActionEvent): Boolean {
        if (UITools.isSanctioned()) return false
        if (!AppSettingsState.instance.devActions) return false
        return true
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(GenerateProjectAction::class.java)
    }
}
